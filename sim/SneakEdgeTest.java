/**
 * Sneak-edge vs the ground-spoof 0.1 strip: the flag condition is horizontal (zero XZ overlap under the box), not vertical.
 * Feet rest exactly on the block top, so the [minY-0.1, minY] strip always reaches the support; widening the strip cannot
 * change a sneak-edge outcome either way. World model: one full block, top y=64, footprint [0,1]x[0,1]; player box
 * half-width 0.3, feet at 64. The support probe mirrors canFallAtLeast: inset box probed down to minY - maxUpStep (0.6),
 * which intersects the block ([63,64]) iff the XZ footprints overlap.
 */
public final class SneakEdgeTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	static final double TOP = 64.0, MAX_STEP_DOWN = 0.6;

	static boolean supported(double px, double pz, double inset, double dx, double dz) {
		return SneakEdge.overlap(px + dx, pz + dz, inset, 0, 1, 0, 1) > 0;
	}

	/** The meshac strip condition: air under the box. Feet at TOP, block top at TOP -> gap 0; only zero XZ overlap yields air. */
	static boolean stripAir(double px, double pz, double strip) {
		// vertical: [TOP-strip, TOP] always covers the block top (gap 0 <= any strip), so the strip reduces to XZ overlap
		return SneakEdge.overlap(px, pz, 0, 0, 1, 0, 1) == 0;
	}

	public static void main(String[] a) {
		java.util.Random r = new java.util.Random(11);
		int clamped = 0;
		for (int i = 0; i < 100_000; i++) {
			double px = 0.85 + r.nextDouble() * 0.55, pz = 0.1 + r.nextDouble() * 0.8; // start supported, biased to the +x edge so the clamp bites often
			if (!supported(px, pz, SneakEdge.INSET, 0, 0)) { i--; continue; }
			double dx = 0.005 + r.nextDouble() * 0.12, dz = (r.nextDouble() - 0.5) * 0.12; // try to walk over the edge
			SneakEdge.Support s = (ox, oz) -> !supported(px, pz, SneakEdge.INSET, ox, oz);
			boolean wouldFall = s.canFall(dx, 0) || s.canFall(0, dz) || s.canFall(dx, dz);
			double[] out = SneakEdge.backOff(dx, dz, s);
			double nx = px + out[0], nz = pz + out[1];
			// 1. the clamp never accepts an unsupported position (its own inset probe still finds support)
			check(supported(nx, nz, SneakEdge.INSET, 0, 0), "clamp accepted an unsupported position from " + px + "," + pz + " req " + dx + "," + dz);
			// 2. and the real box keeps overlap: the meshac strip (any vertical width) finds support
			check(!stripAir(nx, nz, 0.1), "0.1 strip air on a clamped sneak position " + nx + "," + nz);
			check(!stripAir(nx, nz, 0.13), "0.13 strip air on a clamped sneak position " + nx + "," + nz);
			if (wouldFall) {
				clamped++;
				double ov = SneakEdge.overlap(nx, nz, 0, 0, 1, 0, 1);
				check(ov > 0 && ov <= SneakEdge.STEP + 2 * SneakEdge.INSET, "clamped overlap outside (0, 0.05]: " + ov);
			}
		}
		check(clamped > 10_000, "fuzz barely exercised the clamp: " + clamped);
		// 3. a zero-overlap position is air under BOTH strip widths: vertical widening cannot fix a horizontal loss
		check(stripAir(1.31, 0.5, 0.1) && stripAir(1.31, 0.5, 0.13), "constructed zero-overlap position should read air");
		check(!stripAir(1.299, 0.5, 0.1), "0.001 overlap should still read supported (gap 0)");
		// 4. corners: diagonal approach to the (+,+) corner, clamp keeps support
		for (int i = 0; i < 50_000; i++) {
			double px = 0.6 + r.nextDouble() * 0.35, pz = 0.6 + r.nextDouble() * 0.35;
			if (!supported(px, pz, SneakEdge.INSET, 0, 0)) { i--; continue; }
			double dx = 0.01 + r.nextDouble() * 0.1, dz = 0.01 + r.nextDouble() * 0.1;
			SneakEdge.Support s = (ox, oz) -> !supported(px, pz, SneakEdge.INSET, ox, oz);
			double[] out = SneakEdge.backOff(dx, dz, s);
			check(supported(px + out[0], pz + out[1], SneakEdge.INSET, 0, 0), "corner clamp lost support from " + px + "," + pz);
		}
		// 5. float32 relay (Geyser): rounding shifts the box by at most ulp/2, so an overlap above that survives.
		//    Boar's Bedrock clamp probes with a 0.025 inset, so its margin is 0.025; ulp32(x)/2 reaches 0.025 only near |x| = 2^19.
		check(SneakEdge.ulp32(524_288) / 2 >= 0.025 && SneakEdge.ulp32(262_144) / 2 < 0.025, "float32 half-ulp reaches the Boar margin near 2^19, got " + SneakEdge.ulp32(524_288) / 2);
		for (int i = 0; i < 20_000; i++) {
			double px = 0.69 + r.nextDouble() * 0.005; // clamped-edge positions, tiny overlap
			double ov = SneakEdge.overlap(px, 0.5, 0, 0, 1, 0, 1);
			if (ov <= 0) { i--; continue; }
			float fx = (float) px;
			double ov2 = SneakEdge.overlap(fx, 0.5f, 0, 0, 1, 0, 1);
			if (ov > SneakEdge.ulp32(px) / 2 + 1e-9) check(ov2 > 0, "float32 relay destroyed a " + ov + " overlap at " + px);
		}
		// 6. the two clamp loops match their sources structurally: accepted set unchanged when the request is already safe
		SneakEdge.Support safe = (ox, oz) -> false;
		double[] out = SneakEdge.backOff(0.07, -0.04, safe);
		check(out[0] == 0.07 && out[1] == -0.04, "safe request was clamped");
		System.out.println(fails == 0 ? "SneakEdgeTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
