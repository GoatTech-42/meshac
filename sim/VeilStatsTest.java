import dev.meshac.veil.VeilStats;

public class VeilStatsTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		check(VeilStats.selfCheck() == null, "self-check passes on the real classes: " + VeilStats.selfCheck());
		dev.meshac.veil.EspCore.Params bad = new dev.meshac.veil.EspCore.Params(); bad.nearRadius = 0;
		String why = VeilStats.selfCheck(bad); check(why != null && why.contains("near"), "a zero near radius fails the check: " + why);
		dev.meshac.veil.EspCore.Params slow = new dev.meshac.veil.EspCore.Params(); slow.cullAfterChecks = 1000;
		check(VeilStats.selfCheck(slow) != null, "a veil that never hides fails the check");
		VeilStats s = new VeilStats();
		check(s.avgTickMicros() == 0 && s.hidesPerTick() == 0, "fresh counters are zero");
		check(s.line(0, 0, true).contains("0 hidden pairs of 0 tracked") && s.line(0, 0, true).contains("tracked index ok"), "fresh line: " + s.line(0, 0, true));
		for (int t = 0; t < 400; t++) s.tick(t, 50_000, 30, 4, t % 10 == 0 ? 2 : 0, 0, t % 100 == 0 ? 1 : 0, false);
		check(s.ticks == 400 && s.rays == 1600 && s.candidates == 12000, "totals: " + s.ticks + " " + s.rays + " " + s.candidates);
		check(Math.abs(s.avgTickMicros() - 50.0) < 1e-9 && s.worstTickNanos == 50_000, "avg and worst");
		check(s.budgetSkips == 4, "budget skips " + s.budgetSkips);
		check(s.hidesPerTick() > 0.15 && s.hidesPerTick() < 0.25, "hides per tick near 0.2: " + s.hidesPerTick());
		s.tick(401, 900_000, 1, 0, 0, 0, 0, true);
		check(s.worstTickNanos == 900_000 && s.fallbackScans == 1, "worst and fallback");
		String l = s.line(7, 120, true);
		check(l.contains("7 hidden pairs of 120 tracked") && l.contains("1 fallback scans") && l.contains("worst 900 us"), "line: " + l);
		check(s.line(0, 0, false).contains("tracked index OFF, full scan in use"), "index off text");
		check(VeilStats.indexState(true, 0).equals("tracked index ok"), "state ok");
		s.reset();
		check(s.ticks == 0 && s.fallbackScans == 0 && s.lastTick == -1 && s.hidesPerTick() == 0, "reset clears everything, nothing is persisted");
		// a tick counter that goes backwards (server restart inside one JVM, world switch) must not produce nonsense
		s.tick(5000, 10, 0, 0, 1, 0, 0, false); s.tick(10, 10, 0, 0, 1, 0, 0, false); s.tick(300, 10, 0, 0, 1, 0, 0, false);
		check(!Double.isNaN(s.hidesPerTick()) && !Double.isInfinite(s.hidesPerTick()) && s.hidesPerTick() >= 0, "tick going backwards stays finite: " + s.hidesPerTick());
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("VeilStatsTest ok");
	}
}
