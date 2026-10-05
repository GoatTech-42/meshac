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
	private static final class S { long lastMs, lastId; int lastEntity = -1, weak, fast, perfect, snap; float lastYaw; long windowMs; int hits; double lastErr = -1; }

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
		var ray = box.inflate(0.35).clip(eye, eye.add(look.scale(6)));
		if (hit == null && ray.isEmpty() && reach > 0.8) hit = "not looking at target";
		// Robotic aim: the look ray hits the exact same spot of the box hit after hit. People wobble by a few centimetres.
		Vec3 centre = box.getCenter();
		double err = ray.map(v -> v.distanceTo(centre)).orElse(9.0);
		if (s.lastErr >= 0 && Math.abs(err - s.lastErr) < 0.004 && err < 1) s.perfect++; else s.perfect = Math.max(0, s.perfect - 1);
		s.lastErr = err;
		if (hit == null && s.perfect >= 5) { hit = "aim is too steady"; s.perfect = 0; }
		// Attack rate: more than about 12 swings a second, or many hits at low cooldown charge.
		if (now - s.lastMs < 70) s.fast++; else s.fast = Math.max(0, s.fast - 1);
		if (hit == null && s.fast >= 4) { hit = "attack rate"; s.fast = 0; }
		if (pl.getAttackStrengthScale(0.5f) < 0.5f) s.weak++; else s.weak = Math.max(0, s.weak - 1);
		if (hit == null && s.weak >= 6) { hit = "hitting before the cooldown"; s.weak = 0; }
		// Multi-target: two different entities hit inside the same 50 ms.
		if (hit == null && s.lastEntity != -1 && s.lastEntity != target.getId() && now - s.lastMs < 50) hit = "two targets in one tick";
		// Snapping: the view swings more than 40 degrees to a different target inside 300 ms. A person cannot do that twice in a row.
		float yaw = pl.getYRot(); float turn = Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - s.lastYaw));
		if (s.lastEntity != -1 && s.lastEntity != target.getId() && now - s.lastMs < 300 && turn > 40) s.snap++; else if (now - s.lastMs > 600) s.snap = 0;
		if (hit == null && s.snap >= 2) { hit = "snapping between targets"; s.snap = 0; }
		s.lastYaw = yaw;
		s.lastMs = now; s.lastEntity = target.getId();
		return hit;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
