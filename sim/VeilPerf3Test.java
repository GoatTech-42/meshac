import dev.meshac.veil.XrayCore;

/** The fake ore roll runs for every buried stone block of every section sent. It must stay deterministic, unbiased and cheap. */
public class VeilPerf3Test {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		for (double ratio : new double[]{0.02, 0.15, 0.5}) {
			int n = 0, N = 400000; for (int i = 0; i < N; i++) if (XrayCore.fakeOreAt(77, i & 255, (i >> 8) % 300 - 60, i >> 6, ratio)) n++;
			double got = (double) n / N; check(Math.abs(got - ratio) < ratio * 0.08 + 0.002, "ratio " + ratio + " got " + got);
		}
		// neighbouring blocks must not correlate: count pairs where both are fake and compare with independence
		int both = 0, one = 0, N = 400000; for (int i = 0; i < N; i++) { boolean f1 = XrayCore.fakeOreAt(5, i, 10, 3, 0.1), f2 = XrayCore.fakeOreAt(5, i + 1, 10, 3, 0.1); if (f1) one++; if (f1 && f2) both++; }
		check(Math.abs((double) both / Math.max(1, one) - 0.1) < 0.02, "neighbours independent: " + (double) both / one);
		// all three axes matter
		int dx = 0, dy = 0, dz = 0; for (int i = 0; i < 20000; i++) { boolean b = XrayCore.fakeOreAt(5, i, i, i, 0.5); if (b != XrayCore.fakeOreAt(5, i + 1, i, i, 0.5)) dx++; if (b != XrayCore.fakeOreAt(5, i, i + 1, i, 0.5)) dy++; if (b != XrayCore.fakeOreAt(5, i, i, i + 1, 0.5)) dz++; }
		check(dx > 8000 && dy > 8000 && dz > 8000, "each axis changes the roll: " + dx + " " + dy + " " + dz);
		check(XrayCore.fakeOreAt(5, 3, 4, 5, 0.37) == XrayCore.fakeOreAt(5, 3, 4, 5, 0.37), "deterministic");
		long s = 0; for (int w = 0; w < 3; w++) { long t = System.nanoTime(); for (int i = 0; i < 3_000_000; i++) if (XrayCore.fakeOreAt(99L, i & 255, (i >> 8) & 127, i >> 12, 0.02)) s++; if (w == 2) { double ns = (System.nanoTime() - t) / 3e6; check(ns < 100, "fake roll under 100 ns per call even on a loaded box: " + ns); } }
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("VeilPerf3Test ok " + s);
	}
}
