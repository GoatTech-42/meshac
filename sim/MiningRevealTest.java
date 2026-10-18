import dev.meshac.veil.DigRule;
import dev.meshac.veil.XrayCore;

/**
 * Pure model of the digging cases in RIG-QUICKSTART section 6 (rows 6.1 to 6.4).
 * Hardness values are the vanilla numbers read from Blocks.<clinit> with javap in the 1.21.11, 26.1.2, 26.2 and 26.3 jars (all four agree).
 * Per tick progress is speed / hardness / 30 with the right tool, as Mining.digStop sees it through getDestroyProgress.
 * The client times the dig by the block it sees, the server judges by the block it holds.
 */
public class MiningRevealTest {
	static int fails = 0;
	static void check(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL " + what); } }

	static final float STONE = 1.5f, DEEPSLATE = 3.0f, NETHERRACK = 0.4f, END_STONE = 3.0f;
	// fake ore on stone, on deepslate, in the nether (the base block is what the server holds under the fake)
	static final String[] FAKE_NAME = {"coal/iron/gold", "deepslate coal/iron/gold/diamond", "nether quartz"};
	static final float[] FAKE_H = {3.0f, 4.5f, 3.0f};
	static final float[] BASE_H = {STONE, DEEPSLATE, NETHERRACK};
	static final float[] SPEEDS = {1f, 2f, 4f, 6f, 8f, 9f, 12f, 16f, 24f, 40f};

	static float per(float speed, float hardness) { return speed / hardness / 30f; }

	/** Client digs the block it sees from tick 0 and sends STOP once its own progress reaches 1. Returns the tick count between START and STOP. */
	static int clientTicks(float seenDelta) {
		float p = 0; int t = 0;
		while (p < 1f) { p += seenDelta; t++; if (t > 100000) break; }
		return t - 1; // Ticks counted between START and STOP, the STOP lands on the tick that completes the dig
	}

	public static void main(String[] a) {
		// 6.1 and 6.2: a whole dig of a fake ore. The server holds the base block, which is softer, so it never reads an early stop.
		for (int i = 0; i < FAKE_H.length; i++) for (float sp : SPEEDS) {
			float seen = per(sp, FAKE_H[i]), real = per(sp, BASE_H[i]);
			if (seen >= 1f || real >= 1f) continue; // instant break, the rule skips it
			int t = clientTicks(seen);
			check(!DigRule.early(real, t), "fake over base, no reveal: " + FAKE_NAME[i] + " speed " + sp + " ticks " + t);
		}
		// 6.4 variant K: server resends real base block at tick r, client keeps its progress and carries on against the base block.
		for (int i = 0; i < FAKE_H.length; i++) for (float sp : SPEEDS) {
			float seen = per(sp, FAKE_H[i]), real = per(sp, BASE_H[i]);
			if (seen >= 1f || real >= 1f) continue;
			int full = clientTicks(seen);
			for (int r = 0; r <= full; r++) {
				float p = r * seen; int t = r;
				while (p < 1f && t < 100000) { p += real; t++; }
				int ticks = t - 1;
				check(!DigRule.early(real, ticks), "reveal keeps progress: " + FAKE_NAME[i] + " speed " + sp + " reveal at " + r + " ticks " + ticks);
			}
		}
		// 6.4 variant R: the client restarts the dig on the base block, the server gets a new START, ticks count from the restart.
		for (int i = 0; i < FAKE_H.length; i++) for (float sp : SPEEDS) {
			float real = per(sp, BASE_H[i]);
			if (real >= 1f) continue;
			int ticks = clientTicks(real);
			check(!DigRule.early(real, ticks), "reveal restarts dig: " + FAKE_NAME[i] + " speed " + sp + " ticks " + ticks);
		}
		// The ordering the doc claims: every fake ore is harder than the base block under it.
		for (int i = 0; i < FAKE_H.length; i++) check(FAKE_H[i] > BASE_H[i], "fake harder than base: " + FAKE_NAME[i]);
		// The reverse case, a hidden real block dug while the client still sees plain stone: only softer-looking-than-real blocks can flag.
		// Real ore under stone (3.0 vs 1.5) sits right on the 0.5 line and does not flag; debris under netherrack and a spawner under stone do.
		check(!flagsHidden(3.0f, STONE, 8f), "hidden ore shown as stone, speed 8");
		check(!flagsHidden(4.5f, DEEPSLATE, 8f), "hidden deepslate ore shown as deepslate, speed 8");
		check(!flagsHidden(2.5f, STONE, 8f), "hidden chest shown as stone, speed 8");
		check(flagsHidden(30f, NETHERRACK, 8f), "known risk: ancient debris shown as netherrack");
		check(flagsHidden(5.0f, STONE, 2f), "known risk: spawner shown as stone");
		// A reveal of the position resends the block when it is hidden or fake.
		check(XrayCore.resendOnReveal(true, false, 7L, 1, 2, 3, 0.0), "hidden block is resent on reveal");
		check(XrayCore.resendOnReveal(false, true, 7L, 1, 2, 3, 1.0), "fake ore position is resent on reveal");
		check(!XrayCore.resendOnReveal(false, false, 7L, 1, 2, 3, 1.0), "plain exposed block is not resent");
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("MiningRevealTest ok");
	}

	/** Client sees seenH, server holds realH. True when the server reads an early stop. */
	static boolean flagsHidden(float realH, float seenH, float sp) {
		float seen = per(sp, seenH), real = per(sp, realH);
		if (seen >= 1f || real >= 1f) return false;
		return DigRule.early(real, clientTicks(seen));
	}
}
