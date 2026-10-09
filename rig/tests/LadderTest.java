import dev.meshac.Ladder;
/** Plain-java check of the offence ladder. Run: java -cp build/classes/java/main rig/tests/LadderTest.java */
public class LadderTest {
	static int fails = 0;
	static void eq(String what, Object got, Object want) { boolean ok = got.equals(want); if (!ok) fails++; System.out.println((ok ? "ok   " : "FAIL ") + what + ": " + got + (ok ? "" : " (want " + want + ")")); }
	static int rung(double hl, boolean skip, double... agesDays) { return Ladder.rung(Ladder.effectiveOffences(agesDays, hl), 1, skip); }
	public static void main(String[] a) {
		double hl = 14, base = 10, growth = 6, max = 10080;
		eq("no offences -> kick (rung 1)", rung(hl, false), 1);
		eq("one offence 80 s old -> second removal is a tempban (rung 2)", rung(hl, false, 80 / 86400.0), 2);
		eq("one offence 6 h old -> tempban", rung(hl, false, 0.25), 2);
		eq("one offence 1 day old -> tempban", rung(hl, false, 1), 2);
		eq("one offence 14 days old (half forgiven) -> kick again", rung(hl, false, 14), 1);
		eq("two fresh offences -> rung 3", rung(hl, false, 0.001, 0.002), 3);
		eq("three fresh offences -> rung 4", rung(hl, false, 0.001, 0.002, 0.003), 4);
		eq("tier A skips one rung (first offence) ", rung(hl, true), 2);
		eq("tempban 1 (rung 2) minutes", Ladder.tempMinutes(2, 1, base, growth, max), 10L);
		eq("tempban 2 (rung 3) minutes", Ladder.tempMinutes(3, 1, base, growth, max), 60L);
		eq("tempban 3 (rung 4) minutes", Ladder.tempMinutes(4, 1, base, growth, max), 360L);
		eq("tempban 4 (rung 5) minutes", Ladder.tempMinutes(5, 1, base, growth, max), 2160L);
		eq("tempban 5 (rung 6) capped at 7 days", Ladder.tempMinutes(6, 1, base, growth, max), 10080L);
		System.out.println(fails == 0 ? "ALL PASS" : fails + " FAILED"); System.exit(fails == 0 ? 0 : 1);
	}
}
