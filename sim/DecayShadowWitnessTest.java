import java.nio.file.*;
import java.util.*;

/** Source witness for the Stage-1 decay shadow in Movement (grace-boundary persistence draft, log-only):
 *  the shadow replays the live window's claim set/order/threshold, decays only at the exempt/grace ground-claim
 *  reset, is cleared at every full-reset site, is default-off (TRACE-gated; trace defaults false), and no shadow
 *  line reaches a Verdict/MonitorGate/damage/action path. A live-server fixture needs the rig; this guards the
 *  structure until the battery runs it. */
public final class DecayShadowWitnessTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) throws Exception {
		String t = Files.readString(Paths.get("src/main/java/dev/meshac/Movement.java"));
		check(t.contains("final GroundEvidence gndEvDecay = new GroundEvidence(GROUND_WINDOW, GROUND_FLAG_AT);"), "shadow declared on S with the live window config (same threshold/window)");
		int claims = 0;
		for (String l : t.split("\n")) {
			if (!l.contains("gndEvDecay")) continue;
			String bare = l.replace("s.gndEvDecay.clear();", ""); // full-reset clears are inert; the line's other tokens belong to the reset itself
			if (!l.contains("gndEvDecay.clear"))
				check(!bare.contains("Verdict") && !bare.contains("MonitorGate") && !bare.contains("causeFallDamage") && !bare.contains("hit =") && !bare.contains("armNoFall") && !bare.contains("return new double[]"),
					"shadow line reaches a verdict/damage/action path: " + l.trim());
			if (l.contains("gndEvDecay.add(claimAir)")) {
				claims++;
				check(l.contains("if (TRACE)"), "shadow claim collection is TRACE-gated (default off)");
			}
		}
		check(claims == 1, "shadow adds claims at exactly the live window's single claim site, found " + claims);
		int decays = t.split("gndEvDecay\\.decay\\(\\)").length - 1;
		check(decays == 1, "shadow decays at exactly one site (the exempt/grace ground-claim reset), found " + decays);
		int evClears = t.split("s\\.gndEv\\.clear\\(\\)").length - 1;
		int shadowClears = t.split("s\\.gndEvDecay\\.clear\\(\\)").length - 1;
		check(evClears == shadowClears + 1, "shadow cleared at every full-reset site (gndEv.clear sites=" + evClears + "; the one without a shadow clear is the exempt/grace reset where the shadow decays instead)");
		check(t.contains("[trace] nofall decay-shadow") && t.contains("s.decayLogAt = nowMs"), "divergence trace line present and rate-limited");
		check(t.contains("private static final boolean TRACE = System.getenv(\"MESHAC_TRACE\") != null;"), "TRACE is the MESHAC_TRACE env switch, unset by default: the shadow is off by default");
		System.out.println(fails == 0 ? "DecayShadowWitnessTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
