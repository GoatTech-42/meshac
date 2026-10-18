import dev.meshac.SustainBudget;

/** The Bedrock sustain budget today is 1.30x. Its shadow (compare mode) is the Java budget, 1.00x: bunching is already forgiven by the dt scaling in step(). */
public final class BedrockBudgetTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	static double shadow() { return SustainBudget.perTick(1.0, false, 0, 1, false, false, false, 0); }
	static double today() { return SustainBudget.perTick(1.0, false, 0, 1, true, false, false, 0); }
	/** h per tick average bps, packets carry `ticks` ticks each (dt = ticks * 0.05) */
	static int ticksToFlag(double perTickBudget, double bps, int ticks, int seconds) {
		double bal = 0; for (int p = 1; p <= 20 * seconds / ticks; p++) { bal = SustainBudget.step(bal, bps / 20.0 * ticks, perTickBudget, 0.05 * ticks); if (SustainBudget.flagged(bal)) return p * ticks; }
		return -1;
	}
	public static void main(String[] a) {
		check(Math.abs(today() / shadow() - 1.30) < 1e-9, "today is exactly 1.30x the shadow");
		// legit 7.1 bps sprint-jump packets that carry 1, 2, 3 or 4 ticks (Bedrock bunching, lag): the shadow never flags
		for (int n = 1; n <= 4; n++) check(ticksToFlag(shadow(), 7.1, n, 120) == -1, "legit 7.1 bps in " + n + "-tick packets flagged by the shadow");
		// a one-second stall released in one packet (dt clamp at 1 s) and an alternating 1/3 pattern
		check(SustainBudget.step(0, 0.355 * 20, shadow(), 1.0) == 0, "stall release of legit motion built balance");
		double bal = 0; for (int i = 0; i < 400; i++) bal = SustainBudget.step(bal, 0.355 * (i % 2 == 0 ? 1 : 3), shadow(), 0.05 * (i % 2 == 0 ? 1 : 3)); check(bal == 0, "alternating 1/3 tick packets built balance " + bal);
		// cheats: constant 8.15 bps and 9.2 bps (1.3x)
		check(ticksToFlag(shadow(), 8.15, 1, 60) > 0 && ticksToFlag(shadow(), 8.15, 1, 60) < 160, "shadow should flag 8.15 bps within 8 s");
		check(ticksToFlag(shadow(), 9.23, 1, 60) > 0, "shadow flags 9.2 bps");
		check(ticksToFlag(today(), 8.15, 1, 60) == -1 && ticksToFlag(today(), 9.23, 1, 60) == -1, "KNOWN MISS pinned: today's 1.30x budget never flags 8.15 or 9.2 bps");
		check(ticksToFlag(today(), 10.0, 1, 60) > 0, "today flags 10 bps");
		check(SustainBudget.shadowLine(".x", 4.2, 0.37).equals("[bedrocktol] would-have-flagged .x balance=4.20 budget=0.370/tick"), SustainBudget.shadowLine(".x", 4.2, 0.37));
		// the one live Bedrock speed flag: ".MICTHEPROFIT speed 0.52>0.34": 0.52 in one packet is two ticks of a 0.26 sprint walk
		check(Math.abs(0.52 - 2 * 0.26) < 1e-9 && 0.26 < 0.34, "the logged 0.52 packet is two normal ticks, so it is bunching and not speed");
		System.out.println(fails == 0 ? "BedrockBudgetTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
