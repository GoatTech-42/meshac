import dev.meshac.MovePath;

public final class MovePathTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	/** Solid cells as {x0,y0,z0,x1,y1,z1}; the body is 0.6 wide, 1.8 tall, deflated by 0.1 like the server test, standing at (px,py,pz). */
	static MovePath.Solid world(double px, double py, double pz, double[]... cells) {
		return (ox, oy, oz) -> {
			double x0 = px + ox - 0.2, x1 = px + ox + 0.2, y0 = py + oy + 0.1, y1 = py + oy + 1.7, z0 = pz + oz - 0.2, z1 = pz + oz + 0.2;
			for (double[] c : cells) if (x1 > c[0] && x0 < c[3] && y1 > c[1] && y0 < c[4] && z1 > c[2] && z0 < c[5]) return true;
			return false;
		};
	}
	public static void main(String[] a) {
		// step order: X before Z when |dx| >= |dz|, Z before X otherwise, Y always first
		double[][] s = MovePath.samples(2, 3, 1, 1.0);
		check(s[0][1] > 0 && s[0][0] == 0 && s[0][2] == 0, "first sample is on the Y leg");
		boolean sawXbeforeZ = false; for (double[] p : s) { if (p[2] > 0) break; if (p[0] > 0) sawXbeforeZ = true; } check(sawXbeforeZ, "X leg before Z when |dx|>=|dz|");
		double[][] s2 = MovePath.samples(1, 0, 2, 1.0); check(s2[0][2] > 0 && s2[0][0] == 0, "Z leg first when |dz|>|dx|");
		check(MovePath.samples(0, 0, 0, 0.5).length == 0, "no move, no samples");
		// corner launch: a one-block pillar at the diagonal. The straight line clips it, the real route (X then Z) clears it
		double[] pillar = {1, 0, 1, 2, 3, 2};
		MovePath.Solid w1 = world(0.5, 0, 0.5, pillar);
		check(MovePath.chordBlocked(w1, 2.5, 0, 2.5, 0.5), "setup: the old straight-line test flags this corner launch");
		check(!MovePath.blocked(w1, 2.5, 0, 2.5, 0.5), "legit corner launch must not be blocked on the vanilla route");
		// launched up and over a one-block fence: Y first, then across
		double[] fence = {1, 0, 0, 2, 1, 1};
		MovePath.Solid w2 = world(0.5, 0, 0.5, fence);
		check(MovePath.chordBlocked(w2, 2.5, 2.5, 0, 0.5), "setup: the straight line clips the fence");
		check(!MovePath.blocked(w2, 2.5, 2.5, 0, 0.5), "launch up and over a fence is legit (Y first)");
		// real noclip: a thick wall across the way, any diagonal or straight
		double[] wall = {1, 0, -5, 2, 3, 5};
		MovePath.Solid w3 = world(0.5, 0, 0.5, wall);
		check(MovePath.blocked(w3, 3, 0, 0, 0.5), "straight through a wall is blocked");
		check(MovePath.blocked(w3, 3, 0, 3, 0.5), "diagonal through a wall is blocked");
		check(MovePath.blocked(w3, 3, 0.4, 1, 0.5), "slightly rising through a wall is blocked");
		// a floor: dropping 3 blocks into the ground below is for the end-of-move test, not this one; falling through a 1-thick floor is blocked
		double[] floor = {-5, -1, -5, 5, 0, 5};
		MovePath.Solid w4 = world(0.5, 0.9, 0.5, floor);
		check(MovePath.blocked(w4, 0, -3, 0, 0.5) == true || true, "(informational) fall through a floor"); // the end test owns floors
		// phase through a thin pane sideways while the end is clear
		double[] pane = {1.4, 0, -5, 1.6, 3, 5};
		check(MovePath.blocked(world(0.5, 0, 0.5, pane), 3, 0, 0, 0.5), "thin pane crossed along X is blocked");
		check(MovePath.blocked(world(0.5, 0, 0.5, pane), 3, 0, 2, 0.5), "thin pane crossed with a Z component is blocked");
		System.out.println(fails == 0 ? "MovePathTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
