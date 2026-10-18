import dev.meshac.veil.EspCore;
import dev.meshac.veil.XrayCore;
import java.util.Random;

/**
 * Pure perf bench for the veil decision logic. Not part of run-tests.sh (timing is machine dependent).
 *   javac -d /tmp/vb src/main/java/dev/meshac/veil/XrayCore.java src/main/java/dev/meshac/veil/EspCore.java sim/VeilBench.java && nice -n 19 java -Xmx256m -cp /tmp/vb VeilBench
 * It measures only the logic the veil adds on top of Minecraft's own block and ray calls: those (getBlockState, voxel walk) are not available here, so the numbers are a floor, not the full cost.
 */
public class VeilBench {
	static final int W = 48, H = 384, SZ = 48; // a 16x16 chunk plus a 16 block margin on each side so neighbour lookups stay in range
	static byte[] world = new byte[W * H * SZ]; // 0 air, 1 stone, 2 ore
	static int at(int x, int y, int z) { return (y * SZ + z) * W + x; }

	static final class Grid implements XrayCore.Blocks {
		public boolean hidden(int x, int y, int z) { return world[at(x, y, z)] == 2; }
		public boolean transparent(int x, int y, int z) { return y < 0 || y >= H ? false : world[at(x, y, z)] == 0; }
	}

	static long sink;
	public static void main(String[] a) {
		Random r = new Random(1);
		for (int i = 0; i < world.length; i++) { int v = r.nextInt(1000); world[i] = (byte) (v < 20 ? 2 : v < 70 ? 0 : 1); } // 2% ore, 5% air pockets: harsher than real terrain
		Grid g = new Grid();
		// ---- anti-xray: one full-height chunk column, 16x384x16 = 98304 blocks ----
		for (int pass = 0; pass < 3; pass++) {
			long t0 = System.nanoTime(); int hid = 0, fake = 0, ores = 0;
			for (int y = 1; y < H - 1; y++) for (int z = 16; z < 32; z++) for (int x = 16; x < 32; x++) {
				if (g.hidden(x, y, z)) { ores++; if (XrayCore.shouldHide(g, x, y, z)) hid++; }
				else if (world[at(x, y, z)] == 1 && XrayCore.fakeOreAt(12345L, x, y, z, 0.02) && !XrayCore.exposed(g, x, y, z)) fake++;
			}
			long dt = System.nanoTime() - t0;
			System.out.printf("xray chunk 16x384x16 pass %d: %.2f ms (ores %d, hidden %d, fake %d)%n", pass, dt / 1e6, ores, hid, fake);
			sink += hid + fake;
		}
		long t0 = System.nanoTime(); int chunks = 200; for (int c = 0; c < chunks; c++) for (int y = 1; y < H - 1; y += 1) for (int z = 16; z < 32; z++) for (int x = 16; x < 32; x++) if (g.hidden(x, y, z) && XrayCore.shouldHide(g, x, y, z)) sink++;
		System.out.printf("xray hidden-only: %.3f ms per chunk (200 chunk avg)%n", (System.nanoTime() - t0) / 1e6 / chunks);

		// ---- anti-ESP: 100 viewers x 500 entities ----
		int V = 100, E = 500; EspCore esp = new EspCore(new EspCore.Params());
		Random q = new Random(2); boolean[] vis = new boolean[V * E]; double[] dist = new double[V * E];
		for (int i = 0; i < vis.length; i++) { vis[i] = q.nextInt(10) < 3; dist[i] = 5 + q.nextDouble() * 120; }
		// logic only: every pair once per 4 ticks, a quarter of the viewers per tick
		for (int round = 0; round < 3; round++) {
			long t1 = System.nanoTime(); int upd = 0; long tick = round * 4L;
			for (int v = 0; v < V; v++) for (int e = 0; e < E; e++) { sink += esp.update(v, e, tick, dist[v * E + e], false, vis[v * E + e]).ordinal(); upd++; }
			double ms = (System.nanoTime() - t1) / 1e6;
			System.out.printf("esp update, all %d pairs (one 4 tick cycle) round %d: %.2f ms = %.2f ms per tick at 1/4 of viewers per tick%n", upd, round, ms, ms / 4);
		}
		// sample points: allocation per LOS test. 200 pairs per tick is the budget (RAYS_PER_TICK).
		long t2 = System.nanoTime(); int tests = 200 * 400; // 400 ticks = 20 s of budget-limited culling
		for (int i = 0; i < tests; i++) { double[] p = EspCore.samplePoints(i, 64, i, i + 0.6, 65.8, i + 0.6); sink += p.length; }
		System.out.printf("esp samplePoints (allocating): %.2f ms per tick at 200 tests%n", (System.nanoTime() - t2) / 1e6 / 400);
		double[] buf = new double[EspCore.SAMPLE_DOUBLES];
		long t3 = System.nanoTime();
		for (int i = 0; i < tests; i++) { sink += EspCore.samplePointsInto(buf, i, 64, i, i + 0.6, 65.8, i + 0.6); }
		System.out.printf("esp samplePointsInto (reused buffer): %.2f ms per tick at 200 tests%n", (System.nanoTime() - t3) / 1e6 / 400);
		System.out.println("sink " + sink);
	}
}
