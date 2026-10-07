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
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final class S { int fakeCrit; long fakeAt; long[] rt = new long[12]; double[] re = new double[12]; int[] ri = new int[12]; int rn; long swapMs, swapWinMs, hitAt, cycleStart; boolean swapDiff; int swapHits, cycles; long g1, g2; int prevSlot = -1; long lastMs, lastId; int lastEntity = -1, weak, fast, perfect, snap, n; float lastYaw; final long[] gaps = new long[6]; final double[] errs = new double[6]; final int[] ids = new int[6]; int en; long windowMs; int hits; double lastErr = -1; }

	/** Called when the player picks another hotbar slot. */
	public static void swapped(ServerPlayer pl, int slot) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		// Swap, hit, swap back. A hand can do it once; a module repeats it with the same gaps every time.
		if (s.hitAt > s.swapMs && now - s.hitAt < 250 && now - s.swapMs < 400) {
			long a = s.hitAt - s.swapMs, b = now - s.hitAt;
			if (now - s.cycleStart > 20000) { s.cycles = 0; s.cycleStart = now; }
			boolean same = s.cycles > 0 && Math.abs(a - s.g1) <= 12 && Math.abs(b - s.g2) <= 12;
			boolean oneTick = a < 30;
			if (same || oneTick) s.cycles++; else s.cycles = Math.max(0, s.cycles - 1);
			s.g1 = a; s.g2 = b;
			Trace.t(pl, "swap cycle to-hit=" + a + " back=" + b + " cycles=" + s.cycles);
			if (s.cycles >= 3) { s.cycles = 0; Verdict.signal(pl, "combat", "swap, hit and swap back on a timer (" + a + " / " + b + " ms)", 1); }
		}
		s.swapMs = now;
		s.swapDiff = pl.getInventory().getItem(slot).getItem() != pl.getMainHandItem().getItem();
	}

	/** Returns a reason when this attack is not something a person at a keyboard could have sent, else null. */
	public static String check(ServerPlayer pl, Entity target) {
		if (pl.isCreative() || pl.isSpectator()) return null;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		Vec3 eye = pl.getEyePosition();
		AABB box = target.getBoundingBox();
		double reach = Math.sqrt(box.distanceToSqr(eye));
		String hit = null;
		// Reach: vanilla entity interaction range is 3.0. Half a block of slack for latency and the 2-tick rewind.
		if (reach > 3.6) hit = String.format("reach %.2f", reach);
		// Line of sight: a wall between the eyes and the target.
		if (hit == null && reach > 0.5 && !pl.hasLineOfSight(target)) hit = "hit through a block";
		// Aim: the look ray has to pass through the hitbox (grown a little for the client's rounding).
		Vec3 look = pl.getLookAngle();
		// The client aimed at where the target was a few ticks ago, so a quick mob (spider, a player sprinting) is tested over the ground it just covered.
		Vec3 mv = target.getDeltaMovement();
		double bx = Math.max(-2, Math.min(2, mv.x * -4)), bz = Math.max(-2, Math.min(2, mv.z * -4)); // sideways only: gravity is not a reason to look lower
		AABB aimed = new AABB(box.minX + Math.min(0, bx), box.minY, box.minZ + Math.min(0, bz), box.maxX + Math.max(0, bx), box.maxY, box.maxZ + Math.max(0, bz)).inflate(0.35);
		var ray = aimed.clip(eye, eye.add(look.scale(6)));
		if (hit == null && ray.isEmpty() && reach > 0.15) hit = "not looking at target"; // inside the hitbox the ray has no entry point; anywhere else a miss of the box is a miss
		// Robotic aim: the look ray hits the exact same spot of the box hit after hit. People wobble by a few centimetres.
		Vec3 centre = box.getCenter();
		// Closest approach of the look ray to the box centre. Distance from the entry point is nearly constant for any central hit, so it hides wobble.
		double err = ray.isPresent() ? centre.subtract(eye.add(look.scale(Math.max(0, centre.subtract(eye).dot(look))))).length() : 9.0;
		if (s.lastErr >= 0 && Math.abs(err - s.lastErr) < 0.004 && err < 1) s.perfect++; else s.perfect = Math.max(0, s.perfect - 1);
		s.lastErr = err;
		if (hit == null && s.perfect >= 5) { hit = "aim is too steady"; s.perfect = 0; }
		// Same aim error on different targets: the look ray lands at the same spot of every box. People are never that even across targets.
		if (s.lastMs != 0 && now - s.lastMs > 3000) s.en = 0; // an old fight does not count towards this one
		s.errs[s.en % 6] = err; s.ids[s.en % 6] = target.getId(); s.en++;
		if (hit == null && s.en >= 6 && err < 1) {
			double[] sorted = s.errs.clone(); java.util.Arrays.sort(sorted); double med = (sorted[2] + sorted[3]) / 2;
			int close = 0; java.util.Set<Integer> who = new java.util.HashSet<>();
			for (int i = 0; i < 6; i++) if (Math.abs(s.errs[i] - med) < 0.004) { close++; who.add(s.ids[i]); }
			if (close >= 5 && who.size() >= 2) hit = "same aim on different targets";
			// Dead on the centre of the box: a hand never lands within 3 mm of the exact middle three times in six hits.
			int exact = 0; for (int i = 0; i < 6; i++) if (s.errs[i] < 0.003) exact++;
			if (hit == null && exact >= 3) hit = "aim is exactly on the centre";
			// Dead-centre aim: nearly every recent hit lands within 15 cm of the box centre, across more than one target. A person drifts.
			int centred = 0; java.util.Set<Integer> cwho = new java.util.HashSet<>();
			for (int i = 0; i < 6; i++) if (s.errs[i] < 0.15) { centred++; cwho.add(s.ids[i]); }
			if (hit == null && centred >= 4 && cwho.size() >= 2) hit = "aim locked to the centre";
		}
		// Attack rate: more than about 12 swings a second, or many hits at low cooldown charge.
		if (now - s.lastMs < 70) s.fast++; else s.fast = Math.max(0, s.fast - 1);
		if (hit == null && s.fast >= 4) { hit = "attack rate"; s.fast = 0; }
		if (pl.getAttackStrengthScale(0.5f) < 0.5f) s.weak++; else s.weak = Math.max(0, s.weak - 1);
		if (hit == null && s.weak >= 6) { hit = "hitting before the cooldown"; s.weak = 0; }
		s.hitAt = now;
		// Criticals: the client says "in the air" for a few centimetres so the server counts a fall and the hit crits. A real crit needs a real fall.
		double fd = pl.fallDistance;
		if (!pl.onGround() && fd > 0 && fd < 0.3 && !pl.onClimbable() && !pl.isInWater() && !pl.isPassenger() && !pl.getAbilities().mayfly) {
			if (now - s.fakeAt > 10000) s.fakeCrit = 0;
			s.fakeAt = now;
			if (++s.fakeCrit >= 3 && hit == null) { hit = String.format("critical hits from a %.2f block fall", fd); s.fakeCrit = 0; }
		}
		// KillAura: many different targets in a few seconds with the aim ray nowhere near their centres. A hand fights one or two and aims at them.
		int k = s.rn++ % 12; s.rt[k] = now; s.re[k] = err; s.ri[k] = target.getId();
		if (hit == null) {
			java.util.Set<Integer> who = new java.util.HashSet<>(); java.util.List<Double> es = new java.util.ArrayList<>();
			for (int i = 0; i < Math.min(s.rn, 12); i++) if (now - s.rt[i] < 5000) { who.add(s.ri[i]); es.add(s.re[i]); }
			if (es.size() >= 8 && who.size() >= 4) { java.util.Collections.sort(es); if (es.get(es.size() / 2) > 0.9) { hit = "hits on " + who.size() + " targets without aiming at them"; s.rn = 0; } }
		}
		// Multi-target: two different entities hit inside the same 50 ms.
		if (hit == null && s.lastEntity != -1 && s.lastEntity != target.getId() && now - s.lastMs < 50) hit = "two targets in one tick";
		// Snapping: the view swings more than 40 degrees to a different target inside 300 ms. A person cannot do that twice in a row.
		float yaw = pl.getYRot(); float turn = Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - s.lastYaw));
		if (s.lastEntity != -1 && s.lastEntity != target.getId() && now - s.lastMs < 300 && turn > 40) s.snap++; else if (now - s.lastMs > 600) s.snap = 0;
		if (hit == null && s.snap >= 2) { hit = "snapping between targets"; s.snap = 0; }
		s.lastYaw = yaw;
		// Metronome: six attacks in a row with almost the same gap between them. Hands do not keep time to 15 ms.
		long gap = s.lastMs == 0 ? 0 : now - s.lastMs;
		if (gap > 250 && gap < 1500) { s.gaps[s.n++ % 6] = gap; } else if (gap >= 1500) s.n = 0;
		if (hit == null && s.n >= 6) {
			double mean = 0; for (long g : s.gaps) mean += g; mean /= 6;
			double var = 0; for (long g : s.gaps) var += (g - mean) * (g - mean);
			if (Math.sqrt(var / 6) < 15) { hit = "attack timing is too regular"; s.n = 0; }
		}
		if (System.getenv("MESHAC_TRACE") != null) Meshac.LOG.info("[trace] attack {} t={} reach={} err={} gap={} turn={} cd={} fd={} ground={} slot={} swapGap={} hit={}", pl.getGameProfile().name(), target.getId(), String.format("%.2f", reach), String.format("%.3f", err), gap, String.format("%.0f", turn), String.format("%.2f", pl.getAttackStrengthScale(0.5f)), String.format("%.2f", pl.fallDistance), pl.onGround(), pl.getInventory().getSelectedSlot(), now - s.swapMs, hit);
		s.lastMs = now; s.lastEntity = target.getId();
		return hit;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
