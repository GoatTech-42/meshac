package dev.meshac;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

/** Reads block restitution under a claimed landing: max over the footprint cells at the vanilla getOnPosLegacy row. Version-keyed through BounceRestitution. */
public final class BounceBlocks {
	private BounceBlocks() {}
	public static double restitution(BlockGetter level, double x, double y, double z) {
		int by = BounceTracker.probeBlockY(y);
		int[] f = BounceTracker.footprint(x, z);
		double best = 0;
		for (int cx = f[0]; cx <= f[1]; cx++) for (int cz = f[2]; cz <= f[3]; cz++) best = Math.max(best, BounceRestitution.of(level.getBlockState(new BlockPos(cx, by, cz)).getBlock()));
		return best;
	}
}
