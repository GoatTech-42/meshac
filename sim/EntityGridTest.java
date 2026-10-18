import dev.meshac.veil.EntityGrid;
import java.util.*;

/** Candidate search for anti-ESP: the grid must return a superset of the exact radius set and never lose an entity. */
public class EntityGridTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }

	static Set<Integer> near(EntityGrid g, double x, double z, double r) {
		int[] out = new int[16]; int n = g.near(x, z, r, out);
		if (n > out.length) { out = new int[n]; n = g.near(x, z, r, out); }
		Set<Integer> s = new HashSet<>(); for (int i = 0; i < n; i++) s.add(g.idAt(out[i])); check(s.size() == n, "no duplicates"); return s;
	}

	public static void main(String[] a) {
		Random r = new Random(7); EntityGrid g = new EntityGrid();
		int N = 5000; double[] xs = new double[N], zs = new double[N];
		for (int i = 0; i < N; i++) { xs[i] = r.nextDouble() * 2000 - 1000; zs[i] = r.nextDouble() * 2000 - 1000; g.add(i, xs[i], zs[i]); }
		check(g.size() == N, "size");
		for (int q = 0; q < 300; q++) {
			double x = r.nextDouble() * 2000 - 1000, z = r.nextDouble() * 2000 - 1000, rad = r.nextInt(3) == 0 ? 8 : 136;
			Set<Integer> got = near(g, x, z, rad); Set<Integer> exact = new HashSet<>();
			for (int i = 0; i < N; i++) if (Math.hypot(xs[i] - x, zs[i] - z) <= rad) exact.add(i);
			check(got.containsAll(exact), "grid lost an entity inside the radius at query " + q);
			// every extra returned entity lies within one cell diagonal beyond the square
			for (int id : got) if (Math.abs(xs[id] - x) > rad + 16 || Math.abs(zs[id] - z) > rad + 16) { check(false, "returned an entity more than a cell outside"); break; }
		}
		// negative coordinates and cell boundaries
		EntityGrid b = new EntityGrid(); b.add(1, -0.0001, -0.0001); b.add(2, 0, 0); b.add(3, -16.0, -16.0); b.add(4, 15.9999, 15.9999); b.add(5, -16.0001, 0);
		check(near(b, 0, 0, 1).containsAll(List.of(1, 2)), "cell boundary at origin");
		check(near(b, -16, -16, 0.5).contains(3), "exactly on a negative cell edge");
		check(near(b, 0, 0, 20).containsAll(List.of(1, 2, 3, 4, 5)), "radius 20 sees all five");
		// stale cells never leak after clear(), and reuse works over many rebuilds
		EntityGrid c = new EntityGrid();
		for (int round = 0; round < 50; round++) {
			c.clear(); int base = round * 1000;
			for (int i = 0; i < 300; i++) c.add(base + i, (round * 37 + i * 3) % 400 - 200, (round * 11 + i * 7) % 400 - 200);
			check(c.size() == 300, "size after rebuild");
			for (int id : near(c, 0, 0, 1000)) check(id >= base && id < base + 300, "stale entity from an older build returned: " + id);
		}
		c.clear(); check(near(c, 0, 0, 1000).isEmpty() && c.size() == 0, "empty after clear");
		// many entities in one cell, many cells (growth of both tables)
		EntityGrid d = new EntityGrid(); for (int i = 0; i < 4000; i++) d.add(i, 3, 3); check(near(d, 3, 3, 1).size() == 4000, "4000 in one cell");
		EntityGrid e = new EntityGrid(); for (int i = 0; i < 6000; i++) e.add(i, (i % 100) * 16 + 1, (i / 100) * 16 + 1); check(near(e, 800, 480, 2000).size() == 6000, "6000 cells all reachable");
		// too-small output array reports the true count
		int[] tiny = new int[2]; check(d.near(3, 3, 1, tiny) == 4000, "count reported beyond the array");
		// huge coordinates do not crash
		EntityGrid h = new EntityGrid(); h.add(9, 29999999, -29999999); check(near(h, 29999990, -29999990, 20).contains(9), "world border coordinates");
		// cost: 100 viewers x 500 entities, 25 viewer queries per tick, rebuild each tick
		EntityGrid t = new EntityGrid(); long t0 = System.nanoTime();
		for (int tick = 0; tick < 200; tick++) { t.clear(); for (int i = 0; i < 500; i++) t.add(i, (i * 31 + tick) % 600 - 300, (i * 17) % 600 - 300); int[] out = new int[600]; for (int v = 0; v < 25; v++) t.near(v * 10 - 100, 0, 136, out); }
		double ms = (System.nanoTime() - t0) / 1e6 / 200; check(ms < 5, "grid rebuild plus 25 queries took " + ms + " ms per tick (ceiling 5, includes JIT warm-up)");
		if (fails > 0) { System.out.println(fails + " FAIL"); System.exit(1); }
		System.out.println("EntityGridTest ok");
	}
}
