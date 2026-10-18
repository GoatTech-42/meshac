package dev.meshac;

/**
 * Where a legitimate move can have been. Vanilla Entity.collide sweeps the movement one axis at a time: Y first, then the larger horizontal axis, then the other
 * (Direction.axisStepOrder). A move the client really made therefore follows that axis-aligned route, not the straight line between the two reported positions.
 * Launches (mace smash, wind charge, knockback, elytra boost) move more than 1.5 blocks in a tick, and the straight line clips corners the real route clears.
 */
public final class MovePath {
	private MovePath() {}
	public interface Solid { boolean at(double dx, double dy, double dz); } // does the body, shifted by this offset from the start, overlap a solid block

	/** Offsets (from the start) to test along the vanilla route, every `step` blocks, leg ends included, the final end excluded (the end is tested by the server's own collision). */
	public static double[][] samples(double dx, double dy, double dz, double step) {
		boolean zFirst = Math.abs(dx) < Math.abs(dz);
		double[][] legs = zFirst ? new double[][] {{0, dy, 0}, {0, 0, dz}, {dx, 0, 0}} : new double[][] {{0, dy, 0}, {dx, 0, 0}, {0, 0, dz}};
		java.util.ArrayList<double[]> out = new java.util.ArrayList<>();
		double cx = 0, cy = 0, cz = 0;
		for (double[] l : legs) {
			double len = Math.abs(l[0]) + Math.abs(l[1]) + Math.abs(l[2]);
			if (len < 1e-9) continue;
			int n = (int) Math.ceil(len / step);
			for (int i = 1; i <= n; i++) out.add(new double[] {cx + l[0] * i / n, cy + l[1] * i / n, cz + l[2] * i / n});
			cx += l[0]; cy += l[1]; cz += l[2];
		}
		if (!out.isEmpty()) out.remove(out.size() - 1);
		return out.toArray(new double[0][]);
	}

	/** True when any sample of the vanilla route overlaps a solid. */
	public static boolean blocked(Solid s, double dx, double dy, double dz, double step) {
		for (double[] p : samples(dx, dy, dz, step)) if (s.at(p[0], p[1], p[2])) return true;
		return false;
	}

	/** The old straight-line test, kept for comparison in tests. */
	public static boolean chordBlocked(Solid s, double dx, double dy, double dz, double step) {
		int n = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / step);
		for (int i = 1; i < n; i++) if (s.at(dx * i / n, dy * i / n, dz * i / n)) return true;
		return false;
	}
}
