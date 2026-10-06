package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Nuker, Kaboom, FastBreak and friends: breaking blocks no hand could reach, see or break that fast. */
public final class Mining {
	private static final class S { long windowAt; int breaks; long lastAt, turnWin; int turns; Vec3 lastDir; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MAX_PER_SECOND = 8; // a hasted, efficiency V player on soft blocks tops out near 5

	/** True when this break should be refused. */
	public static boolean refuse(ServerPlayer pl, BlockPos pos) {
		if (pl.isCreative() || pl.isSpectator()) return false;
		Vec3 eye = pl.getEyePosition();
		Vec3 centre = Vec3.atCenterOf(pos);
		BlockHitResult hit = pl.level().clip(new ClipContext(eye, centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pl));
		if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(pos)) {
			Verdict.signal(pl, "mining", "block broken through another block", 1);
			return true;
		}
		// A hand breaks what the crosshair is on. Nuker breaks blocks all around without turning.
		if (new net.minecraft.world.phys.AABB(pos).inflate(0.3).clip(eye, eye.add(pl.getLookAngle().scale(8))).isEmpty()) {
			Verdict.signal(pl, "mining", "broke a block it is not looking at", 1);
			return true;
		}
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		// Nuker turns the view to each block in turn. A tunnel miner keeps pointing the same way; fast successive breaks more than 40 degrees apart are not a hand.
		Vec3 dir = centre.subtract(eye).normalize();
		if (s.lastDir != null && now - s.lastAt < 600 && dir.dot(s.lastDir) < 0.766) {
			if (now - s.turnWin > 3000) { s.turnWin = now; s.turns = 0; }
			if (++s.turns >= 4) { s.turns = 0; Verdict.signal(pl, "mining", "breaks blocks in every direction", 1); return true; }
		}
		s.lastDir = dir; s.lastAt = now;
		if (now - s.windowAt > 1000) { s.windowAt = now; s.breaks = 0; }
		if (++s.breaks > MAX_PER_SECOND) {
			Verdict.signal(pl, "mining", "more than " + MAX_PER_SECOND + " blocks in a second", 1);
			return true;
		}
		return false;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
