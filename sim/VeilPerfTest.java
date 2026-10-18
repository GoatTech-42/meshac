import dev.meshac.veil.EspCore;
import java.util.Random;

/** Hot spot fix: samplePointsInto must return exactly what samplePoints returns, without allocating per call. Also generous ceilings so a future slow-down fails loudly. */
public class VeilPerfTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		Random r = new Random(3); double[] buf = new double[EspCore.SAMPLE_DOUBLES];
		for (int i = 0; i < 2000; i++) {
			double x = r.nextDouble() * 1000 - 500, y = r.nextDouble() * 300 - 60, z = r.nextDouble() * 1000 - 500, w = 0.1 + r.nextDouble(), h = 0.1 + r.nextDouble() * 3;
			double[] want = EspCore.samplePoints(x, y, z, x + w, y + h, z + w);
			int n = EspCore.samplePointsInto(buf, x, y, z, x + w, y + h, z + w);
			check(n == want.length, "count " + n + " vs " + want.length);
			for (int k = 0; k < n; k++) if (buf[k] != want[k]) { check(false, "value " + k + " differs"); break; }
		}
		check(EspCore.SAMPLE_DOUBLES == 13 * 3, "13 points");
		// ceilings: 100 viewers x 500 entities, a full 4 tick cycle of decisions must stay far below one tick budget
		EspCore e = new EspCore(new EspCore.Params()); long t0 = System.nanoTime();
		for (int round = 0; round < 3; round++) for (int v = 0; v < 100; v++) for (int en = 0; en < 500; en++) e.update(v, en, round * 4L, 5 + (v * 7 + en) % 120, false, (v + en) % 3 == 0);
		double ms = (System.nanoTime() - t0) / 1e6 / 3;
		check(ms < 400, "50000 pair decisions took " + ms + " ms per cycle (ceiling 400, this includes JIT warm-up)");
		if (fails > 0) { System.out.println(fails + " FAIL"); System.exit(1); }
		System.out.println("VeilPerfTest ok");
	}
}
