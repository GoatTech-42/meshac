import dev.meshac.veil.*;
import java.util.*;

public class NeighbourGridTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	static final int H = 64;
	static int[] world;
	static int idx(int x, int y, int z) { return (y * 64 + z) * 64 + x; }
	static int calls; static int cls0(int x, int y, int z) { return cls(x, y, z); }
	static int cls(int x, int y, int z) { calls++; if (x < 0 || z < 0 || x >= 64 || z >= 64) return NeighbourGrid.TRANSPARENT; /* unloaded: transparent */ if (y < 0 || y >= H) return 0; return world[idx(x, y, z)]; }

	public static void main(String[] a) {
		Random r = new Random(11);
		world = new int[64 * H * 64];
		for (int i = 0; i < world.length; i++) { int v = r.nextInt(100); world[i] = v < 3 ? NeighbourGrid.HIDDEN : v < 8 ? NeighbourGrid.TRANSPARENT : NeighbourGrid.STONE; }
		// the same decisions as unmemoised lookups, for every block of several sections incl. ones touching the edge of the loaded area
		XrayCore.Blocks direct = new XrayCore.Blocks() {
			public boolean hidden(int x, int y, int z) { return (cls(x, y, z) & NeighbourGrid.HIDDEN) != 0; }
			public boolean transparent(int x, int y, int z) { return (cls(x, y, z) & NeighbourGrid.TRANSPARENT) != 0; }
		};
		NeighbourGrid g = new NeighbourGrid();
		long directCalls = 0, gridCalls = 0; int hid = 0, fake = 0;
		for (int[] sec : new int[][]{{16, 16, 16}, {0, 0, 0}, {48, 32, 48}, {16, 48, 0}}) {
			g.reset(sec[0], sec[1], sec[2], NeighbourGridTest::cls0);
			calls = 0; boolean[] wantH = new boolean[4096], wantF = new boolean[4096];
			for (int ly = 0; ly < 16; ly++) for (int lz = 0; lz < 16; lz++) for (int lx = 0; lx < 16; lx++) {
				int x = sec[0] + lx, y = sec[1] + ly, z = sec[2] + lz, i = lx | ly << 4 | lz << 8;
				int c = world[idx(x, y, z)];
				if ((c & NeighbourGrid.HIDDEN) != 0) wantH[i] = XrayCore.shouldHide(direct, x, y, z);
				else if ((c & NeighbourGrid.STONE) != 0 && XrayCore.fakeOreAt(5, x, y, z, 0.15)) wantF[i] = !XrayCore.exposed(direct, x, y, z);
			}
			directCalls += calls; calls = 0;
			for (int ly = 0; ly < 16; ly++) for (int lz = 0; lz < 16; lz++) for (int lx = 0; lx < 16; lx++) {
				int x = sec[0] + lx, y = sec[1] + ly, z = sec[2] + lz, i = lx | ly << 4 | lz << 8;
				int c = world[idx(x, y, z)]; boolean h = false, f = false; // own block read directly, like sec.getBlockState in the adapter
				if ((c & NeighbourGrid.HIDDEN) != 0) h = XrayCore.shouldHide(g, x, y, z);
				else if ((c & NeighbourGrid.STONE) != 0 && XrayCore.fakeOreAt(5, x, y, z, 0.15)) f = !XrayCore.exposed(g, x, y, z);
				check(h == wantH[i] && f == wantF[i], "same decision at " + x + "," + y + "," + z);
				if (h) hid++; if (f) fake++;
			}
			gridCalls += g.lookups;
		}
		check(hid > 0 && fake > 0, "the test world produced decisions: " + hid + " hidden " + fake + " fake");
		System.out.println("classifier calls: direct " + directCalls + ", memoised " + gridCalls + " neighbour lookups"); check(gridCalls < directCalls, "memo does not add lookups: " + gridCalls + " vs " + directCalls);
		// outside the box: delegated, not cached, never wrong
		g.reset(16, 16, 16, NeighbourGridTest::cls0); int before = g.lookups;
		check(g.classify(40, 40, 40) == cls(40, 40, 40) && g.classify(40, 40, 40) == cls(40, 40, 40), "far position answered");
		check(g.lookups == before + 2, "outside the box is never cached");
		// inside the box: second ask is free
		g.classify(20, 20, 20); int l1 = g.lookups; g.classify(20, 20, 20); check(g.lookups == l1, "inside the box is cached");
		// corners of the 18-box are in, one more is out
		g.reset(16, 16, 16, NeighbourGridTest::cls0); g.classify(15, 15, 15); g.classify(32, 32, 32); int l2 = g.lookups; g.classify(15, 15, 15); g.classify(32, 32, 32); check(g.lookups == l2, "border corners cached");
		g.classify(33, 32, 32); g.classify(33, 32, 32); check(g.lookups == l2 + 2, "one past the border is not cached");
		// reset forgets everything (a stale memo would serve the last section's blocks)
		world[idx(20, 20, 20)] = NeighbourGrid.HIDDEN; g.reset(16, 16, 16, NeighbourGridTest::cls0); check(g.hidden(20, 20, 20), "fresh after reset");
		world[idx(20, 20, 20)] = NeighbourGrid.STONE; check(g.hidden(20, 20, 20), "memo holds within one plan");
		g.reset(16, 16, 16, NeighbourGridTest::cls0); check(!g.hidden(20, 20, 20), "reset drops the memo");
		// a classifier value of 0 (air above the world, for instance) is memoised as known, not looked up forever
		g.reset(16, 16, 16, NeighbourGridTest::cls0); g.classify(20, -1, 20); g.classify(20, -1, 20); // y -1 is outside the box anyway: use H boundary instead
		world[idx(20, 20, 20)] = 0; g.reset(16, 16, 16, NeighbourGridTest::cls0); g.classify(20, 20, 20); int l3 = g.lookups; g.classify(20, 20, 20); check(g.lookups == l3, "a zero classification is cached");
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("NeighbourGridTest ok");
	}
}
