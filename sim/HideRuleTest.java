import dev.meshac.veil.DigRule;
import dev.meshac.veil.HideRule;

/** HideRule against DigRule: a masked block never reads as an early stop, and the blocks it refuses are exactly the ones that would. Hardness from Blocks.<clinit> in the 1.21.11, 26.1.2, 26.2 and 26.3 jars. */
public class HideRuleTest {
	static int fails = 0;
	static void check(boolean ok, String w) { if (!ok) { fails++; System.out.println("FAIL " + w); } }
	static final String[] REAL = {"ore", "deepslate ore", "chest", "trapped chest", "spawner", "ender chest", "ancient debris"};
	static final float[] RH = {3.0f, 4.5f, 2.5f, 2.5f, 5.0f, 22.5f, 30.0f};
	static final String[] SHOWN = {"stone", "deepslate", "netherrack", "end stone"};
	static final float[] SH = {1.5f, 3.0f, 0.4f, 3.0f};

	static boolean flags(float realH, float shownH, float sp) {
		float seen = sp / shownH / 30f, real = sp / realH / 30f;
		if (seen >= 1f || real >= 1f) return false;
		float p = 0; int t = 0;
		while (p < 1f) { p += seen; t++; }
		return DigRule.early(real, t - 1);
	}

	public static void main(String[] a) {
		for (int i = 0; i < RH.length; i++) for (int j = 0; j < SH.length; j++) {
			boolean safe = HideRule.safeToMask(RH[i], SH[j]);
			boolean anyFlag = false;
			for (float sp = 1f; sp <= 60f; sp += 0.5f) anyFlag |= flags(RH[i], SH[j], sp);
			if (safe) check(!anyFlag, "masked " + REAL[i] + " as " + SHOWN[j] + " must never flag");
			else check(anyFlag, "refused " + REAL[i] + " as " + SHOWN[j] + " should be one that can flag (otherwise the rule is too strict)");
			System.out.println(REAL[i] + " as " + SHOWN[j] + ": " + (safe ? "mask" : "leave"));
		}
		// the blocks players care about most stay masked where they live
		check(HideRule.safeToMask(3.0f, 1.5f), "ore under stone masked");
		check(HideRule.safeToMask(4.5f, 3.0f), "deepslate ore masked");
		check(HideRule.safeToMask(2.5f, 1.5f), "chest under stone masked");
		check(HideRule.safeToMask(5.0f, 3.0f), "spawner under deepslate masked");
		check(!HideRule.safeToMask(5.0f, 1.5f), "spawner under stone left alone");
		check(!HideRule.safeToMask(30.0f, 0.4f), "ancient debris in netherrack left alone");
		check(!HideRule.safeToMask(22.5f, 1.5f), "ender chest under stone left alone");
		check(HideRule.safeToMask(0f, 1.5f), "instant block");
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("HideRuleTest ok");
	}
}
