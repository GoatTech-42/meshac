package dev.meshac;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Cobweb and honey recognised from the real body box (pose, scale) the way vanilla triggers them: any overlapped cell for a web, and the honey slide predicate for honey. */
public final class StuckBlocks {
	private StuckBlocks() {}
	public static boolean cobweb(BlockGetter level, AABB b) {
		int[] c = MediumFootprint.cells(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		for (int cx = c[0]; cx <= c[1]; cx++) for (int cy = c[2]; cy <= c[3]; cy++) for (int cz = c[4]; cz <= c[5]; cz++)
			if (level.getBlockState(new BlockPos(cx, cy, cz)).is(Blocks.COBWEB)) return true;
		return false;
	}
	public static boolean honeySlide(BlockGetter level, AABB b, double x, double y, double z, double width, boolean onGround) {
		int[] c = MediumFootprint.cells(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		for (int cx = c[0]; cx <= c[1]; cx++) for (int cy = c[2]; cy <= c[3]; cy++) for (int cz = c[4]; cz <= c[5]; cz++)
			if (level.getBlockState(new BlockPos(cx, cy, cz)).is(Blocks.HONEY_BLOCK) && HoneyRule.sliding(onGround, y, cx, cy, cz, x, z, width)) return true;
		return false;
	}
	/** Any overlapped cell is powder snow, by the same entityInside overlap as the web (real pose box, deflated 1e-5). Callers test the server box and the endpoint box. */
	public static boolean powderBody(BlockGetter level, AABB b) {
		int[] c = MediumFootprint.cells(b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
		for (int cx = c[0]; cx <= c[1]; cx++) for (int cy = c[2]; cy <= c[3]; cy++) for (int cz = c[4]; cz <= c[5]; cz++)
			if (level.getBlockState(new BlockPos(cx, cy, cz)).is(Blocks.POWDER_SNOW)) return true;
		return false;
	}
}
