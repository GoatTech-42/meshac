/** Which geometries the noclip checks catch, catch late, or cannot see. World: thin and thick walls at body height. */
public final class PathCollisionTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	static java.util.List<PathCollision.Box> wall(double x0, double x1, double z0, double z1) {
		return java.util.List.of(new PathCollision.Box(x0, 63, z0, x1, 66, z1));
	}

	public static void main(String[] a) {
		// 1-thick solid wall at x in [3,4]: a 2.2-block move through it is caught
		check(PathCollision.blocked(wall(3, 4, -8, 8), 1.9, 64, 0, 2.2, 0, 0), "solid wall crossing missed");
		// a 2.2-block move ending just before the wall is clean
		check(!PathCollision.blocked(wall(3, 4, -8, 8), 0.5, 64, 0, 2.2, 0, 0), "move ending before the wall flagged");
		// fence (0.5 thick) and glass pane (0.1875 thick) both catch: the deflated body is 0.4 wide and straddles them
		check(PathCollision.blocked(wall(2.75, 3.25, -8, 8), 1.9, 64, 0, 2.2, 0, 0), "fence crossing missed");
		check(PathCollision.blocked(wall(2.90625, 3.09375, -8, 8), 1.9, 64, 0, 2.2, 0, 0), "pane crossing missed");
		// an open doorway (1 wide): center passes, hugging the frame clips it
		java.util.List<PathCollision.Box> door = new java.util.ArrayList<>(wall(3, 4, -8, -0.5)); door.addAll(wall(3, 4, 0.5, 8));
		check(!PathCollision.blocked(door, 1.9, 64, 0, 2.2, 0, 0), "doorway center flagged");
		check(PathCollision.blocked(door, 1.9, 64, 0.35, 2.2, 0, 0), "doorway edge clip missed");
		// the check only runs past 1.5 blocks (d2 > 2.25): 1.4 blocks through the same wall is never looked at
		// (that is Movement's d2 gate, not this geometry; noted for the report)
		// diagonal corner clip: through the corner cell of an L
		java.util.List<PathCollision.Box> corner = new java.util.ArrayList<>(wall(3, 4, 0, 8)); corner.addAll(wall(3, 8, -1, 0));
		check(PathCollision.blocked(corner, 2.0, 64, 2.0, 2.2, 0, -2.2), "corner clip missed");
		// piston push / pearl embed: half a block into a frame is invisible to the path check (too short),
		// but the in-block check sees it every packet it persists - 10 straight packets to flag
		check(!PathCollision.blocked(wall(3, 4, -8, 8), 2.6, 64, 0, 0.5, 0, 0), "0.5 push ran the path check");
		check(PathCollision.inBlock(wall(3, 4, -8, 8), 3.1, 64, 0), "embedded body not seen by the in-block check");
		check(!PathCollision.inBlock(wall(3, 4, -8, 8), 2.75, 64, 0), "clean adjacent body seen as embedded");
		// 10-block rule: refused whatever is in between (Movement's d2 > 100 branch); geometry here just confirms the path length
		check(PathCollision.blocked(java.util.List.of(), 0, 64, 0, 10.5, 0, 0) == false, "empty path flagged");
		System.out.println(fails == 0 ? "PathCollisionTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
