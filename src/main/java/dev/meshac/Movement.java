package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.projectile.hurtingprojectile.windcharge.AbstractWindCharge;
import net.minecraft.world.phys.AABB;

/** Movement checks: speed, fly (hover/ascend), high jump. Runs on every position packet, before vanilla. */
public final class Movement {
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int SIGNAL_AT = 10;      // buffered violations before a setback
	private static final int SKIP_TICKS = 20;  // grace after teleport, damage, effects
	private static final int GROUND_WINDOW = 5;    // PROPOSAL: sliding window over the last ground claims (legit battery gates)
	private static final int GROUND_FLAG_AT = 3;   // PROPOSAL: unsupported claims inside the window that fire nofall
	private static final int STRONG_ARM_AT = 2;    // PROPOSAL (P3-strong draft, log-only): shadow handback arms below the flag threshold; A-row-gated kill criteria in P3-STRONG-DRAFT.md

	private static final class S { final ContextLogBudget.Caller ctxBudget = new ContextLogBudget.Caller(); int ownerEntity; Object ownerDimension; volatile boolean blinkHit; volatile long burstMark; final BlinkOrigin bo = new BlinkOrigin(); long iceAt; double prevDtS; long flyMedAt, gtLogAt; final CompareLogBudget.Caller spShadowLog = new CompareLogBudget.Caller(); double spShadow; boolean wasExempt; long lastPosAt, burstStart; int burstN; double burstGap; boolean lastMoving; double ax = Double.NaN, az; long tickEnds, teAtLastPos;  final BlinkClock clock = new BlinkClock(); final BounceTracker bounce = new BounceTracker(); final MediumExit exit = new MediumExit(); final StepAccumulator stepAcc = new StepAccumulator(); double lastH, lastFallH; int slipN;
		double px, py, pz, cx, cy, cz, dy, goodX, goodY, goodZ, fallPeak = -1e9, fdMax; long nfAt;
		boolean ground, cground, init, ownTp;
		String freezeCheck = ""; long glideAt; double glideH; final ContextLogBudget.Caller noclipBudget = new ContextLogBudget.Caller(); long noclipCtxAt; long lastNs, freezeUntil; final java.util.concurrent.ConcurrentLinkedQueue<Long> arrivals = new java.util.concurrent.ConcurrentLinkedQueue<>(); double balMs; int inBlock; boolean pending; long graceAt, ajAt, windAt, exemptAt; double spBal; boolean boosted; double airY0; long kbAt; double kbX, kbY, kbZ; int sinceJump = 99, ajN, noKb, levT, bufLev, bufHop, hopClock, bufGround, grace, clean, bufSpeed, bufFly, bufJump, bufClimb, bufStatus, slowTicks; final GroundEvidence gndEv = new GroundEvidence(GROUND_WINDOW, GROUND_FLAG_AT); boolean graceSoft; final GroundEvidence gndShadow = new GroundEvidence(GROUND_WINDOW, GROUND_FLAG_AT); long softLogAt; final GroundEvidence gndEvDecay = new GroundEvidence(GROUND_WINDOW, GROUND_FLAG_AT); long decayLogAt; long nfStrongAt; double fdStrongMax;
	}

 private static S state(ServerPlayer pl) {
  return STATE.compute(pl.getUUID(),(id,old)->{
   if(old!=null && MovementLifetime.reusable(old.ownerEntity,pl.getId(),old.ownerDimension,pl.level().dimension(),pl.isAlive()))return old;
   S fresh=new S();fresh.ownerEntity=pl.getId();fresh.ownerDimension=pl.level().dimension();return fresh;
  });
 }

	/** True when this check may set back, hold or punish. Monitor mode is absolute: false for every check, listed egregious or not. */
	private static boolean acts(String check) { return MonitorGate.acts(Config.get().monitor(), Config.get().isEgregious(check)); }
	/** Monitor mode: the three pending-action fields are cleared together, never one of them. */
	private static void monitorClear(S s) { if (Config.get().monitor()) { s.freezeUntil = 0; s.nfAt = 0; s.fdMax = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; } }
	/** Drops every action that outlives its signal (hold replay, delayed NoFall damage). Called on monitor on and before a config reload. */
	public static void clearPendingActions() { for (S s : STATE.values()) { s.freezeUntil = 0; s.nfAt = 0; s.fdMax = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; } }
	/** Netty thread: stamp when a position packet really arrived. The main thread only sees it at the next tick. */
	public static void arrive(ServerPlayer pl, ServerboundMovePlayerPacket p) {
		if (!p.hasPosition()) return;
		S s = state(pl);
		long now = System.nanoTime();
		s.arrivals.add(now);
		// Blink: the client holds every movement packet back and releases them in one burst. A real stall (lag, a frozen client) holds the client's tick-end packets back as well; Blink does not touch them. Movement packets quiet for 500 ms right after the player was moving, tick-end packets still arriving on time, then most of the held packets landing inside 150 ms.
		boolean blink = s.clock.arrive(now, p.getX(0), p.getZ(0));
		s.burstMark = s.clock.burstMark; // the origin snapshot keys on the candidate's first packet; propagate before the volatile hit flag so the main thread never sees a hit with a stale mark
		if (blink) s.blinkHit = true;
	}

	/** The client ended a tick (network thread). */
	public static void tickEnd(ServerPlayer pl) { S s = state(pl); s.tickEnds++; s.clock.tickEnd(); }

	/** Whether the server is waiting for the client to confirm a teleport. Set by the packet handler before check(). */
	public static void awaiting(ServerPlayer pl, boolean waiting) { state(pl).pending = waiting; }

	/** Returns a setback position, or null if the packet is fine. */
	public static double[] check(ServerPlayer pl, ServerboundMovePlayerPacket p) {
  if (!pl.isAlive()) { STATE.remove(pl.getUUID()); return null; }
		S s = state(pl);
		monitorClear(s); // entry: monitor mode leaves no hold, no NoFall record, no fall distance pending, whichever path returns first
  if (s.pending) {
   // Consume exactly one stamp for a position packet, no stamp for rotation/status.
   if (p.hasPosition()) { Long ignored = s.arrivals.poll(); s.lastNs = ignored != null ? ignored.longValue() : System.nanoTime(); }
   s.blinkHit = false; s.burstMark = 0; s.balMs = 0; s.sinceJump = 99; s.ajN = 0;
   s.grace = Math.max(s.grace, 2); s.graceAt = System.currentTimeMillis();
   return null;
  }
		if (Config.get().exempt(pl)) return null;
		if (p.hasRotation()) {
   float yaw=p.getYRot(pl.getYRot()), pitch=p.getXRot(pl.getXRot());
   Interact.look(pl, yaw, pitch); Mining.look(pl, yaw, pitch);
  }
		if (s.blinkHit) {
			BlinkOrigin.Act act = s.bo.onHit(s.burstMark, p.hasPosition()); // DEFER: this thread has not reached the burst's first packet yet, so there is no fresh origin
			if (act != BlinkOrigin.Act.DEFER) {
				s.blinkHit = false;
				if (!pl.isCreative() && !pl.isSpectator() && !pl.isPassenger() && !pl.isFallFlying() && !pl.getAbilities().mayfly) {
					Verdict.Step bst = Verdict.signal(pl, "blink", "held its movement packets back and released them in one burst", 1);
					if (bst != Verdict.Step.LOG && acts("blink") && act == BlinkOrigin.Act.ORIGIN && p.hasPosition()) { // the flushed packets already moved the player: put them back where the burst began (soft mode never gets here)
						s.ownTp = true; s.bufSpeed = s.bufFly = s.bufJump = s.bufGround = s.bufClimb = s.bufHop = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; s.gndEv.clear(); s.gndShadow.clear(); s.gndEvDecay.clear(); s.graceSoft = false; s.balMs = 0; s.spBal = 0; s.spShadow = 0; s.grace = 2; s.graceAt = System.currentTimeMillis(); s.dy = 0;
						s.goodX = s.bo.x; s.goodY = s.bo.y; s.goodZ = s.bo.z;
						s.px = s.cx = s.bo.x; s.py = s.cy = s.bo.y; s.pz = s.cz = s.bo.z;
						if (bst == Verdict.Step.HOLD) { s.freezeUntil = System.currentTimeMillis() + 1500; s.freezeCheck = "blink"; }
						return new double[] {s.bo.x, s.bo.y, s.bo.z, 0};
					}
				}
			}
		}
		if (!p.hasPosition()) return statusOnly(pl, p, s);
		if (s.grace > 0 && System.currentTimeMillis() - s.graceAt > 1100) { s.grace = 0; if (s.graceSoft) { s.graceSoft = false; if (TRACE && s.gndShadow.unsupported() > 0) Meshac.LOG.info("[trace] nofall shadow-evidence end {} count={}", pl.getGameProfile().name(), s.gndShadow.unsupported()); s.gndShadow.clear(); } } // grace is 20 ticks of real time; a player standing still sends few packets
		double x = p.getX(pl.getX()), y = p.getY(pl.getY()), z = p.getZ(pl.getZ());
		boolean ground = p.isOnGround();
		// Our own record of the fall: the highest point since the player last stood on something real. A NoFall hack keeps resetting the server's count, so a flag hands it back.
		{
			AABB fb = pl.getBoundingBox().move(x - pl.getX(), y - pl.getY(), z - pl.getZ());
			boolean supported = !pl.level().noCollision(pl, new AABB(fb.minX, fb.minY - 0.1, fb.minZ, fb.maxX, fb.minY, fb.maxZ));
			if (TRACE && ground && s.fallPeak - y >= 1.0) Meshac.LOG.info("[trace] bounce touchdown y={} peak={} fall={} supported={} nofallHold={} grace={} armedBefore={}", r(y), r(s.fallPeak), r(s.fallPeak - y), supported, s.nfAt != 0, s.grace, s.bounce.armed());
			if (supported && s.nfAt == 0) {
				s.lastFallH = Math.max(0, s.fallPeak - y); // the fall that ended here: a bounce block hands most of it back
				// Arm the bounce tracker NOW, before the jump/high-jump/microhop math below: the launch packet itself is already covered.
				// Restitution comes from the version adapter (legacy block handlers or native definition) (version-keyed: 26.2 slime 1.0, bed 0.66 for living entities, honey/ladder/ordinary 0); sneaking suppresses the bounce in vanilla.
				if (BounceTracker.contact(ground, s.lastFallH)) s.bounce.land(y, s.lastFallH, pl.isShiftKeyDown() ? 0.0 : BounceBlocks.restitution(pl.level(), x, y, z), System.currentTimeMillis()); // the blocks under the CLAIMED landing footprint at vanilla's getOnPosLegacy() row (y-0.2), max restitution: permissive at pad edges
				if (TRACE && (s.lastFallH >= 1.0 || s.bounce.armed())) Meshac.LOG.info("[trace] bounce land y={} fallH={} g={} contact={} sneak={} rest={} armed={}", r(y), r(s.lastFallH), ground, BounceTracker.contact(ground, s.lastFallH), pl.isShiftKeyDown(), r(BounceBlocks.restitution(pl.level(), x, y, z)), s.bounce.armed());
			} else if (pl.onClimbable() || pl.isInWater() || pl.isInLava()) s.bounce.land(y, 0, 0, System.currentTimeMillis()); // a ladder or liquid cancels the fall: no bounce energy
			if ((supported && s.nfAt == 0) || pl.isInWater() || pl.isInLava() || pl.onClimbable() || pl.isFallFlying() || pl.isPassenger() || y > s.fallPeak + 50) s.fallPeak = y; else s.fallPeak = Math.max(s.fallPeak, y);
		}
		Long arrived = s.arrivals.poll(); // pairs 1:1 with the stamp from arrive()
		// Awaiting-confirm handled before rotation, Blink or fall bookkeeping. // vanilla ignores every move while it waits for the teleport confirm, and so do we: the stale packets a client sends after a setback (dy 1.9, 6 blocks over budget on the rig) must not add heat
		s.bo.snapshot(arrived, s.burstMark, s.init, s.goodX, s.goodY, s.goodZ); // first packet of a possible Blink burst: remember the last accepted spot before it
		// The server position is the last accepted one (vanilla has already handled the previous packet).
		// If it is neither where the previous packet started nor where it claimed, the server moved the player.
		double sx = pl.getX(), sy = pl.getY(), sz = pl.getZ();
		boolean atStart = near(sx, sy, sz, s.px, s.py, s.pz), atClaim = near(sx, sy, sz, s.cx, s.cy, s.cz);
		// A real teleport moves the player a long way. Hacks that send several tiny fake positions in one tick (Criticals, MaceDMG) must not be able to open the grace window.
		double offBy = Math.min(Math.sqrt((sx - s.px) * (sx - s.px) + (sy - s.py) * (sy - s.py) + (sz - s.pz) * (sz - s.pz)), Math.sqrt((sx - s.cx) * (sx - s.cx) + (sy - s.cy) * (sy - s.cy) + (sz - s.cz) * (sz - s.cz)));
		if (TeleportDetect.serverMoved(s.init, atStart, atClaim, offBy)) {
			s.init = true; s.graceAt = System.currentTimeMillis(); s.grace = s.ownTp ? 3 : SKIP_TICKS; s.ownTp = false; s.graceSoft = false; s.gndShadow.clear(); // our own setback needs only a short grace
			s.dy = 0; s.ground = ground; s.bufSpeed = s.bufFly = s.bufJump = 0; s.spBal = 0; s.spShadow = 0;
			s.goodX = sx; s.goodY = sy; s.goodZ = sz;
			s.px = sx; s.py = sy; s.pz = sz; s.cx = x; s.cy = y; s.cz = z; s.cground = ground;
			return null;
		}
		if (atClaim) { s.dy = s.cy - s.py; s.ground = s.cground; }
		if (s.freezeUntil != 0 && !acts(s.freezeCheck)) s.freezeUntil = 0; // monitor mode (or a check no longer enforced): a hold armed earlier never replays
		if (MonitorGate.holdReplays(System.currentTimeMillis(), s.freezeUntil, acts(s.freezeCheck))) { // hold: ignore every move and keep putting the player back
			s.spBal = 0; s.spShadow = 0;
			s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ;
			return new double[] {s.goodX, s.goodY, s.goodZ, 0};
		}
		// NoClip: the body sits inside solid blocks. Vanilla only refuses moves that newly collide, so a player who starts inside a wall walks on through it.
		if (!pl.isCreative() && !pl.isSpectator() && !pl.isPassenger() && s.grace == 0 && !pl.level().noCollision(pl, pl.getBoundingBox().deflate(0.1))) {
			if (++s.inBlock >= 10) { s.inBlock = 0; ctxLog(pl, s, x - sx, y - sy, z - sz); noclipCtx(pl, s, sx, sy, sz, x, y, z); Verdict.Step st = Verdict.signal(pl, "noclip", "moving inside solid blocks", 1); if (st != Verdict.Step.LOG && acts("noclip")) { s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ; return new double[] {s.goodX, s.goodY, s.goodZ, 0}; } } // LOG = exempt, disabled, or log-only mode: record, never correct
		} else s.inBlock = 0;
		double dx = x - sx, dy = y - sy, dz = z - sz;
		// NoClip, Teleport, Blink. No exemption (damage, grace) covers these; only a teleport the server itself ordered does.
		// Any move over 1.5 blocks must not pass through solid blocks on the way (vanilla only tests where the packet ends); a move over 10 blocks is refused whatever is in between.
		double d2 = dx * dx + dy * dy + dz * dz;
		if (!s.pending && !pl.isCreative() && !pl.isSpectator() && !pl.isPassenger() && (d2 > 100 || d2 > 2.25 && blocked(pl, dx, dy, dz))) {
			ctxLog(pl, s, dx, dy, dz); noclipCtx(pl, s, sx, sy, sz, x, y, z); Verdict.Step st = Verdict.signal(pl, "noclip", d2 > 100 ? String.format("jumped %.0f blocks in one packet", Math.sqrt(d2)) : String.format("moved %.1f blocks through solid blocks", Math.sqrt(d2)), 2);
			if (st != Verdict.Step.LOG && acts("noclip")) { // LOG = exempt, disabled, or log-only mode: record, never correct
				s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ;
				return new double[] {s.goodX, s.goodY, s.goodZ, 0};
			}
		}
		// Slippy: client-side friction override. On a normal block (0.6) a stopping player loses about 45 percent of the speed every tick; ice keeps 89 percent. Five ground ticks in a row that lose only 3 to 20 percent on a block that is not ice, slime or the like is a changed friction.
		{
			double h = Math.hypot(dx, dz);
			boolean normal = !pl.isInWater() && !pl.isInLava() && !pl.isPassenger() && !pl.isFallFlying() && !pl.getAbilities().mayfly && pl.level().getBlockState(pl.blockPosition().below()).getBlock().getFriction() == 0.6f && pl.level().getBlockState(pl.blockPosition()).getBlock().getFriction() == 0.6f;
			if (ground && s.ground && normal && !pl.isCreative() && !pl.isSpectator()) {
				if (h > 0.03 && s.lastH > 0.03) { double r = h / s.lastH; if (r > 0.80 && r < 0.97) s.slipN++; else if (r < 0.75 || r >= 1.0) s.slipN = 0; }
				else s.slipN = 0;
			} else s.slipN = 0;
			s.lastH = h;
			if (s.slipN >= 20) { s.slipN = 0; Verdict.signal(pl, "movement", "slippery ground: speed barely drops on a normal block", 1); }
		}
		boolean baseExempt = pl.isCreative() || pl.isSpectator() || pl.getAbilities().mayfly || pl.isPassenger()
			|| pl.isFallFlying() || pl.isAutoSpinAttack() || pl.isInWater() || pl.isInLava() || pl.onClimbable()
			|| pl.hasEffect(MobEffects.LEVITATION) || pl.hasEffect(MobEffects.SLOW_FALLING);
		// A wind charge launches the player far past any jump or fall. Exempt from the moment a charge is near until a moment after the landing.
		long nowMs = System.currentTimeMillis();
		if (pl.isAutoSpinAttack()) { s.windAt = nowMs; Glide.wind(pl); } // riptide: a launch far past any sprint, in rain or out of water (not covered by isInWater); same window as a wind charge
		s.boosted = BoostWindow.boosted(nowMs, s.windAt, ground, s.boosted);
		// Only server-observed elytra motion can seed the horizontal exit envelope.
		// Removing the elytra does not instantly erase the vanilla client momentum.
		if (pl.isFallFlying()) { s.glideAt = nowMs; s.glideH = Math.hypot(dx, dz); }
		// Only a hit that can throw the player (a mob, a projectile, a blast) buys slack. Fire, poison, wither, starvation and cactus hurt every second and would mask any hack run beside them.
		var hs = pl.getLastDamageSource();
		boolean pushed = pl.hurtTime > 0 && hs != null && (hs.getDirectEntity() != null || hs.getEntity() != null || hs.getMsgId().contains("explosion") || hs.getMsgId().contains("badRespawnPoint") || hs.getMsgId().contains("intentionalGameDesign") || hs.getMsgId().contains("fall") || hs.getMsgId().contains("flyIntoWall") || hs.getMsgId().contains("anvil") || hs.getMsgId().contains("fallingBlock"));
		boolean exempt = baseExempt || pushed || s.boosted;
		if (pushed) { s.grace = SKIP_TICKS; s.graceAt = System.currentTimeMillis(); s.graceSoft = hs.getMsgId().contains("fall") || hs.getMsgId().contains("flyIntoWall") || hs.getMsgId().contains("anvil"); } // fall/flyIntoWall/anvil/fallingBlock cannot throw the player (fall matches fallingBlock): soft grace, O3 shadow evidence stays live
		if (TRACE) Meshac.LOG.info("[trace] {} dx={} dy={} dz={} g={} sg={} grace={} exempt={} hurt={} bounce={}", pl.getGameProfile().name(), r(dx), r(dy), r(dz), ground, s.ground, s.grace, exempt, pl.hurtTime, s.bounce.armed());
		String hit = null;
		// Levitation (NoLevitation): the effect lifts the player every tick. Not rising for a while, with open air above, means the client ignores it.
		if (pl.hasEffect(MobEffects.LEVITATION) && !pl.isCreative() && !pl.isSpectator() && !pl.isPassenger()) {
			boolean open = pl.level().noCollision(pl, pl.getBoundingBox().move(0, 0.3, 0));
			if (++s.levT > 8 && open && dy < 0.01) s.bufLev++; else s.bufLev = Math.max(0, s.bufLev - 1);
			if (s.bufLev >= 6) { hit = String.format("levitation dy %.3f", dy); s.bufLev = 0; }
		} else { s.levT = 0; s.bufLev = 0; }
		if (pl.onClimbable() && s.grace <= 0 && !pl.isCreative() && !pl.isSpectator()) { // vanilla climbs at most 0.15 per tick
			s.bufClimb = dy > 0.21 ? s.bufClimb + 2 : Math.max(0, s.bufClimb - 1);
			if (s.bufClimb >= 4) hit = String.format("ladder dy %.3f", dy);
		} else s.bufClimb = 0;
		// Ground-claim probe, shared by the air-chain reset gate and the nofall check: a ground claim
		// with only air 0.1 below the moved box is unsupported (spoof evidence, not a landing).
		// O3: while grace came from a non-displacing damage source the probe still runs, log-only
		// (shadow evidence; never flags, never clears the live window). MESHAC_GROUND_TOL=classify
		// widens the strip to 0.13 for a partial collision top within the gap (tolerated claim).
		boolean softGrace = s.grace > 0 && s.graceSoft && !baseExempt && !s.boosted;
		boolean claimAir = false, claimTol = false;
		if (ground && ((!exempt && s.grace <= 0) || softGrace)) {
			AABB gb = pl.getBoundingBox().move(dx, dy, dz);
			boolean air = pl.level().noCollision(pl, new AABB(gb.minX, gb.minY - 0.1, gb.minZ, gb.maxX, gb.minY, gb.maxZ));
			if (air && GROUND_TOL_CLASSIFY) {
				SupportHit sup = supportHit(pl.level(), new AABB(gb.minX, gb.minY - GroundTolerance.CANDIDATE, gb.minZ, gb.maxX, gb.minY, gb.maxZ));
				if (sup != null) {
					claimTol = GroundTolerance.tolerated(true, true, sup.top, gb.minY);
					if (claimTol && nowMs - s.gtLogAt > 1000) { s.gtLogAt = nowMs; Meshac.LOG.info(GroundTolerance.toleratedLine(pl.getGameProfile().name(), sup.id, sup.top, gb.minY, s.bufGround)); }
				}
			}
			claimAir = air && !claimTol;
		}
		if (s.grace > 0 || exempt) {
			if (s.grace > 0) s.grace--;
			if (s.grace == 0 && s.graceSoft) { s.graceSoft = false; if (TRACE && s.gndShadow.unsupported() > 0) Meshac.LOG.info("[trace] nofall shadow-evidence end {} count={}", pl.getGameProfile().name(), s.gndShadow.unsupported()); s.gndShadow.clear(); }
			s.airY0 = y; s.sinceJump = 99; s.exemptAt = System.currentTimeMillis();
			if (FpGuards.bendsGravity(pl.isInWater(), pl.isInLava(), pl.onClimbable(), pl.hasEffect(MobEffects.LEVITATION), pl.hasEffect(MobEffects.SLOW_FALLING))) s.exit.onMedium(nowMs); // the next non-exempt packet is the medium's exit step
			if (exempt) { s.spBal = SustainBudget.onExempt(s.spBal, s.wasExempt); s.spShadow = SustainBudget.onExempt(s.spShadow, s.wasExempt); } s.wasExempt = exempt; // a boosted or pushed stretch must not leave balance behind to flag the first normal step
		} else {
			s.wasExempt = false;
			final boolean firstExit = s.exit.firstAfter(nowMs, dy); // the first packet out of water, lava, a ladder or an effect: its dy comes from that medium, not from the gravity chain
			// Speed: horizontal blocks per packet. Ground and air have different vanilla ceilings (sprint-jump is the peak);
			// slippery blocks lift both. Scaled by the speed attribute (0.13 when sprinting).
			double fr = pl.level().getBlockState(pl.blockPosition().below()).getBlock().getFriction();
			double slow = Math.min(pl.level().getBlockState(pl.blockPosition().below()).getBlock().getSpeedFactor(),
				pl.level().getBlockState(pl.blockPosition()).getBlock().getSpeedFactor()); // soul sand is a short block: the player sinks into its cell
			double slowEff = SustainBudget.effectiveSlow(slow, pl.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_EFFICIENCY)); // vanilla lerps the block slowdown away with movement_efficiency (Soul Speed)
			boolean web = StuckBlocks.cobweb(pl.level(), pl.getBoundingBox()); // speed cap: only a web the last accepted position was already stuck in; the entry packet still carries normal momentum // the same overlap the fly check uses
			s.slowTicks = slow < 1.0 ? s.slowTicks + 1 : 0; // the first ticks on a slow block still carry normal momentum
			double cap = SustainBudget.packetCap(s.ground && ground, pl.getSpeed() / 0.13, fr, s.slowTicks, slowEff, Meshac.skip(pl)); // slowEff, not slow: Soul Speed removes the block slowdown for the per-packet cap too. A hacker at 1.5x still trips.
			// One second at no more than the last verified glide speed. No flight,
			// timer, wall or jump check is exempted, and acceleration is not allowed.
			cap = GlideWindow.cap(cap, nowMs, s.glideAt, s.glideH);
			if (web) cap = FpGuards.webCap(pl.getSpeed() / 0.13); // vanilla cobweb scales the next move by 0.25: a speed-potion sprint-jump into a web carries 0.25 x its normal packet once
			double h = Math.hypot(dx, dz);
			if (h > cap) { s.bufSpeed += h > cap * 1.25 ? 3 : 1; s.clean = 0; } // a big overshoot counts triple
			else if (++s.clean >= 10) { s.bufSpeed = Math.max(0, s.bufSpeed - 1); s.clean = 0; }
			if (s.bufSpeed >= SIGNAL_AT) hit = String.format("speed %.2f>%.2f", h, cap);
			// Sustained speed: the per-packet cap carries burst grace, so a modest always-on multiplier can sit under it forever
			// (round 1 case A5: 2.0x ground-only at 8.17 bps, zero signals). Budget distance against the fastest sustained
			// legit pace measured on this rig: flat sprint-jump 7.09 bps average, worst 0.4 s window 9.2. A 7.4 bps budget
			// keeps that whole legit trace at 0.77 of the 4-block limit while the cheat flags in about 5 s.
			if (fr > 0.61) s.iceAt = nowMs; // slick ground keeps its momentum for a while after the player steps off
			double sustain = SustainBudget.perTick(pl.getSpeed() / 0.13, nowMs - s.iceAt < 2000, s.slowTicks, slowEff, Meshac.skip(pl), web, GlideWindow.capWindow(nowMs, s.glideAt), s.glideH);
			long arrNs = arrived != null ? arrived.longValue() : System.nanoTime();
			double dtS = s.lastNs == 0 ? 0.05 : Math.min(1.0, Math.max(0.05, (arrNs - s.lastNs) / 1e9)); // never below one tick: a stalled link releasing held packets must not look like a sprint; Timer owns packet rate
			s.spBal = SustainBudget.step(s.spBal, h, sustain, dtS);
			if (BEDROCK_BUDGET_COMPARE && Meshac.skip(pl)) bedrockShadow(pl, s, h, dtS, nowMs, slowEff, web); // Java budget beside the Bedrock one, log only
			if (TRACE) Meshac.LOG.info("[trace] spbal {} h={} dtS={} sustain={} bal={}", pl.getGameProfile().name(), r(h), r(dtS), r(sustain), r(s.spBal));
			if (SustainBudget.flagged(s.spBal) && hit == null) { hit = String.format("speed sustained %.1f blocks over budget", s.spBal); s.spBal = 0; s.spShadow = 0; }
			double prevDt = s.prevDtS; s.prevDtS = dtS;
			boolean powderNear = pl.level().getBlockState(pl.blockPosition()).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
			|| pl.level().getBlockState(pl.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW)
			|| pl.level().getBlockState(pl.blockPosition().below(2)).is(net.minecraft.world.level.block.Blocks.POWDER_SNOW); // struggle-climbing out of powdered snow kicks dy ~0.63, a hair over the bar - mutant319 FP MA-65NE 2026-10-11
			// Fly: in the air, vanilla gravity gives dy = (lastDy - 0.08) * 0.98. Going above that is not vanilla.
			// Slow Falling (gravity 0.01), levitation, fluids, climbables, cobweb, honey, scaffolding and bubble columns all bend that chain. Server-side state only, never dy.
			net.minecraft.core.BlockPos fp = pl.blockPosition(); net.minecraft.world.level.Level fl = pl.level();
			// Hard mediums bend the gravity chain for as long as the player is in them (and 400 ms after). Cobweb and honey are NOT here: they only exempt a packet that moves the way vanilla's stuck / slide does (dy gates, real body box), this packet only.
			boolean flyMedium = FpGuards.hardMedium(pl.hasEffect(MobEffects.SLOW_FALLING), pl.hasEffect(MobEffects.LEVITATION), pl.isInWater(), pl.isInLava(), pl.onClimbable(),
				fl.getBlockState(fp).is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN) || fl.getBlockState(fp.below()).is(net.minecraft.world.level.block.Blocks.BUBBLE_COLUMN),
				fl.getBlockState(fp).is(net.minecraft.world.level.block.Blocks.SCAFFOLDING) || fl.getBlockState(fp.below()).is(net.minecraft.world.level.block.Blocks.SCAFFOLDING));
			AABB srvBox = pl.getBoundingBox(), endBox = srvBox.move(x - pl.getX(), y - pl.getY(), z - pl.getZ()); // real pose box at the last accepted position (where the stuck or slide state was set) and at this packet's endpoint (the first contact)
			boolean stuckNow = FpGuards.stuckNow(dy, StuckBlocks.cobweb(fl, srvBox) || StuckBlocks.cobweb(fl, endBox), StuckBlocks.honeySlide(fl, srvBox, pl.getX(), pl.getY(), pl.getZ(), pl.getBbWidth(), pl.onGround()) || StuckBlocks.honeySlide(fl, endBox, x, y, z, pl.getBbWidth(), ground));
			if (flyMedium) { s.flyMedAt = nowMs; s.bufFly = 0; }
			if (!s.ground && !ground && !FpGuards.flyMediumExempt(flyMedium, nowMs - s.flyMedAt) && !stuckNow && System.currentTimeMillis() > s.freezeUntil + 600) { // right after a hold the player hangs where we froze them; that is our doing, not a flight
				boolean inPowderBody = StuckBlocks.powderBody(fl, srvBox) || StuckBlocks.powderBody(fl, endBox); // real pose box at the server and endpoint positions, the entityInside overlap (same as the web test)
				AABB pfb = pl.getBoundingBox().move(x - pl.getX(), y - pl.getY(), z - pl.getZ());
				boolean supportedBelow = !pl.level().noCollision(pl, new AABB(pfb.minX, pfb.minY - 0.1, pfb.minZ, pfb.maxX, pfb.minY, pfb.maxZ)); // the same support test the fall record uses
				double expect = FpGuards.stuckExpect((s.dy - 0.08) * 0.98, inPowderBody); // powder snow multiplies every move by 1.5 vertically and zeroes the velocity: a steady -0.1176 fall
				boolean powderRise = FpGuards.powderRise(dy, powderNear); // 0.2 per tick powder snow climb: fly, high jump and microhop FPs in the ilhsmc triage (dy 0.200, high jump rose ~2.0)
				if (powderRise) s.airY0 = y;
				s.bufFly = firstExit ? s.bufFly : FpGuards.flyBuf(s.bufFly, dy, expect, powderRise, FpGuards.staleFall(dy, prevDt) || FpGuards.restingInPowder(inPowderBody, supportedBelow, dy)); // a big climb counts triple; a fall right after a multi-tick packet is not a hover (triage: fly dy -0.161 expect -0.341)
				if (!firstExit && s.bufFly >= SIGNAL_AT && hit == null) hit = String.format("fly dy %.3f expect %.3f", dy, expect);
				// Air jump: a fresh upward kick in mid-air. A wind charge can do it once per 10 ticks, so two kicks inside 8 packets, or four inside 2.5 s, is not vanilla.
				s.sinceJump++;
				if (FpGuards.airKick(dy, expect) && !firstExit && s.grace == 0 && !s.bounce.bouncing(y, nowMs)) { // a bounce re-launch is a vanilla upward kick (restitution handler); firstExit is the one-packet medium-exit waiver
					long ms = System.currentTimeMillis();
					s.ajN = FpGuards.airKickCount(s.ajN, ms, s.ajAt); s.ajAt = ms;
					if (hit == null && FpGuards.airJumpFlag(s.sinceJump, s.ajN)) hit = String.format("air jump dy %.3f", dy);
					s.sinceJump = 0;
				}
				// High jump: one jump climbs 1.25 blocks (more with Jump Boost). Rising past that in one flight is not vanilla unless a wind charge threw the player.
				MobEffectInstance jb = pl.getEffect(MobEffects.JUMP_BOOST);
				double room = 1.45 + (jb == null ? 0 : (jb.getAmplifier() + 1) * 0.6);
				if (s.airY0 == 0) s.airY0 = y; // a fresh state has no takeoff height yet
				if (dy > 0.05 && y - s.airY0 > room && s.grace == 0 && hit == null && System.currentTimeMillis() - s.exemptAt > 4000 && !s.bounce.bouncing(y, nowMs)) hit = String.format("high jump rose %.2f", y - s.airY0);
			} else if (!claimAir) { if (!stuckNow) s.bufFly = 0; s.sinceJump = 99; s.airY0 = s.ground ? sy : y; } // a stuck or sliding packet skips the checks for itself only; an unsupported ground claim is not a landing: it must not reset the air chains (rig round-2 human high jump)
			boolean bouncePad = pl.level().getBlockState(pl.blockPosition().below()).is(net.minecraft.world.level.block.Blocks.SLIME_BLOCK)
				|| pl.level().getBlockState(pl.blockPosition().below()).is(net.minecraft.tags.BlockTags.BEDS)
				|| pl.level().getBlockState(pl.blockPosition().below(2)).is(net.minecraft.world.level.block.Blocks.SLIME_BLOCK)
				|| pl.level().getBlockState(pl.blockPosition().below(2)).is(net.minecraft.tags.BlockTags.BEDS); // bed/slime bounces decay through the microhop band - PhilipIsGaming FP 2026-10-10
			// MITIGATION 2026-10-10 (case MA-M9HG stair FP, per Luke): step-accumulator disabled until the stair-tolerant fix passes the zero-FP legit battery.
			final boolean STEP_ACCUMULATOR_ENABLED = true; // stair-tolerant fix (case MA-M9HG); the zero-FP battery is NOT done: helper sims only, no native witness, so no deploy on this alone
			// Step: a hack can climb a block with several small rises that all claim ground. Add them up.
			boolean bodyInPowderStep = StuckBlocks.powderBody(fl, srvBox) || StuckBlocks.powderBody(fl, endBox); // vanilla climbs powder only while inside it; same pose-box overlap as the fly test (a body at the edge is judged by its real box, not a feet/above cell pair)
			String stepHit = s.stepAcc.packet(ground, dy, pl.hasEffect(MobEffects.JUMP_BOOST), STEP_ACCUMULATOR_ENABLED && hit == null, FpGuards.powderClimb(dy, bodyInPowderStep)); // one vanilla auto-step (slab/stair) resets the window, never counts - MA-M9HG stair FP
			if (stepHit != null) hit = stepHit;
			// Step: claiming ground while rising more than a half block. Vanilla only auto-steps 0.6.
			if (StepEvidence.excessive(ground, dy, pl.hasEffect(MobEffects.JUMP_BOOST)) && hit == null) hit = String.format("step dy %.3f onGround", dy);
			// High jump / step: leaving the ground higher than a jump (0.42 + jump boost) or a step (0.6).
			if (!StepEvidence.autoStep(ground, dy, pl.hasEffect(MobEffects.JUMP_BOOST)) && s.ground && dy > 0.62 + 0.1 * (pl.hasEffect(MobEffects.JUMP_BOOST) ? 3 : 0) && GlideWindow.jumpCheckLive(nowMs, s.glideAt) && !FpGuards.powderJumpBand(dy, powderNear) && !s.bounce.bouncing(y, nowMs)) { // powder exempts only the ~0.63 struggle band, not any big dy (rig round-2 powder-snow jump) // not just after an elytra landing (rocket/glide momentum carries into the first hop) and not during a real slime/bed bounce (launches the same way)
				s.bufJump += 2;
				if (s.bufJump >= 2 && hit == null) hit = String.format("jump dy %.3f", dy);
			} else s.bufJump = Math.max(0, s.bufJump - 1);
			// Micro hop: leaving the ground by less than a jump (0.42) with nothing under the new spot. Criticals (packet and mini jump) and MaceDMG do this on every hit.
		if (s.ground && dy > 0.005 && dy < 0.30 && !bouncePad && !s.bounce.bouncing(y, nowMs) && !pl.isInWater() && !pl.onClimbable() && !pl.isPassenger() && !pl.getAbilities().flying) {
				AABB mb = pl.getBoundingBox().move(dx, dy, dz);
				boolean unsupported = pl.level().noCollision(pl, new AABB(mb.minX, mb.minY - 0.03, mb.minZ, mb.maxX, mb.minY, mb.maxZ));
				boolean lowCeiling = !pl.level().noCollision(pl, new AABB(mb.minX, mb.maxY, mb.minZ, mb.maxX, mb.maxY + 0.6, mb.maxZ)); // a jump clipped by a ceiling lands in this dy band legitimately - Luke sprint-jump-under-2-block FP 2026-10-11
				boolean powder = powderNear; // struggle-climbing out of powdered snow repeats dy ~0.2 - Luke FP 2026-10-11; also standing on top of it or one block above
				if (TRACE) Meshac.LOG.info("[trace] microhop dy={} unsupported={} lowCeiling={} powder={} buf={}", r(dy), unsupported, lowCeiling, powder, s.bufHop);
				if (unsupported && !lowCeiling && !powder) s.bufHop += 2;
				if (s.bufHop >= 3 && hit == null) { hit = String.format("microhop dy %.3f", dy); s.bufHop = 0; }
			}
			if (++s.hopClock >= 30) { s.hopClock = 0; s.bufHop = Math.max(0, s.bufHop - 1); } // one hop is forgiven, two inside about 1.5 s are not
		}
		// Timer: each move packet is worth 50 ms. Packets running ahead of the real clock = game speed hack.
		long now = arrived != null ? arrived : System.nanoTime();
		if (s.lastNs != 0) {
			// Teleports make the client send extra confirm packets, so no timing while in grace.
			s.balMs = s.grace > 0 ? 0 : s.balMs + 50 - (now - s.lastNs) / 1e6;
			s.balMs = Math.max(-300, Math.min(s.balMs, 1600)); // a stalled connection (tunnel, wifi) releases its held packets in one burst: bank up to 1.6 s of catch-up
			if (s.balMs > 1300 && hit == null) hit = String.format("timer ahead %.0f ms", s.balMs);
		}
		s.lastNs = now;
		// NoFall / ground spoof: claims to stand on something with only air below. Windowed evidence
		// (rig round 2: an interleaved spoof slipped the old 3-consecutive buffer; honest air packets
		// between spoofed claims no longer erase the evidence).
		if (ground && !exempt && s.grace <= 0) {
			if (!claimTol) { // a tolerated claim is a landing on a partial shape: it neither enters nor evicts the evidence window
				s.gndEv.add(claimAir);
				if (TRACE) { s.gndEvDecay.add(claimAir); // grace-boundary persistence DRAFT, log-only shadow: identical claim set/order/threshold as the live window; decays (not clears) at the exempt/grace reset below; default off (TRACE is the MESHAC_TRACE env switch, unset by default)
					if (s.gndEvDecay.flagged() && !s.gndEv.flagged() && nowMs - s.decayLogAt > 1000) { s.decayLogAt = nowMs; Meshac.LOG.info("[trace] nofall decay-shadow {} count={} (grace-boundary persistence draft, log-only)", pl.getGameProfile().name(), s.gndEvDecay.unsupported()); } }
				s.bufGround = s.gndEv.unsupported();
				if (GROUND_TOL_COMPARE && claimAir) groundTolCompare(pl, s, pl.getBoundingBox().move(dx, dy, dz));
				if (TRACE && (claimAir || s.bufGround > 0)) groundCtx(pl, s, s.gndEv.flagged() && hit == null, claimAir, ground, dy, s.prevDtS, exempt);
				if (TRACE && (claimAir || s.bufGround > 0)) Meshac.LOG.info("[trace] gndclaim {} air={}", pl.getGameProfile().name(), claimAir); // replay-harness capture (sim/GroundReplay): one line per scored claim
				if (s.nfStrongAt == 0 && s.gndEv.unsupported() >= STRONG_ARM_AT && hit == null) { s.nfStrongAt = nowMs; Meshac.LOG.info("[trace] nofall strong-arm {} count={} (P3-strong draft, log-only)", pl.getGameProfile().name(), s.gndEv.unsupported()); }
				if (s.gndEv.flagged() && hit == null) hit = "nofall ground spoof";
			}
		} else if (ground && softGrace) { // O3, log-only: shadow window during non-displacing damage grace - never flags, never clears the live window
			if (!claimTol) {
				s.gndShadow.add(claimAir);
				if (TRACE && s.gndShadow.flagged() && nowMs - s.softLogAt > 1000) { s.softLogAt = nowMs; Meshac.LOG.info("[trace] nofall shadow-evidence {} count={} (damage grace, log-only)", pl.getGameProfile().name(), s.gndShadow.unsupported()); }
			}
		} else if (ground) { if (TRACE && s.bufGround > 0) Meshac.LOG.info("[trace] gndclear {}", pl.getGameProfile().name()); s.gndEv.clear(); if (TRACE) s.gndEvDecay.decay(); s.bufGround = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; } // exempt/grace ground claim: fresh start, as before; air packets leave the window alone
		if (hit != null) {
			String[] ch = hit.split(" ", 2);
			if (ch[0].equals("speed") || ch[0].equals("fly") || ch[0].equals("air") || ch[0].equals("high") || ch[0].equals("microhop")) ctxLog(pl, s, dx, dy, dz); // log only: what the player was doing when the speed check fired
			if (ch[0].equals("nofall")) { if (MonitorGate.armNoFall(acts("nofall"))) s.nfAt = System.currentTimeMillis(); s.nfStrongAt = 0; s.fdStrongMax = 0; if (TRACE) Meshac.LOG.info("[trace] nofall flag peak={} y={}", r(s.fallPeak), r(y)); }
			Verdict.Step step = Verdict.signal(pl, ch[0], ch.length > 1 ? ch[1] : "", ch[0].equals("step") ? 3 : ch[0].equals("fly") ? 3 : ch[0].equals("microhop") ? 2 : 1); // a block climbed in a few ticks is never an accident
			if (step == Verdict.Step.LOG || !acts(ch[0])) { s.bufSpeed = s.bufFly = s.bufJump = s.bufGround = s.bufClimb = s.bufHop = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; s.gndEv.clear(); s.gndShadow.clear(); s.gndEvDecay.clear(); s.graceSoft = false; s.balMs = 0; s.spBal = 0; s.spShadow = 0; return null; } // soft mode: record, never correct
			s.ownTp = true;
			s.bufSpeed = s.bufFly = s.bufJump = s.bufGround = s.bufClimb = s.bufHop = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; s.gndEv.clear(); s.gndShadow.clear(); s.gndEvDecay.clear(); s.graceSoft = false; s.balMs = 0; s.spBal = 0; s.spShadow = 0; s.grace = 2; s.graceAt = System.currentTimeMillis(); s.dy = 0;
			s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ;
			if (step == Verdict.Step.HOLD) { s.freezeUntil = System.currentTimeMillis() + 1500; s.freezeCheck = ch[0]; }
			return new double[] {s.goodX, s.goodY, s.goodZ, 0};
		}
		if (s.bufSpeed == 0 && s.bufFly == 0 && s.bufJump == 0 && s.inBlock == 0) { s.goodX = x; s.goodY = y; s.goodZ = z; }
		s.px = sx; s.py = sy; s.pz = sz; s.cx = x; s.cy = y; s.cz = z; s.cground = ground;
		return null;
	}

	/** Once per server tick for every player. A player who stands still because the client ignores the push sends no move packets at all, so the check cannot hang on them. */
	public static void tick(ServerPlayer pl) {
  if (!pl.isAlive()) { STATE.remove(pl.getUUID()); return; }
		S s = STATE.get(pl.getUUID());
		if (s != null) monitorClear(s);
  if(s!=null && !MovementLifetime.reusable(s.ownerEntity,pl.getId(),s.ownerDimension,pl.level().dimension(),true)){STATE.remove(pl.getUUID());return;}
		if (s == null || !s.init) return;
		for (AbstractWindCharge wc : pl.level().getEntitiesOfClass(AbstractWindCharge.class, pl.getBoundingBox().inflate(10))) // own charges anywhere in range, anyone else's only close enough to knock the player
			if (BoostWindow.chargeCounts(wc.getOwner() == pl, pl.distanceToSqr(wc))) { s.windAt = System.currentTimeMillis(); Glide.wind(pl); break; }
		double sx = pl.getX(), sy = pl.getY(), sz = pl.getZ();
		boolean baseExempt = pl.isCreative() || pl.isSpectator() || pl.getAbilities().mayfly || pl.isPassenger() || pl.isFallFlying() || pl.isInWater() || pl.isInLava() || pl.onClimbable()
			|| pl.hasEffect(MobEffects.LEVITATION) || pl.hasEffect(MobEffects.SLOW_FALLING);
		// AntiKnockback: a hit pushes the player about half a block. Barely moving half a second after a hit, with open space behind, three times in a row, is not luck.
		long nowMs = System.currentTimeMillis();
		// NoFall hands the fall back: the hack kept the server's fall count at zero, so when the player really lands the drop we recorded is applied, unless vanilla already counted it.
		if (s.nfAt != 0 && !acts("nofall")) { s.nfAt = 0; s.fdMax = 0; } // monitor mode: nothing pending survives
		if (MonitorGate.processNoFall(s.nfAt, acts("nofall"))) {
			AABB tb = pl.getBoundingBox();
			boolean supported = !pl.level().noCollision(pl, new AABB(tb.minX, tb.minY - 0.1, tb.minZ, tb.maxX, tb.minY, tb.maxZ));
			s.fdMax = Math.max(s.fdMax, pl.fallDistance);
			if (supported && !pl.isCreative() && !pl.isSpectator()) {
				double drop = s.fallPeak - pl.getY();
				if (TRACE) Meshac.LOG.info("[trace] nofall land peak={} y={} drop={} fdMax={}", r(s.fallPeak), r(pl.getY()), r(drop), r(s.fdMax));
				if (MonitorGate.damage(acts("nofall"), drop, s.fdMax)) pl.causeFallDamage(drop, 1.0f, pl.damageSources().fall());
				s.nfAt = 0; s.fdMax = 0; s.fallPeak = pl.getY();
			} else if (nowMs - s.nfAt > 15000 || pl.isInWater() || pl.isInLava() || pl.isFallFlying() || pl.onClimbable()) { s.nfAt = 0; s.fdMax = 0; }
		}
		if (s.nfStrongAt != 0) { // P3-strong draft, log-only: shadow of the handback watcher above; never arms MonitorGate, never deals damage
			AABB sb = pl.getBoundingBox();
			boolean sSupported = !pl.level().noCollision(pl, new AABB(sb.minX, sb.minY - 0.1, sb.minZ, sb.maxX, sb.minY, sb.maxZ));
			s.fdStrongMax = Math.max(s.fdStrongMax, pl.fallDistance);
			if (sSupported && !pl.isCreative() && !pl.isSpectator()) {
				Meshac.LOG.info("[trace] nofall strong-handback WOULD fire {} peak={} y={} drop={} fdMax={} (P3-strong draft, log-only)", pl.getGameProfile().name(), r(s.fallPeak), r(pl.getY()), r(s.fallPeak - pl.getY()), r(s.fdStrongMax));
				s.nfStrongAt = 0; s.fdStrongMax = 0;
			} else if (nowMs - s.nfStrongAt > 15000 || pl.isInWater() || pl.isInLava() || pl.isFallFlying() || pl.onClimbable()) { s.nfStrongAt = 0; s.fdStrongMax = 0; }
		}
		net.minecraft.world.damagesource.DamageSource lds = pl.getLastDamageSource(); // only a mob or player swing pushes this way: blasts, falls and wind charges do not count
		boolean meleeHurt = lds != null && lds.getDirectEntity() instanceof net.minecraft.world.entity.LivingEntity && System.currentTimeMillis() - s.windAt > 5000;
		if (pl.hurtTime > 0 && s.kbAt == 0 && meleeHurt) { s.kbAt = nowMs; s.kbX = sx; s.kbY = sy; s.kbZ = sz; }
		else if (s.kbAt != 0 && nowMs - s.kbAt > 450) {
			boolean open = pl.level().noCollision(pl, pl.getBoundingBox().inflate(0.8, -0.1, 0.8));
			if (TRACE) Meshac.LOG.info("[trace] kb eval open={} ex={} blk={} sh={} hp={}", open, baseExempt, pl.isBlocking(), pl.isShiftKeyDown(), pl.getHealth());
			if (open && !baseExempt && !pl.isBlocking() && !pl.isShiftKeyDown() && pl.getHealth() > 0) {
				double moved = Math.max(Math.hypot(sx - s.kbX, sz - s.kbZ), sy - s.kbY); // a blast from below throws a player up, not sideways
				if (TRACE) Meshac.LOG.info("[trace] kb moved={} pkt={}", r(moved), r(Math.hypot(s.cx - s.kbX, s.cz - s.kbZ)));
				s.noKb = moved < 0.3 ? s.noKb + 1 : 0;
			}
			s.kbAt = 0;
			if (s.noKb >= 3) { s.noKb = 0; Verdict.signal(pl, "antiknockback", "no push after three hits", 2); }
		}
	}

	/** Packets that carry only the on-ground flag. A NoFall hack sends these while falling to reset fall damage. */
	private static double[] statusOnly(ServerPlayer pl, ServerboundMovePlayerPacket p, S s) {
		if (!p.isOnGround() || !s.init || s.grace > 0 || pl.isCreative() || pl.isSpectator() || pl.getAbilities().mayfly || pl.isPassenger()
			|| pl.isFallFlying() || pl.isInWater() || pl.isInLava() || pl.onClimbable()) { s.bufStatus = 0; return null; }
		AABB b = pl.getBoundingBox();
		boolean air = pl.level().noCollision(pl, new AABB(b.minX, b.minY - 0.1, b.minZ, b.maxX, b.minY, b.maxZ));
		s.bufStatus = air ? s.bufStatus + 1 : 0;
		if (s.bufStatus < 3) return null;
		s.bufStatus = 0;
		if (MonitorGate.armNoFall(acts("nofall"))) s.nfAt = System.currentTimeMillis(); if (TRACE) Meshac.LOG.info("[trace] nofall status flag peak={} y={}", r(s.fallPeak), r(pl.getY()));
		Verdict.Step step = Verdict.signal(pl, "nofall", "ground flag in mid-air");
		if (step == Verdict.Step.LOG || !acts("nofall")) return null; // soft mode / monitor mode
		if (step == Verdict.Step.HOLD) { s.freezeUntil = System.currentTimeMillis() + 1500; s.freezeCheck = "nofall"; }
		return new double[] {pl.getX(), pl.getY(), pl.getZ(), 0};
	}

	/** True when the body, slid from where the server has it to the claimed spot in half-block steps, overlaps a solid block. */
	private static boolean blocked(ServerPlayer pl, double dx, double dy, double dz) {
		// the route vanilla collision sweeps (Y, then the larger horizontal axis, then the other), not the straight line: launches clip corners the straight line crosses
		return MovePath.blocked((ox, oy, oz) -> !pl.level().noCollision(pl, pl.getBoundingBox().move(ox, oy, oz).deflate(0.1)), dx, dy, dz, 0.5);
	}

	/** Trace only (MESHAC_TRACE): which blocks sit around the player when a ground claim has air below, so partial-block and ledge cases can be told from a spoof. */
	private static void groundCtx(ServerPlayer pl, S s, boolean flagged, boolean air, boolean ground, double dy, double dtS, boolean exempt) {
		net.minecraft.core.BlockPos p = pl.blockPosition();
		net.minecraft.world.level.block.state.BlockState f = pl.level().getBlockState(p), b1 = pl.level().getBlockState(p.below()), b2 = pl.level().getBlockState(p.below(2));
		Meshac.LOG.info(GroundTrace.line(pl.getGameProfile().name(), flagged, id(f), top(pl, p, f), id(b1), top(pl, p.below(), b1), id(b2), air, ground, s.ground, s.bufGround, dy, dtS, exempt));
	}
	private static String id(net.minecraft.world.level.block.state.BlockState st) { return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString(); }
	private static double top(ServerPlayer pl, net.minecraft.core.BlockPos p, net.minecraft.world.level.block.state.BlockState st) {
		net.minecraft.world.phys.shapes.VoxelShape sh = st.getCollisionShape(pl.level(), p);
		return sh.isEmpty() ? 0 : sh.max(net.minecraft.core.Direction.Axis.Y);
	}

	/** The block whose collision shape tops the 0.13 strip: highest colliding shape top plus its block id. */
	static final class SupportHit { final double top; final String id; SupportHit(double top, String id) { this.top = top; this.id = id; } }

	/** Derives the support from the blocks whose collision shapes actually reach the strip, not from a point sample:
	 * a center-X/Z sample floors into the wrong block for snow layers (thin top near a block edge) and misses fences/walls/closed
	 * gates entirely (their collision reaches 1.5 above the block that owns it, one block below the strip floor), so every column
	 * under the strip is scanned one block below the strip floor and up. Shape BOUNDING boxes, not voxel-exact: a stair whose
	 * bounding box clips the strip can read as support where the voxel shape would not collide. Runs only on unsupported claims. */
	static SupportHit supportHit(net.minecraft.world.level.Level level, AABB strip) {
		double best = Double.NEGATIVE_INFINITY; String bestId = null;
		int x0 = (int) Math.floor(strip.minX), x1 = (int) Math.floor(strip.maxX - 1e-9), z0 = (int) Math.floor(strip.minZ), z1 = (int) Math.floor(strip.maxZ - 1e-9);
		int yLo = (int) Math.floor(strip.minY) - 1, yHi = (int) Math.floor(strip.maxY);
		net.minecraft.core.BlockPos.MutableBlockPos mp = new net.minecraft.core.BlockPos.MutableBlockPos();
		for (int yy = yLo; yy <= yHi; yy++) for (int xx = x0; xx <= x1; xx++) for (int zz = z0; zz <= z1; zz++) {
			mp.set(xx, yy, zz);
			net.minecraft.world.level.block.state.BlockState st = level.getBlockState(mp);
			if (st.isAir()) continue;
			net.minecraft.world.phys.shapes.VoxelShape sh = st.getCollisionShape(level, mp);
			if (sh.isEmpty()) continue;
			double sy0 = sh.min(net.minecraft.core.Direction.Axis.Y) + yy, sy1 = sh.max(net.minecraft.core.Direction.Axis.Y) + yy;
			double sx0 = sh.min(net.minecraft.core.Direction.Axis.X) + xx, sx1 = sh.max(net.minecraft.core.Direction.Axis.X) + xx;
			double sz0 = sh.min(net.minecraft.core.Direction.Axis.Z) + zz, sz1 = sh.max(net.minecraft.core.Direction.Axis.Z) + zz;
			if (sx1 <= strip.minX || sx0 >= strip.maxX || sz1 <= strip.minZ || sz0 >= strip.maxZ || sy1 <= strip.minY || sy0 >= strip.maxY) continue; // shape bounds miss the strip
			if (sy1 > best) { best = sy1; bestId = id(st); }
		}
		return bestId == null ? null : new SupportHit(best, bestId);
	}

	/** Compare mode for the candidate ground tolerance: MESHAC_GROUND_TOL=compare. Default off. Logs only, never changes a verdict. */
	private static final boolean GROUND_TOL_COMPARE = "compare".equals(System.getenv("MESHAC_GROUND_TOL"));
	private static final boolean GROUND_TOL_CLASSIFY = "classify".equals(System.getenv("MESHAC_GROUND_TOL")); // classify: tolerated partial-shape claims leave the evidence window; log-only tolerated lines
	private static void groundTolCompare(ServerPlayer pl, S s, AABB b) {
		long now = System.currentTimeMillis();
		if (now - s.gtLogAt < 1000) return; // one line a second per player
		boolean hit13 = !pl.level().noCollision(pl, new AABB(b.minX, b.minY - GroundTolerance.CANDIDATE, b.minZ, b.maxX, b.minY, b.maxZ));
		if (!hit13) return;
		net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing((b.minX + b.maxX) / 2, b.minY - GroundTolerance.CANDIDATE, (b.minZ + b.maxZ) / 2);
		net.minecraft.world.level.block.state.BlockState st = pl.level().getBlockState(p);
		double top = top(pl, p, st) + p.getY();
		if (GroundTolerance.wouldPass(true, true, top)) { s.gtLogAt = now; Meshac.LOG.info(GroundTolerance.line(pl.getGameProfile().name(), id(st), top, b.minY, s.bufGround)); }
	}

	/** Compare mode for the Bedrock sustain budget: MESHAC_BEDROCK_BUDGET=compare. Default off. Runs the Java (1.00x) budget on Bedrock players and logs when it would have flagged. Never changes a verdict. */
	private static final boolean BEDROCK_BUDGET_COMPARE = "compare".equals(System.getenv("MESHAC_BEDROCK_BUDGET"));
	private static final CompareLogBudget BEDROCK_COMPARE_LOGS = new CompareLogBudget();
	private static void bedrockShadow(ServerPlayer pl, S s, double h, double dtS, long nowMs, double slowEff, boolean web) {
		double per = SustainBudget.perTick(pl.getSpeed() / 0.13, nowMs - s.iceAt < 2000, s.slowTicks, slowEff, false, web, GlideWindow.capWindow(nowMs, s.glideAt), s.glideH);
		s.spShadow = SustainBudget.step(s.spShadow, h, per, dtS);
		if (SustainBudget.flagged(s.spShadow)) {
			long tick = pl.level().getServer().getTickCount();
			if (BEDROCK_COMPARE_LOGS.take(s.spShadowLog, tick)) Meshac.LOG.info("{} tick={} suppressedCaller={} suppressedGlobal={}", SustainBudget.shadowLine(pl.getGameProfile().name(), s.spShadow, per), tick, s.spShadowLog.suppressed(), BEDROCK_COMPARE_LOGS.suppressed());
			s.spShadow = 0;
		}
	}

	/** Log-only, rate-limited (MoveContext.noclipCtxDue): from/to coords and the block each end sits in for a noclip flag, so a pearl/piston embed can be told from a cheat in the log. Changes no behavior. */
	private static void noclipCtx(ServerPlayer pl, S s, double fx, double fy, double fz, double tx, double ty, double tz) {
		long nowMs = System.currentTimeMillis();
		if (!MoveContext.noclipCtxDue(nowMs, s.noclipCtxAt) || !CTX_LOGS.take(s.noclipBudget, System.nanoTime())) return;
		s.noclipCtxAt = nowMs;
		net.minecraft.world.level.Level lv = pl.level();
		String fb = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(lv.getBlockState(net.minecraft.core.BlockPos.containing(fx, fy, fz)).getBlock()));
		String tb = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(lv.getBlockState(net.minecraft.core.BlockPos.containing(tx, ty, tz)).getBlock()));
		Meshac.LOG.info("{} droppedCaller={} droppedGlobal={}", MoveContext.noclipLine(pl.getGameProfile().name() + (Meshac.skip(pl) ? " (bedrock)" : ""), fx, fy, fz, fb, tx, ty, tz, tb), s.noclipBudget.dropped(), CTX_LOGS.dropped());
	}

	/** Log-only: one context line per noclip or speed flag (rare), so the next log window can explain it. Changes no behavior. */
	private static final ContextLogBudget CTX_LOGS = new ContextLogBudget();
	private static void ctxLog(ServerPlayer pl, S s, double dx, double dy, double dz) {
  if (!CTX_LOGS.take(s.ctxBudget, System.nanoTime())) return;
		var ds = pl.getLastDamageSource();
		double dtS = s.prevDtS == 0 ? 0.05 : s.prevDtS; // the elapsed time the speed budget used for the latest packet
		Meshac.LOG.info("{} droppedCaller={} droppedGlobal={}", MoveContext.line(pl.getGameProfile().name() + (Meshac.skip(pl) ? " (bedrock)" : "") + (s.pending ? " (awaiting-tp-ack)" : ""), dx, dy, dz, dtS, pl.onGround(), s.ground, pl.isInWater(), pl.isInLava(), pl.onClimbable(), pl.isPassenger(), pl.isFallFlying(),
			s.grace, pl.hurtTime, ds == null ? null : ds.getMsgId(), System.currentTimeMillis(), s.windAt, s.kbAt, s.glideAt, s.graceAt), s.ctxBudget.dropped(), CTX_LOGS.dropped());
	}

	private static final boolean TRACE = System.getenv("MESHAC_TRACE") != null;
	private static double r(double v) { return Math.round(v * 1000) / 1000.0; }

	private static boolean near(double ax, double ay, double az, double bx, double by, double bz) {
		return Math.abs(ax - bx) < 0.01 && Math.abs(ay - by) < 0.01 && Math.abs(az - bz) < 0.01;
	}

	/** A mace smash with Wind Burst throws the player upward like a wind charge does. */
	public static void maceSmash(ServerPlayer pl) { S s = STATE.get(pl.getUUID()); if (s != null) s.windAt = System.currentTimeMillis(); Glide.wind(pl); }
	public static void forget(UUID id) { STATE.remove(id); }
}
