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
	private static final class S { long windowAt; int breaks; }
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
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.windowAt > 1000) { s.windowAt = now; s.breaks = 0; }
		if (++s.breaks > MAX_PER_SECOND) {
			Verdict.signal(pl, "mining", "more than " + MAX_PER_SECOND + " blocks in a second", 1);
			return true;
		}
		return false;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
