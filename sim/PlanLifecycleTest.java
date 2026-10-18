import dev.meshac.veil.*;
import java.util.*;

/** A two-chunk toy world run through the same pure pieces the adapter uses: XrayCore for the plan, PlanGuard, NeighbourMask and BorderGuard for the validity check. */
public class PlanLifecycleTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	static final int AIR = 0, STONE = 1, ORE = 2;
	// chunk A is x 0..15, chunk B (east of it) is x 16..31, one 16 block tall section, z 0..15
	static int[][][] world = new int[32][16][16]; // [x][y][z]
	static boolean bLoaded;
	static int lookups;
	static boolean transparentAt(int x, int y, int z) { if (y < 0 || y >= 16) return false; if (x >= 16 && !bLoaded) return true; if (x < 0 || z < 0 || z > 15) return true; return world[x][y][z] == AIR; }
	static final XrayCore.Blocks view = new XrayCore.Blocks() {
		public boolean hidden(int x, int y, int z) { return x >= 0 && z >= 0 && z <= 15 && y >= 0 && y < 16 && world[x][y][z] == ORE; }
		public boolean transparent(int x, int y, int z) { return transparentAt(x, y, z); }
	};
	record Plan(int[] idx, int mask, int[] bIdx, int[] bBits) {}
	static int maskNow() { return NeighbourMask.of(false, bLoaded, false, false); }
	static Plan plan() {
		List<Integer> mods = new ArrayList<>();
		for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) for (int x = 0; x < 16; x++) if (view.hidden(x, y, z) && XrayCore.shouldHide(view, x, y, z)) mods.add(x | y << 4 | z << 8);
		int[] idx = mods.stream().mapToInt(i -> i).toArray();
		int[] b = Arrays.stream(idx).filter(BorderGuard::isBorder).toArray();
		int[] bits = BorderGuard.compute(b, (i, dx, dz) -> { lookups++; int q = b[i]; return transparentAt((q & 15) + dx, (q >> 4) & 15, ((q >> 8) & 15) + dz); });
		return new Plan(idx, maskNow(), b, bits);
	}
	static int check(Plan p) {
		return PlanGuard.check(() -> true, p.mask(), PlanLifecycleTest::maskNow, () -> BorderGuard.allUnchanged(p.bIdx(), p.bBits(), (i, dx, dz) -> { lookups++; int q = p.bIdx()[i]; return transparentAt((q & 15) + dx, (q >> 4) & 15, ((q >> 8) & 15) + dz); }));
	}
	static void fill() { for (int[][] a : world) for (int[] b : a) Arrays.fill(b, STONE); }

	public static void main(String[] args) {
		// ---- leak 2: planned while B was unloaded ----
		fill(); world[15][5][5] = ORE; world[7][8][8] = ORE; bLoaded = false;
		Plan p = plan();
		check(Arrays.stream(p.idx()).anyMatch(i -> i == (7 | 8 << 4 | 8 << 8)) && Arrays.stream(p.idx()).noneMatch(i -> i == (15 | 5 << 4 | 5 << 8)), "interior ore hidden, border ore shown while B is unloaded");
		check(check(p) == PlanGuard.OK, "nothing changed: plan is valid");
		bLoaded = true;
		check(check(p) == PlanGuard.NEIGHBOUR_LOADED, "B loaded: the plan is rebuilt");
		p = plan();
		check(Arrays.stream(p.idx()).anyMatch(i -> i == (15 | 5 << 4 | 5 << 8)), "after the rebuild the border ore is hidden");
		check(p.mask() == NeighbourMask.EAST && check(p) == PlanGuard.OK, "new plan is valid and stores the mask");
		// ---- leak 1: no-update change across the border ----
		world[16][5][5] = AIR; // WorldEdit style, no setBlock update reaches us
		check(check(p) == PlanGuard.BORDER_CHANGED, "a no-update change next to a hidden border ore is noticed");
		p = plan();
		check(Arrays.stream(p.idx()).noneMatch(i -> i == (15 | 5 << 4 | 5 << 8)), "the now exposed ore is no longer hidden");
		check(check(p) == PlanGuard.OK, "and the new plan is valid");
		// ---- a neighbour that unloads and loads again does not rebuild ----
		bLoaded = false; check(check(p) == PlanGuard.OK, "unload alone: valid");
		bLoaded = true; check(check(p) == PlanGuard.OK, "reload of the same neighbour: valid (the border check still guards the content)");
		// ---- cost: a fully loaded chunk with only interior ore makes no lookups at all ----
		fill(); world[7][8][8] = ORE; world[8][3][9] = ORE; bLoaded = true; p = plan();
		lookups = 0; for (int i = 0; i < 1000; i++) check(check(p) == PlanGuard.OK, "steady"); check(lookups == 0, "no lookups for interior-only plans: " + lookups);
		// ---- cost: border ore costs at most its own faces per check ----
		fill(); world[15][5][5] = ORE; world[0][6][0] = ORE; p = plan();
		lookups = 0; check(check(p) == PlanGuard.OK, "steady with border ore"); check(lookups <= 3, "one face for the edge ore, two for the corner: " + lookups);
		// ---- PlanGuard laziness ----
		int[] calls = {0, 0, 0};
		check(PlanGuard.check(() -> { calls[0]++; return true; }, 15, () -> { calls[1]++; return 15; }, () -> { calls[2]++; return true; }) == PlanGuard.OK && calls[1] == 0, "mask lookup skipped when all four were loaded");
		calls[0] = calls[1] = calls[2] = 0;
		check(PlanGuard.check(() -> { calls[0]++; return false; }, 0, () -> { calls[1]++; return 15; }, () -> { calls[2]++; return false; }) == PlanGuard.OWN_STALE && calls[1] == 0 && calls[2] == 0, "own stale stops before the dearer checks");
		calls[0] = calls[1] = calls[2] = 0;
		check(PlanGuard.check(() -> true, 0, () -> { calls[1]++; return 1; }, () -> { calls[2]++; return false; }) == PlanGuard.NEIGHBOUR_LOADED && calls[2] == 0, "neighbour check stops before the border check");
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("PlanLifecycleTest ok");
	}
}
