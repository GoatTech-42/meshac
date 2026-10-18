import dev.meshac.FpGuards;
import dev.meshac.SustainBudget;

/** Pins the Bedrock rules that exist today. No Minecraft needed. */
public final class BedrockMovementTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	static int ticksToFlag(double bps, boolean bedrock) {
		double per = SustainBudget.perTick(1.0, false, 0, 1, bedrock, false, false, 0), bal = 0;
		for (int t = 1; t <= 20 * 60; t++) { bal = SustainBudget.step(bal, bps / 20.0, per, 0.05); if (SustainBudget.flagged(bal)) return t; }
		return -1;
	}
	public static void main(String[] a) {
		check(FpGuards.bedrockUuid(0L) && FpGuards.bedrockUuid(0x0000000000000000L) && !FpGuards.bedrockUuid(0x1234L << 32) && !FpGuards.bedrockUuid(-1L), "bedrock uuid rule: high half zero only");
		check(ticksToFlag(7.13, true) == -1 && ticksToFlag(7.13, false) == -1, "legit 7.13 bps flagged");
		check(ticksToFlag(8.15, false) > 0 && ticksToFlag(8.15, false) < 160, "java 8.15 bps cheat should flag in 8 s");
		check(ticksToFlag(8.15, true) == -1, "KNOWN MISS: bedrock 1.30 budget lets an 8.15 bps cheat through for 60 s");
		check(ticksToFlag(10.0, true) > 0, "bedrock 10 bps cheat should flag");
		// Bunched Bedrock packets: two ticks of legit sprint-jump in one packet, dt doubled, no balance
		double per = SustainBudget.perTick(1.0, false, 0, 1, true, false, false, 0), bal = 0;
		for (int i = 0; i < 200; i++) bal = SustainBudget.step(bal, 0.3564 * 2, per, 0.10);
		check(bal == 0, "bunched legit bedrock packets built balance " + bal);
		// Release after a stall: 1.0 s of legit motion arriving in one packet
		bal = SustainBudget.step(0, 0.3564 * 20, per, 1.0);
		check(bal == 0, "stall release of legit motion built balance " + bal);
		System.out.println(fails == 0 ? "BedrockMovementTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
