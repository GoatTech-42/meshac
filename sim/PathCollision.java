/**
 * Geometric model of the noclip path check (Movement.blocked): a move over 1.5 blocks (d2 > 2.25) is refused when the body,
 * slid from the server position to the claim in half-block steps and deflated 0.1 (0.4 x 1.6 x 0.4), overlaps a solid cell
 * at any sample; over 10 blocks it is refused outright. Sim-only: src/ never calls this. The voxel world is a set of solid
 * [x,x+1]x[z,z+1] columns between y0 and y1 (walls, fences, panes, frames), thin boxes included.
 */
public final class PathCollision {
	private PathCollision() {}

	public static final class Box {
		public final double x0, y0, z0, x1, y1, z1;
		public Box(double x0, double y0, double z0, double x1, double y1, double z1) { this.x0 = x0; this.y0 = y0; this.z0 = z0; this.x1 = x1; this.y1 = y1; this.z1 = z1; }
		boolean hits(Box o) { return x1 > o.x0 && x0 < o.x1 && y1 > o.y0 && y0 < o.y1 && z1 > o.z0 && z0 < o.z1; }
	}

	/** The sliding body: 0.4 x 1.6 x 0.4 (the 0.6 x 1.8 player deflated 0.1 a side) standing with feet at y. */
	public static Box body(double cx, double feetY, double cz) { return new Box(cx - 0.2, feetY + 0.1, cz - 0.2, cx + 0.2, feetY + 1.7, cz + 0.2); }

	/** Movement.blocked, structure-verbatim: half-block samples, endpoint excluded (vanilla already tested where the packet ends). */
	public static boolean blocked(java.util.List<Box> solids, double sx, double sy, double sz, double dx, double dy, double dz) {
		int n = (int) Math.ceil(Math.sqrt(dx * dx + dy * dy + dz * dz) / 0.5);
		for (int i = 1; i < n; i++) {
			Box b = body(sx + dx * i / n, sy + dy * i / n, sz + dz * i / n);
			for (Box s : solids) if (b.hits(s)) return true;
		}
		return false;
	}

	/** The in-block check: the body deflated 0.1 overlaps a solid cell right now (needs 10 straight packets to flag). */
	public static boolean inBlock(java.util.List<Box> solids, double cx, double feetY, double cz) {
		Box b = body(cx, feetY, cz);
		for (Box s : solids) if (b.hits(s)) return true;
		return false;
	}
}
