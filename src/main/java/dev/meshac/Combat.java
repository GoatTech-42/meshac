package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Melee checks, one call per attack: reach, line of sight, aim, attack rate, multi-target, robotic aim. */
public final class Combat {
	private static volatile net.minecraft.server.MinecraftServer recorderServer;
	public static void recorderServer(net.minecraft.server.MinecraftServer server) { recorderServer = server; }
	public static final CombatRecorder RECORDER = new CombatRecorder(() -> recorderServer != null && recorderServer.isSameThread());
	private static final java.util.Map<java.util.UUID, long[]> MULTI = new java.util.concurrent.ConcurrentHashMap<>();
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final class S { Object dimension; final CombatMissWindow aimMisses = new CombatMissWindow(); final int[] aimTicks = new int[12]; final CombatLogBudget logs = new CombatLogBudget(); final LosSnapshotCache los = new LosSnapshotCache(8); final CombatFeatures features = new CombatFeatures(); int featureHits; long traceNs; int traceTick; float traceYaw, tracePitch; Vec3 traceEye, traceCentre; int fakeCrit, missN; long missAt; long fakeAt; long[] rt = new long[12]; double[] re = new double[12]; int[] ri = new int[12]; int rn; long swapMs, swapWinMs, hitAt, cycleStart; boolean swapDiff; int swapHits, cycles; long g1, g2; int prevSlot = -1; long lastMs, lastId; int lastEntity = -1, weak, fast, perfect, snap, n; float lastYaw; final long[] gaps = new long[6]; final double[] errs = new double[6]; final int[] ids = new int[6]; int en; long windowMs; int hits; double lastErr = -1; double lastAtkY = Double.NaN; }

 /** Do not carry aim/multi-target evidence or LOS cache between dimensions. */
 private static S state(ServerPlayer pl) {
  Object dim = pl.level().dimension();
  return STATE.compute(pl.getUUID(), (id, old) -> {
   if (old != null && java.util.Objects.equals(old.dimension, dim)) return old;
   S fresh = new S(); fresh.dimension = dim; return fresh;
  });
 }

	/** No Verdict call and no return reason: ambiguous patterns cannot heat, cancel or punish. */
	private static void lowConfidence(ServerPlayer pl, String pattern) {
		S state = state(pl);
		if (state.logs.allow(CombatLogBudget.Kind.EVIDENCE, System.nanoTime())) Meshac.LOG.info("[combat-evidence] confidence=low punishEligible=false player={} pattern={} suppressed={}", pl.getGameProfile().name(), pattern, state.logs.takeSuppressed(CombatLogBudget.Kind.EVIDENCE));
	}

	/** Called when the player picks another hotbar slot. */
	public static void swapped(ServerPlayer pl, int slot) {
		S s = state(pl);
		long now = System.currentTimeMillis();
		// Swap, hit, swap back. A hand can do it once; a module repeats it with the same gaps every time.
		if (s.hitAt > s.swapMs && now - s.hitAt < 250 && now - s.swapMs < 400) {
			long a = s.hitAt - s.swapMs, b = now - s.hitAt;
			if (now - s.cycleStart > 20000) { s.cycles = 0; s.cycleStart = now; }
			boolean same = s.cycles > 0 && Math.abs(a - s.g1) <= 12 && Math.abs(b - s.g2) <= 12;
			boolean oneTick = a < 30;
			if (same || oneTick) s.cycles++; else s.cycles = Math.max(0, s.cycles - 1);
			s.g1 = a; s.g2 = b;
			if (Trace.ON && s.logs.allow(CombatLogBudget.Kind.TRACE, System.nanoTime())) Trace.t(pl, "swap cycle to-hit=" + a + " back=" + b + " cycles=" + s.cycles);
			if (s.cycles >= 3) { s.cycles = 0; lowConfidence(pl, "swap, hit and swap back on a timer (" + a + " / " + b + " ms)"); }
		}
		s.swapMs = now;
		s.swapDiff = pl.getInventory().getItem(slot).getItem() != pl.getMainHandItem().getItem();
	}

	/** Tick observations of the eye, target box and visibility, kept for 15 ticks (750 ms at 20 TPS). */
	private record Sample(long tick, Object dimension, UUID identity, Vec3 eye, AABB box, LosSnapshotCache.Result visibility) {}
	private static final int REWIND_TICKS = 15;
	private static final Map<UUID, Map<Integer, java.util.ArrayDeque<Sample>>> TRACK = new ConcurrentHashMap<>();

	/** END_SERVER_TICK: remember where this player and each living thing near them stood, so an attack can be judged against what the client could actually see when it clicked. */
	public static void tick(ServerPlayer pl, int tick) {
		if (Config.get().skips(pl, "combat")) return;
		Map<Integer, java.util.ArrayDeque<Sample>> m = TRACK.computeIfAbsent(pl.getUUID(), k -> new ConcurrentHashMap<>());
		Vec3 eye = pl.getEyePosition();
		java.util.Set<Integer> seen = new java.util.HashSet<>();
		var nearby = pl.level().getEntities(pl, pl.getBoundingBox().inflate(12), x -> x != pl && x instanceof net.minecraft.world.entity.LivingEntity);
		S state = state(pl);
		for (int i = 0; i < nearby.size(); i++) {
			Entity e = nearby.get(i);
			java.util.ArrayDeque<Sample> dq = m.computeIfAbsent(e.getId(), k -> new java.util.ArrayDeque<>());
			// Eight observations per player/tick, at most four clips each. Unknown
			// snapshots must not turn budget pressure into a false blocked verdict.
			boolean selected = LosSnapshotCache.selected(tick, i, nearby.size(), 8);
			LosSnapshotCache.Result los = state.los.sample(tick, e.getId(), selected, () -> visible(pl, e, eye, e.getBoundingBox()));
			dq.addLast(new Sample(tick, pl.level().dimension(), e.getUUID(), eye, e.getBoundingBox(), los));
			while (!dq.isEmpty() && (dq.size() > REWIND_TICKS || !validSample(pl, dq.peekFirst()))) dq.pollFirst();
			seen.add(e.getId());
		}
		m.keySet().removeIf(id -> !seen.contains(id));
	}

	private static boolean validSample(ServerPlayer pl, Sample sample) {
		int tick = pl.level().getServer().getTickCount();
		return CombatEvidence.recentTick(tick, sample.tick, REWIND_TICKS) && pl.level().dimension().equals(sample.dimension);
	}

 private static boolean validSample(ServerPlayer pl, Sample sample, Entity target) {
  return validSample(pl, sample) && CombatEvidence.sameSubject(pl.level().dimension(), sample.dimension, target.getUUID(), sample.identity);
 }

	/** True when any of several points on the target can be reached by a straight line from the eyes (closest point, centre, feet, eyes). A wall must hide all of them. */
	private static boolean visible(ServerPlayer pl, Entity target, Vec3 eye, AABB box) {
		Vec3[] pts = {
			new Vec3(Math.max(box.minX, Math.min(eye.x, box.maxX)), Math.max(box.minY, Math.min(eye.y, box.maxY)), Math.max(box.minZ, Math.min(eye.z, box.maxZ))),
			box.getCenter(), target.getEyePosition(), new Vec3(box.getCenter().x, box.minY + 0.1, box.getCenter().z)
		};
		for (Vec3 p : pts) {
			var r = pl.level().clip(new net.minecraft.world.level.ClipContext(eye, p, net.minecraft.world.level.ClipContext.Block.COLLIDER, net.minecraft.world.level.ClipContext.Fluid.NONE, pl));
			if (r.getType() == net.minecraft.world.phys.HitResult.Type.MISS) return true;
		}
		return false;
	}

	/** Returns a reason when this attack is not something a person at a keyboard could have sent, else null. */
	public static String check(ServerPlayer pl, Entity target) {
		if (Config.get().skips(pl, "combat")) return null;
		S s = state(pl);
		dev.meshac.veil.Veil.attacked(pl, target, pl.level().getServer().getTickCount());
		long now = System.currentTimeMillis();
		Vec3 eye = pl.getEyePosition();
		AABB box = target.getBoundingBox();
		double reach = Math.sqrt(box.distanceToSqr(eye));
		String hit = null;
		// Reach: vanilla entity interaction range is 3.0 (plus a small margin for hitbox growth and rounding), judged against every (eye, box) pair from the last 750 ms - what the client could actually see when it clicked. Latency widens the window, not the limit: a static target gives no slack, and 750 ms back a slow connection still finds its legal click.
		double best = reach;
		Map<Integer, java.util.ArrayDeque<Sample>> track = TRACK.get(pl.getUUID());
		if (track != null) {
			java.util.ArrayDeque<Sample> dq = track.get(target.getId());
			if (dq != null && !dq.isEmpty()) {
				// Current server position is a legal candidate too, even before END_SERVER_TICK samples it.
				for (Sample sm : dq) if (validSample(pl, sm, target)) best = CombatEvidence.closest(best, Math.sqrt(sm.box.distanceToSqr(sm.eye)));
			}
		}
		if (best > 3.2) hit = String.format("reach %.2f", best);
		// Line of sight: a wall between the eyes and the target.
		if (hit == null && reach > 0.5 && !visible(pl, target, eye, box)) {
			// Judge LOS against sampled visibility as well as current visibility. Do not
			// re-clip an old position against newly closed doors or placed blocks.
			int losSeen = 0, losBlocked = 0;
			if (track != null) {
				var dq = track.get(target.getId());
				if (dq != null) for (Sample sm : dq) { if (!validSample(pl, sm, target)) continue; if (sm.visibility == LosSnapshotCache.Result.VISIBLE) losSeen++; else if (sm.visibility == LosSnapshotCache.Result.BLOCKED) losBlocked++; }
			}
			// UNKNOWN (over the tick's observation budget) is not evidence either way: it must
			// not veto a blocked verdict, or 9+ nearby entities would blind the LOS check.
			if (losSeen == 0 && losBlocked >= 2) hit = "hit through a block";
		}
		// Aim: the look ray has to pass through the hitbox (grown a little for the client's rounding).
		Vec3 look = pl.getLookAngle();
		// The client aimed at where the target was a few ticks ago, so a quick mob (spider, a player sprinting) is tested over the ground it just covered.
		Vec3 mv = target.getDeltaMovement();
		double bx = Math.max(-2, Math.min(2, mv.x * -4)), bz = Math.max(-2, Math.min(2, mv.z * -4)); // sideways only: gravity is not a reason to look lower
		AABB aimed = new AABB(box.minX + Math.min(0, bx), box.minY, box.minZ + Math.min(0, bz), box.maxX + Math.max(0, bx), box.maxY, box.maxZ + Math.max(0, bz)).inflate(0.6);
		var ray = aimed.clip(eye, eye.add(look.scale(6)));
		boolean aimAccepted = aimed.contains(eye) || ray.isPresent();
		// Rewind the eye and target together, including vertical motion. Current
		// velocity alone cannot describe a jump, bounce or a moving attacker.
		if (!aimAccepted && track != null) {
			var history = track.get(target.getId());
			if (history != null) for (Sample sm : history) {
				if (!validSample(pl, sm, target)) continue;
				AABB oldBox = sm.box.inflate(0.6);
				boolean oldAim = oldBox.contains(sm.eye) || oldBox.clip(sm.eye, sm.eye.add(look.scale(6))).isPresent();
				aimAccepted = CombatEvidence.eitherAim(aimAccepted, oldAim);
				if (aimAccepted) break;
			}
		}
		if (s.aimMisses.observe(target.getUUID(), target.getId(), pl.level().getServer().getTickCount(), System.nanoTime(), aimAccepted || reach <= 0.15 || hit != null) && hit == null) hit = "not looking at target";
		// // inside the hitbox the ray has no entry point; anywhere else a miss of the box is a miss
		// Robotic aim: the look ray hits the exact same spot of the box hit after hit. People wobble by a few centimetres.
		Vec3 centre = box.getCenter();
		// Closest approach of the look ray to the box centre. Distance from the entry point is nearly constant for any central hit, so it hides wobble.
		double err = ray.isPresent() ? centre.subtract(eye.add(look.scale(Math.max(0, centre.subtract(eye).dot(look))))).length() : 9.0;
		if (s.lastErr >= 0 && Math.abs(err - s.lastErr) < 0.004 && err < 1) s.perfect++; else s.perfect = Math.max(0, s.perfect - 1);
		s.lastErr = err;
		// Holding a still mouse on a stationary target is legal. Same-target steadiness is not evidence of automation.
		// Same aim error on different targets: the look ray lands at the same spot of every box. People are never that even across targets.
		if (s.lastMs != 0 && now - s.lastMs > 3000) s.en = 0; // an old fight does not count towards this one
		s.errs[s.en % 6] = err; s.ids[s.en % 6] = target.getId(); s.en++;
		// Precise or consistent aim, including across targets, is legal and does not prove automation.

		// Attack rate: a sustained stretch of sub-70 ms handling gaps. Four or five fast legal clicks, or a stalled
		// link releasing a handful of queued swings, arrive bunched (candidate4 audit) - longer batches still cannot prove automation; these patterns are evidence only.
		if (now - s.lastMs < 70) s.fast++; else if (now - s.lastMs >= 150) s.fast = 0; else s.fast = Math.max(0, s.fast - 1);
		if (hit == null && s.fast >= 12) { lowConfidence(pl, "attack rate"); s.fast = 0; }
		// Cooldown charge is a damage mechanic, not input proof. Retain the repeated rhythm as evidence only.
		if (pl.getAttackStrengthScale(0.5f) < 0.5f && s.lastMs != 0 && now - s.lastMs < 300) s.weak++; else s.weak = Math.max(0, s.weak - 1);
		if (hit == null && s.weak >= 10) { lowConfidence(pl, "hitting before the cooldown"); s.weak = 0; }
		s.hitAt = now;
		// Criticals: the client says "in the air" for a few centimetres so the server counts a fall and the hit crits. A real crit needs a real fall.
		double fd = pl.fallDistance;
		// Equal attack-time endpoints do not prove flat motion: genuine jumps can repeat a phase.
		// Keep this legacy pattern as low-confidence evidence only, never a consistency verdict.
		boolean flatSinceLast = !Double.isNaN(s.lastAtkY) && Math.abs(pl.getY() - s.lastAtkY) < 0.05;
		if (!pl.onGround() && fd > 0 && fd < 0.3 && flatSinceLast && !pl.onClimbable() && !pl.isInWater() && !pl.isPassenger() && !pl.getAbilities().mayfly) {
			if (now - s.fakeAt > 10000) s.fakeCrit = 0;
			s.fakeAt = now;
			if (++s.fakeCrit >= 10 && hit == null) { lowConfidence(pl, String.format("short-fall attack pattern %.2f", fd)); s.fakeCrit = 0; }
		}
		// Multi-target aim evidence must use actual ray misses, not distance from a box centre.
		// A valid edge hit or a rewound hit on a large entity must never count as a miss.
		int k = s.rn++ % 12; s.rt[k] = now; s.re[k] = aimAccepted ? 0.0 : 9.0; s.ri[k] = target.getId(); s.aimTicks[k] = pl.level().getServer().getTickCount();
		if (hit == null) {
			java.util.Set<Integer> who = new java.util.HashSet<>(), observedTicks = new java.util.HashSet<>(); java.util.List<Double> es = new java.util.ArrayList<>();
			for (int i = 0; i < Math.min(s.rn, 12); i++) if (now - s.rt[i] < 5000) { who.add(s.ri[i]); observedTicks.add(s.aimTicks[i]); es.add(s.re[i]); }
			if (es.size() >= 8 && who.size() >= 4 && observedTicks.size() >= 4) { java.util.Collections.sort(es); if (CombatEvidence.multiAimViolation(aimAccepted, es.size(), who.size(), observedTicks.size(), es.get(es.size() / 2))) { hit = "hits on " + who.size() + " targets without aiming at them"; s.rn = 0; } }
		}
		// Multi-target: same-tick target changes. Tick coalescing delivers legal spaced clicks together (candidate4 audit),
		// so only a repeated batch pattern counts; the aimed multi-target check above carries the real detection.
		if (hit == null && s.lastEntity != -1 && s.lastEntity != target.getId() && now - s.lastMs < 50) {
			long[] mt = MULTI.computeIfAbsent(pl.getUUID(), kk -> new long[2]);
			if (now - mt[0] > 10000) mt[1] = 0;
			mt[0] = now;
			if (++mt[1] >= 4) { mt[1] = 0; lowConfidence(pl, "two targets in one tick"); }
		}
		// Snapping: a fast large turn onto a different target that still lands near the centre, three times running.
		// Accurate skilled flicks can look identical, so this is low-confidence evidence only.
		float yaw = pl.getYRot(); float turn = Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - s.lastYaw));
		s.snap = CombatEvidence.nextConsecutive(s.snap, s.lastEntity != -1 && s.lastEntity != target.getId() && now - s.lastMs < 250 && turn > 60 && err < 0.6);
		if (hit == null && s.snap >= 3) { lowConfidence(pl, "snapping between targets"); s.snap = 0; }
		s.lastYaw = yaw;
		// Metronome: six attacks in a row with almost the same gap between them. Hands do not keep time to 15 ms.
		long gap = s.lastMs == 0 ? 0 : now - s.lastMs;
		if (gap > 250 && gap < 1500) { s.gaps[s.n++ % 6] = gap; } else if (gap >= 1500) s.n = 0;
		if (hit == null && s.n >= 6) {
			double mean = 0; for (long g : s.gaps) mean += g; mean /= 6;
			double var = 0; for (long g : s.gaps) var += (g - mean) * (g - mean);
			// A regular legal click rhythm or packet delivery cadence is not enough to convict.
			if (Math.sqrt(var / 6) < 15) s.n = 0;
		}
		// Features 1/7/10 are descriptive observations, never evidence of guilt by themselves.
		double radial = Math.hypot(centre.x - eye.x, centre.z - eye.z);
		if (radial > 1e-6) {
			double sideX = -(centre.z - eye.z) / radial, sideZ = (centre.x - eye.x) / radial;
			Vec3 offset = eye.add(look.scale(Math.max(0, centre.subtract(eye).dot(look)))).subtract(centre);
			double lateral = offset.x * sideX + offset.z * sideZ;
			double vertical = offset.y / Math.max(0.01, box.maxY - box.minY);
			double targetSideSpeed = mv.x * sideX + mv.z * sideZ;
			double attackSpeed = pl.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
			RECORDER.add(pl.getUUID(), System.nanoTime(), pl.level().getServer().getTickCount(), target.getId(), lateral, vertical, targetSideSpeed, pl.getAttackStrengthScale(0.5f), attackSpeed, s.lastMs == 0 ? -1 : now - s.lastMs, pl.connection.latency(), reach, err);
			CombatFeatures.Summary evidence = s.features.observe(System.nanoTime(), target.getId(), pl.getMainHandItem().getItem().toString() + ":" + pl.getInventory().getSelectedSlot(), lateral, vertical, targetSideSpeed, pl.getAttackStrengthScale(0.5f), 1000.0 / attackSpeed);
			if (Trace.ON && s.logs.allow(CombatLogBudget.Kind.TRACE, System.nanoTime())) Meshac.LOG.info("[combat-features-raw] confidence=unmeasured punishEligible=false player={} target={} lateral={} vertical={} targetSideSpeed={} charge={} attackSpeed={} gapHandlingMs={} pingMs={}", pl.getGameProfile().name(), target.getId(), lateral, vertical, targetSideSpeed, pl.getAttackStrengthScale(0.5f), attackSpeed, s.lastMs == 0 ? -1 : now - s.lastMs, pl.connection.latency());
			if (evidence != null && evidence.n() >= 8 && ++s.featureHits % 8 == 0 && s.logs.allow(CombatLogBudget.Kind.SUMMARY, System.nanoTime())) Meshac.LOG.info("[combat-features] confidence=unmeasured punishEligible=false player={} target={} features=1,7,10 summary={}", pl.getGameProfile().name(), target.getId(), evidence);
		}
		if (pl.getMainHandItem().getItem().toString().contains("mace")) Movement.maceSmash(pl);
		if (Trace.ON && s.logs.allow(CombatLogBudget.Kind.TRACE, System.nanoTime())) Meshac.LOG.info("[trace] attack {} t={} reach={} err={} gap={} turn={} cd={} fd={} ground={} slot={} swapGap={} hit={}", pl.getGameProfile().name(), target.getId(), String.format("%.2f", reach), String.format("%.3f", err), gap, String.format("%.0f", turn), String.format("%.2f", pl.getAttackStrengthScale(0.5f)), String.format("%.2f", pl.fallDistance), pl.onGround(), pl.getInventory().getSelectedSlot(), now - s.swapMs, hit);
		// Diagnostic only. Server handling times are not packet arrival times: do not use
		// these samples to convict on cadence alone, especially under lag or low TPS.
		if (Trace.ON) {
			long ns = System.nanoTime();
			int tick = pl.level().getServer().getTickCount();
			double gapMs = s.traceNs == 0 ? -1 : (ns - s.traceNs) / 1_000_000.0;
			double eyeStep = s.traceEye == null ? 0 : eye.distanceTo(s.traceEye);
			double centreStep = s.traceCentre == null || s.lastEntity != target.getId() ? -1 : centre.distanceTo(s.traceCentre);
			if (s.logs.allow(CombatLogBudget.Kind.TRACE, ns)) Meshac.LOG.info("[a15] player={} target={} wallMs={} tick={} tickGap={} gapMs={} reach={} rewindReach={} aimErr={} cd={} yaw={} pitch={} yawStep={} pitchStep={} eyeStep={} centreStep={} targetVx={} targetVy={} targetVz={} pingMs={} slot={} ground={} verdict={}",
				pl.getGameProfile().name(), target.getId(), now, tick, s.traceNs == 0 ? -1 : tick - s.traceTick,
				gapMs, reach, best, err, pl.getAttackStrengthScale(0.5f), pl.getYRot(), pl.getXRot(),
				s.traceNs == 0 ? 0 : net.minecraft.util.Mth.wrapDegrees(pl.getYRot() - s.traceYaw),
				s.traceNs == 0 ? 0 : pl.getXRot() - s.tracePitch, eyeStep, centreStep,
				mv.x, mv.y, mv.z, pl.connection.latency(), pl.getInventory().getSelectedSlot(), pl.onGround(), hit);
			s.traceNs = ns; s.traceTick = tick; s.traceYaw = pl.getYRot(); s.tracePitch = pl.getXRot(); s.traceEye = eye; s.traceCentre = centre;
		}
		s.lastMs = now; s.lastEntity = target.getId(); s.lastAtkY = pl.getY();
		return hit;
	}

	public static void forget(UUID id) { STATE.remove(id); TRACK.remove(id); MULTI.remove(id); RECORDER.clear(id); }
}
