package dev.meshac;

/** Integration test for Movement.supportHit - the classify-mode support derivation. BOX-ONLY:
 * needs a live ServerLevel, so it is NOT in sim/run-tests.sh (that suite is pure). Compile with
 * the full Minecraft classpath on Box and call GroundSupportIT.run(server.overworld()) from the
 * rig bootstrap or a dev command. Covers the reviewer NO-GO blocks: snow layers, carpet, closed
 * gate/fence (collision 1.5 above the owning block), open gate (no collision at all). Battery
 * rows B2/B5/B6 watch the same blocks live. */
public final class GroundSupportIT {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } else System.out.println("ok " + m); }

	/** Strip under a 0.6-wide box centered on (bx+0.5, *, bz+0.5) with feet at feetY, candidate depth. */
	static net.minecraft.world.phys.AABB strip(int bx, int bz, double feetY) {
		double minX = bx + 0.2, minZ = bz + 0.2;
		return new net.minecraft.world.phys.AABB(minX, feetY - GroundTolerance.CANDIDATE, minZ, minX + 0.6, feetY, minZ + 0.6);
	}

	public static void run(net.minecraft.server.level.ServerLevel level) {
		net.minecraft.core.BlockPos snowP = new net.minecraft.core.BlockPos(0, 64, 0);
		net.minecraft.core.BlockPos carpetP = new net.minecraft.core.BlockPos(2, 64, 0);
		net.minecraft.core.BlockPos closedGateP = new net.minecraft.core.BlockPos(4, 64, 0);
		net.minecraft.core.BlockPos openGateP = new net.minecraft.core.BlockPos(6, 64, 0);
		net.minecraft.core.BlockPos fenceP = new net.minecraft.core.BlockPos(8, 64, 0);
		net.minecraft.core.BlockPos airP = new net.minecraft.core.BlockPos(10, 64, 0);
		level.setBlock(snowP, net.minecraft.world.level.block.Blocks.SNOW.defaultBlockState().setValue(net.minecraft.world.level.block.SnowLayerBlock.LAYERS, 2), 3);
		level.setBlock(carpetP, net.minecraft.world.level.block.Blocks.WHITE_CARPET.defaultBlockState(), 3);
		level.setBlock(closedGateP, net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, false), 3);
		level.setBlock(openGateP, net.minecraft.world.level.block.Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(net.minecraft.world.level.block.FenceGateBlock.OPEN, true), 3);
		level.setBlock(fenceP, net.minecraft.world.level.block.Blocks.OAK_FENCE.defaultBlockState(), 3);
		level.setBlock(airP, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);

		// snow, 2 layers: top 64.25, stand 0.13 above it
		Movement.SupportHit sup = Movement.supportHit(level, strip(0, 0, 64.25 + 0.13));
		check(sup != null && Math.abs(sup.top - 64.25) < 1e-6 && sup.id.equals("minecraft:snow"), "snow 2-layer support top 64.25 (was missed by the center sample flooring to an integer top)");
		check(sup != null && GroundTolerance.tolerated(true, true, sup.top, 64.25 + 0.13), "snow stand within gap is tolerated");

		// carpet: top 64.0625, stand 0.13 above
		sup = Movement.supportHit(level, strip(2, 0, 64.0625 + 0.13));
		check(sup != null && Math.abs(sup.top - 64.0625) < 1e-6 && sup.id.equals("minecraft:white_carpet"), "carpet support top 64.0625");
		check(sup != null && GroundTolerance.tolerated(true, true, sup.top, 64.0625 + 0.13), "carpet stand within gap is tolerated");

		// closed gate: collision top 1.5 above the owning block (65.5) - the case the point sample missed entirely
		sup = Movement.supportHit(level, strip(4, 0, 65.5 + 0.13));
		check(sup != null && Math.abs(sup.top - 65.5) < 1e-6 && sup.id.equals("minecraft:oak_fence_gate"), "closed gate support top 65.5 from the block BELOW the strip floor");
		check(sup != null && GroundTolerance.tolerated(true, true, sup.top, 65.5 + 0.13), "closed-gate stand within gap is tolerated");

		// open gate: empty collision shape - no support, the claim must stay a spoof
		sup = Movement.supportHit(level, strip(6, 0, 65.5 + 0.13));
		check(sup == null, "open gate has no collision: supportHit must find nothing (battery B5 open-gate edge)");

		// fence: same 1.5 family as the closed gate
		sup = Movement.supportHit(level, strip(8, 0, 65.5 + 0.13));
		check(sup != null && Math.abs(sup.top - 65.5) < 1e-6 && sup.id.equals("minecraft:oak_fence"), "fence support top 65.5 from the block below the strip floor");
		check(sup != null && GroundTolerance.tolerated(true, true, sup.top, 65.5 + 0.13), "fence stand within gap is tolerated");

		// open air: nothing, ever
		sup = Movement.supportHit(level, strip(10, 0, 65.5 + 0.13));
		check(sup == null, "open air: no support");

		System.out.println(fails == 0 ? "GroundSupportIT ok" : fails + " failures");
		if (fails > 0) throw new AssertionError("GroundSupportIT failures: " + fails);
	}
}
