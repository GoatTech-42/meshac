import dev.meshac.StepAccumulator;

/** The stair-tolerant step accumulator (case MA-M9HG fix) against the legit battery and step-hack controls. */
public final class StepAccumulatorTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	/** Feed a dy sequence (all onGround, no jump boost); count flags. */
	static int run(double[] dys, boolean enabled) {
		StepAccumulator acc = new StepAccumulator();
		int n = 0;
		for (double dy : dys) if (acc.packet(true, dy, false, enabled) != null) n++;
		return n;
	}

	/** A climb of discrete steps: rise H every k ticks, zeros between, total packets n. */
	static double[] stairs(double h, int k, int n) {
		double[] d = new double[n];
		for (int i = k - 1; i < n; i += k) d[i] = h;
		return d;
	}

	public static void main(String[] a) {
		// stairs and slabs (0.5) at every cadence from sprint (k=2) to slow walk (k=8)
		for (int k = 2; k <= 8; k++) check(run(stairs(0.5, k, 4000), true) == 0, "0.5 stairs at cadence " + k + " flagged");
		// full-block stairs are two 0.5 steps; snow-layer ramps (0.125), carpet (0.0625), cake-like 0.4375 steps
		check(run(stairs(0.125, 1, 4000), true) == 0, "sprint up a snow-layer ramp flagged");
		check(run(stairs(0.0625, 1, 4000), true) == 0, "carpet ramp flagged");
		check(run(stairs(0.4375, 2, 4000), true) == 0, "0.4375 steps flagged (inside the reset band)");
		check(run(stairs(0.6, 3, 4000), true) == 0, "full vanilla auto-steps (0.6) flagged");
		check(run(stairs(0.42, 2, 4000), true) == 0, "band floor 0.42 flagged");
		// a seeded mixed walk: slabs, stairs, snow, flat
		{
			double[] d = new double[6000]; java.util.Random r = new java.util.Random(3);
			double[] hs = {0, 0, 0.5, 0.125, 0.0625, 0.5, 0};
			for (int i = 0; i < d.length; i++) d[i] = hs[r.nextInt(hs.length)];
			check(run(d, true) == 0, "mixed legit climb flagged");
		}
		// cheat controls
		check(run(stairs(0.3, 1, 60), true) > 0, "step hack 0.3/tick missed");
		check(run(stairs(0.2, 1, 60), true) > 0, "step hack 0.2/tick missed (0.8 in the window)");
		check(run(stairs(0.15, 1, 60), true) == 0, "0.15/tick is under the limit in 4 ticks: known bound, not a license");
		// window expiry: two 0.3 rises more than 4 packets apart never share a window
		{
			double[] d = new double[12]; d[1] = 0.3; d[8] = 0.3;
			check(run(d, true) == 0, "window did not expire between rises");
		}
		// jump boost band: 0.9 steps with boost reset, and the limit widens by the same band
		{
			StepAccumulator acc = new StepAccumulator(); int n = 0;
			for (int i = 0; i < 2000; i++) if (acc.packet(true, i % 3 == 2 ? 0.9 : 0, true, true) != null) n++;
			check(n == 0, "0.9 auto-steps with jump boost flagged");
		}
		// disabled mode flags nothing but still returns null rather than throwing
		check(run(stairs(0.3, 1, 60), false) == 0, "disabled accumulator flagged");
		// MA-M9HG regression pin (case kick 2026-10-11, "step rose 1.06 on the ground", pardoned):
		// a window that totals 1.06 WITH a vanilla 0.5 step in it must not flag - the step resets
		// the window, leaving 0.56 under the 0.65 limit. The case cannot recur while vanilla
		// steps reset. The same 1.06 total with NO vanilla-step packet MUST flag: the rule still
		// catches a real 1.06 rise in a 4-packet window.
		{
			double[] m9hg = {0.5, 0.2, 0.2, 0.16}; // totals 1.06, vanilla step first
			check(run(m9hg, true) == 0, "MA-M9HG shape (1.06 total with a vanilla step) flagged");
			double[] hack = {0.3, 0.3, 0.3, 0.16}; // same 1.06 total, no vanilla step
			check(run(hack, true) > 0, "1.06 total with no vanilla step missed (rule went soft)");
			// jump boost variant of the case: the band widens reset and limit together
			StepAccumulator acc = new StepAccumulator();
			String hit = null;
			for (double dy : new double[] {0.8, 0.2, 0.06}) // 0.8 = 0.5 + 0.3 boost band, totals 1.06
				hit = acc.packet(true, dy, true, true);
			check(hit == null, "MA-M9HG shape under jump boost flagged");
		}
		System.out.println(fails == 0 ? "StepAccumulatorTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
