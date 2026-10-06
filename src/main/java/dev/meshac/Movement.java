package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

/** Movement checks: speed, fly (hover/ascend), high jump. Runs on every position packet, before vanilla. */
public final class Movement {
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int SIGNAL_AT = 6;      // buffered violations before a setback
	private static final int SKIP_TICKS = 20;  // grace after teleport, damage, effects

	private static final class S {
		double px, py, pz, cx, cy, cz, dy, goodX, goodY, goodZ;
		boolean ground, cground, init, ownTp;
		long lastNs, freezeUntil; final java.util.concurrent.ConcurrentLinkedQueue<Long> arrivals = new java.util.concurrent.ConcurrentLinkedQueue<>(); double balMs; int inBlock; boolean pending; long graceAt; long kbAt; double kbX, kbZ; int noKb, levT, bufLev, bufHop, hopClock, bufGround, grace, clean, bufSpeed, bufFly, bufJump, bufClimb, bufStatus, riseT, slowTicks; double rise;
	}

	/** Netty thread: stamp when a position packet really arrived. The main thread only sees it at the next tick. */
	public static void arrive(ServerPlayer pl, ServerboundMovePlayerPacket p) {
		if (p.hasPosition()) STATE.computeIfAbsent(pl.getUUID(), k -> new S()).arrivals.add(System.nanoTime());
	}

	/** Whether the server is waiting for the client to confirm a teleport. Set by the packet handler before check(). */
	public static void awaiting(ServerPlayer pl, boolean waiting) { STATE.computeIfAbsent(pl.getUUID(), k -> new S()).pending = waiting; }

	/** Returns a setback position, or null if the packet is fine. */
	public static double[] check(ServerPlayer pl, ServerboundMovePlayerPacket p) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		if (p.hasRotation()) Interact.look(pl, p.getYRot(pl.getYRot()), p.getXRot(pl.getXRot()));
		if (!p.hasPosition()) return statusOnly(pl, p, s);
		if (s.grace > 0 && System.currentTimeMillis() - s.graceAt > 1100) s.grace = 0; // grace is 20 ticks of real time; a player standing still sends few packets
		double x = p.getX(pl.getX()), y = p.getY(pl.getY()), z = p.getZ(pl.getZ());
		boolean ground = p.isOnGround();
		Long arrived = s.arrivals.poll(); // pairs 1:1 with the stamp from arrive()
		// The server position is the last accepted one (vanilla has already handled the previous packet).
		// If it is neither where the previous packet started nor where it claimed, the server moved the player.
		double sx = pl.getX(), sy = pl.getY(), sz = pl.getZ();
		boolean atStart = near(sx, sy, sz, s.px, s.py, s.pz), atClaim = near(sx, sy, sz, s.cx, s.cy, s.cz);
		// A real teleport moves the player a long way. Hacks that send several tiny fake positions in one tick (Criticals, MaceDMG) must not be able to open the grace window.
		double offBy = Math.min(Math.sqrt((sx - s.px) * (sx - s.px) + (sy - s.py) * (sy - s.py) + (sz - s.pz) * (sz - s.pz)), Math.sqrt((sx - s.cx) * (sx - s.cx) + (sy - s.cy) * (sy - s.cy) + (sz - s.cz) * (sz - s.cz)));
		if (!s.init || (!(atStart || atClaim) && offBy > 0.75)) {
			s.init = true; s.graceAt = System.currentTimeMillis(); s.grace = s.ownTp ? 3 : SKIP_TICKS; s.ownTp = false; // our own setback needs only a short grace
			s.dy = 0; s.ground = ground; s.bufSpeed = s.bufFly = s.bufJump = 0;
			s.goodX = sx; s.goodY = sy; s.goodZ = sz;
			s.px = sx; s.py = sy; s.pz = sz; s.cx = x; s.cy = y; s.cz = z; s.cground = ground;
			return null;
		}
		if (atClaim) { s.dy = s.cy - s.py; s.ground = s.cground; }
		if (System.currentTimeMillis() < s.freezeUntil) { // hold: ignore every move and keep putting the player back
			s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ;
			return new double[] {s.goodX, s.goodY, s.goodZ, 0};
		}
		// NoClip: the body sits inside solid blocks. Vanilla only refuses moves that newly collide, so a player who starts inside a wall walks on through it.
		if (!pl.isCreative() && !pl.isSpectator() && !pl.isPassenger() && s.grace == 0 && !pl.level().noCollision(pl, pl.getBoundingBox().deflate(0.1))) {
			if (++s.inBlock >= 10) { s.inBlock = 0; Verdict.signal(pl, "noclip", "moving inside solid blocks", 1); s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ; return new double[] {s.goodX, s.goodY, s.goodZ, 0}; }
		} else s.inBlock = 0;
		double dx = x - sx, dy = y - sy, dz = z - sz;
		// NoClip, Teleport, Blink. No exemption (damage, grace) covers these; only a teleport the server itself ordered does.
		// Any move over 1.5 blocks must not pass through solid blocks on the way (vanilla only tests where the packet ends); a move over 10 blocks is refused whatever is in between.
		double d2 = dx * dx + dy * dy + dz * dz;
		if (!s.pending && !pl.isCreative() && !pl.isSpectator() && !pl.isPassenger() && (d2 > 100 || d2 > 2.25 && blocked(pl, dx, dy, dz))) {
			Verdict.signal(pl, "noclip", d2 > 100 ? String.format("jumped %.0f blocks in one packet", Math.sqrt(d2)) : String.format("moved %.1f blocks through solid blocks", Math.sqrt(d2)), 2);
			s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ;
			return new double[] {s.goodX, s.goodY, s.goodZ, 0};
		}
		boolean exempt = pl.isCreative() || pl.isSpectator() || pl.getAbilities().mayfly || pl.isPassenger()
			|| pl.isFallFlying() || pl.isInWater() || pl.isInLava() || pl.onClimbable() || pl.hurtTime > 0
			|| pl.hasEffect(MobEffects.LEVITATION) || pl.hasEffect(MobEffects.SLOW_FALLING);
		if (pl.hurtTime > 0) { s.grace = SKIP_TICKS; s.graceAt = System.currentTimeMillis(); }
		// AntiKnockback: a hit pushes the player about half a block. Barely moving half a second after a hit, with open space behind, three times in a row, is not luck.
		long nowMs = System.currentTimeMillis();
		if (pl.hurtTime > 0 && s.kbAt == 0) { s.kbAt = nowMs; s.kbX = sx; s.kbZ = sz; }
		else if (s.kbAt != 0 && nowMs - s.kbAt > 450) {
			boolean open = pl.level().noCollision(pl, pl.getBoundingBox().inflate(0.8, -0.1, 0.8));
			if (open && !exempt && !pl.isBlocking() && !pl.isShiftKeyDown() && pl.getHealth() > 0) {
				double moved = Math.hypot(sx - s.kbX, sz - s.kbZ);
				if (TRACE) Meshac.LOG.info("[trace] kb moved={}", r(moved));
				s.noKb = moved < 0.3 ? s.noKb + 1 : 0;
			}
			s.kbAt = 0;
			if (s.noKb >= 3) { s.noKb = 0; Verdict.signal(pl, "antiknockback", "no push after three hits", 2); }
		}
		if (TRACE) Meshac.LOG.info("[trace] {} dx={} dy={} dz={} g={} sg={} grace={} exempt={} hurt={}", pl.getGameProfile().name(), r(dx), r(dy), r(dz), ground, s.ground, s.grace, exempt, pl.hurtTime);
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
		if (s.grace > 0 || exempt) {
			if (s.grace > 0) s.grace--;
		} else {
			// Speed: horizontal blocks per packet. Ground and air have different vanilla ceilings (sprint-jump is the peak);
			// slippery blocks lift both. Scaled by the speed attribute (0.13 when sprinting).
			double fr = pl.level().getBlockState(pl.blockPosition().below()).getBlock().getFriction();
			double slow = Math.min(pl.level().getBlockState(pl.blockPosition().below()).getBlock().getSpeedFactor(),
				pl.level().getBlockState(pl.blockPosition()).getBlock().getSpeedFactor()); // soul sand is a short block: the player sinks into its cell
			boolean web = pl.level().getBlockState(pl.blockPosition()).is(net.minecraft.world.level.block.Blocks.COBWEB)
				|| pl.level().getBlockState(pl.blockPosition().above()).is(net.minecraft.world.level.block.Blocks.COBWEB);
			s.slowTicks = slow < 1.0 ? s.slowTicks + 1 : 0; // the first ticks on a slow block still carry normal momentum
			double cap = (s.ground && ground ? 0.34 : 0.62) * (pl.getSpeed() / 0.13) * (fr > 0.61 ? 3 : 1) * (s.slowTicks >= 5 ? Math.max(slow, 0.65) : 1.0);
			if (web) cap = 0.10; // vanilla cobweb scales motion by 0.25 or less
			double h = Math.hypot(dx, dz);
			if (h > cap) { s.bufSpeed += h > cap * 1.25 ? 3 : 1; s.clean = 0; } // a big overshoot counts triple
			else if (++s.clean >= 10) { s.bufSpeed = Math.max(0, s.bufSpeed - 1); s.clean = 0; }
			if (s.bufSpeed >= SIGNAL_AT) hit = String.format("speed %.2f>%.2f", h, cap);
			// Fly: in the air, vanilla gravity gives dy = (lastDy - 0.08) * 0.98. Going above that is not vanilla.
			if (!s.ground && !ground) {
				double expect = (s.dy - 0.08) * 0.98;
				s.bufFly = dy > expect + 0.03 ? s.bufFly + (dy > expect + 0.3 ? 3 : 1) : Math.max(0, s.bufFly - 1); // a big climb counts triple
				if (s.bufFly >= SIGNAL_AT && hit == null) hit = String.format("fly dy %.3f expect %.3f", dy, expect);
			} else s.bufFly = 0;
			// Step: a hack can climb a block with several small rises that all claim ground. Add them up.
			if (ground && dy > 0.05) { s.rise += dy; s.riseT = 4; } else if (--s.riseT <= 0) { s.rise = 0; s.riseT = 0; }
			if (s.rise > 0.65 + 0.1 * (pl.hasEffect(MobEffects.JUMP_BOOST) ? 3 : 0) && hit == null) { hit = String.format("step rose %.2f on the ground", s.rise); s.rise = 0; }
			// Step: claiming ground while rising more than a half block. Vanilla only auto-steps 0.6.
			if (ground && dy > 0.62 + 0.1 * (pl.hasEffect(MobEffects.JUMP_BOOST) ? 3 : 0) && hit == null) hit = String.format("step dy %.3f onGround", dy);
			// High jump / step: leaving the ground higher than a jump (0.42 + jump boost) or a step (0.6).
			if (s.ground && dy > 0.62 + 0.1 * (pl.hasEffect(MobEffects.JUMP_BOOST) ? 3 : 0)) {
				s.bufJump += 2;
				if (s.bufJump >= 2 && hit == null) hit = String.format("jump dy %.3f", dy);
			} else s.bufJump = Math.max(0, s.bufJump - 1);
			// Micro hop: leaving the ground by less than a jump (0.42) with nothing under the new spot. Criticals (packet and mini jump) and MaceDMG do this on every hit.
			if (s.ground && dy > 0.005 && dy < 0.30 && !pl.isInWater() && !pl.onClimbable() && !pl.isPassenger() && !pl.getAbilities().flying) {
				AABB mb = pl.getBoundingBox().move(dx, dy, dz);
				boolean unsupported = pl.level().noCollision(pl, new AABB(mb.minX, mb.minY - 0.03, mb.minZ, mb.maxX, mb.minY, mb.maxZ));
				if (TRACE) Meshac.LOG.info("[trace] microhop dy={} unsupported={} buf={}", r(dy), unsupported, s.bufHop);
				if (unsupported) s.bufHop += 2;
				if (s.bufHop >= 3 && hit == null) { hit = String.format("microhop dy %.3f", dy); s.bufHop = 0; }
			}
			if (++s.hopClock >= 30) { s.hopClock = 0; s.bufHop = Math.max(0, s.bufHop - 1); } // one hop is forgiven, two inside about 1.5 s are not
		}
		// Timer: each move packet is worth 50 ms. Packets running ahead of the real clock = game speed hack.
		long now = arrived != null ? arrived : System.nanoTime();
		if (s.lastNs != 0) {
			// Teleports make the client send extra confirm packets, so no timing while in grace.
			s.balMs = s.grace > 0 ? 0 : s.balMs + 50 - (now - s.lastNs) / 1e6;
			s.balMs = Math.max(-300, Math.min(s.balMs, 600)); // lag may bank up to 300 ms of catch-up
			if (s.balMs > 450 && hit == null) hit = String.format("timer ahead %.0f ms", s.balMs);
		}
		s.lastNs = now;
		// NoFall / ground spoof: claims to stand on something with only air below.
		if (ground && !exempt && s.grace <= 0) {
			AABB b = pl.getBoundingBox().move(dx, dy, dz);
			boolean air = pl.level().noCollision(pl, new AABB(b.minX, b.minY - 0.1, b.minZ, b.maxX, b.minY, b.maxZ));
			s.bufGround = air ? s.bufGround + 1 : 0;
			if (s.bufGround >= 3 && hit == null) hit = "nofall ground spoof";
		} else s.bufGround = 0;
		if (hit != null) {
			String[] ch = hit.split(" ", 2);
			Verdict.Step step = Verdict.signal(pl, ch[0], ch.length > 1 ? ch[1] : "", ch[0].equals("step") ? 3 : ch[0].equals("microhop") ? 2 : 1); // a block climbed in a few ticks is never an accident
			s.ownTp = true;
			s.bufSpeed = s.bufFly = s.bufJump = s.bufGround = s.bufClimb = s.bufHop = 0; s.balMs = 0; s.grace = 2; s.graceAt = System.currentTimeMillis(); s.dy = 0;
			s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ;
			if (step == Verdict.Step.HOLD) s.freezeUntil = System.currentTimeMillis() + 1500;
			return new double[] {s.goodX, s.goodY, s.goodZ, 0};
		}
		if (s.bufSpeed == 0 && s.bufFly == 0 && s.bufJump == 0 && s.inBlock == 0) { s.goodX = x; s.goodY = y; s.goodZ = z; }
		s.px = sx; s.py = sy; s.pz = sz; s.cx = x; s.cy = y; s.cz = z; s.cground = ground;
		return null;
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
		Verdict.Step step = Verdict.signal(pl, "nofall", "ground flag in mid-air");
		if (step == Verdict.Step.HOLD) s.freezeUntil = System.currentTimeMillis() + 1500;
		return new double[] {pl.getX(), pl.getY(), pl.getZ(), 0};
	}

	/** True when the body, slid from where the server has it to the claimed spot in half-block steps, overlaps a solid block. */
	private static boolean blocked(ServerPlayer pl, double dx, double dy, double dz) {
		int n = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / 0.5);
		for (int i = 1; i < n; i++) if (!pl.level().noCollision(pl, pl.getBoundingBox().move(dx * i / n, dy * i / n, dz * i / n).deflate(0.1))) return true;
		return false;
	}

	private static final boolean TRACE = System.getenv("MESHAC_TRACE") != null;
	private static double r(double v) { return Math.round(v * 1000) / 1000.0; }

	private static boolean near(double ax, double ay, double az, double bx, double by, double bz) {
		return Math.abs(ax - bx) < 0.01 && Math.abs(ay - by) < 0.01 && Math.abs(az - bz) < 0.01;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
