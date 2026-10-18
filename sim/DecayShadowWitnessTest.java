import java.nio.file.*;
import java.util.*;

/** Source witness for the Stage-1 decay shadow in Movement (grace-boundary persistence draft, log-only).
 *  Per-site coverage, not aggregate counts: every reset site that clears the live window must handle the
 *  shadow on the same line (decay at the exempt/grace boundary, clear at full resets); the shadow replays
 *  the live window's claim set/order/threshold; every collection line is TRACE-gated (MESHAC_TRACE env,
 *  unset by default); no shadow line reaches a Verdict/MonitorGate/damage/action path; the telemetry line
 *  reports both windows so a simultaneous live+shadow flag is never invisible; and the gndclear capture
 *  fires at every boundary that holds state in either window, so the replay harness sees every decay
 *  boundary. A live-server fixture needs the rig; this guards the structure until the battery runs it. */
public final class DecayShadowWitnessTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) throws Exception {
		String t = Files.readString(Paths.get("src/main/java/dev/meshac/Movement.java"));
		check(t.contains("final GroundEvidence gndEvDecay = new GroundEvidence(GROUND_WINDOW, GROUND_FLAG_AT);"), "shadow declared on S with the live window config (same threshold/window)");
		int claims = 0, decays = 0, resets = 0;
		for (String l : t.split("\n")) {
			if (l.contains("s.gndEv.clear()")) { // per-site reset coverage: each live-window clear handles the shadow on the same line
				resets++;
				check(l.contains("s.gndEvDecay.decay()") || l.contains("s.gndEvDecay.clear()"), "live-window reset site leaves the shadow unhandled: " + l.trim());
			}
			if (l.contains("s.gndEvDecay.clear()")) // every full reset of the shadow is captured for exact replay
				check(l.contains("[trace] gndreset"), "full shadow reset without a gndreset capture line: " + l.trim());
			if (l.contains("gndEvDecay.decay()")) {
				decays++;
				check(l.contains("[trace] gndclear"), "shadow decay outside the exempt/grace gndclear branch: " + l.trim());
			}
			if (!l.contains("gndEvDecay")) continue;
			if (!l.contains("gndEvDecay.clear")) { // full-reset clears are inert; other shadow lines must stay off verdict paths
				check(!l.contains("Verdict") && !l.contains("MonitorGate") && !l.contains("causeFallDamage") && !l.contains("hit =") && !l.contains("armNoFall") && !l.contains("return new double[]"),
					"shadow line reaches a verdict/damage/action path: " + l.trim());
			}
			if (l.contains("gndEvDecay.add(claimAir)")) {
				claims++;
				check(l.contains("if (TRACE)"), "shadow claim collection is TRACE-gated (default off)");
			}
		}
		check(claims == 1, "shadow adds claims at exactly the live window's single claim site, found " + claims);
		check(decays == 1, "shadow decays at exactly one site (the exempt/grace reset), found " + decays);
		check(resets >= 4, "expected the live-window reset sites (scored branch excluded), found " + resets);
		check(t.split("\\[trace] gndreset").length - 1 == 3, "gndreset captured at all 3 full-reset sites (blink-setback, soft-return, post-flag)");
		check(t.contains("[trace] nofall decay-shadow {} count={} liveCount={} liveFlagged={}"), "shadow telemetry reports both windows (a simultaneous live+shadow flag is never invisible)");
		check(t.contains("s.decayLogAt = nowMs"), "shadow telemetry is rate-limited");
		check(t.contains("TRACE && (s.bufGround > 0 || s.gndEvDecay.unsupported() > 0)"), "gndclear capture fires at every boundary holding state in EITHER window (replay sees every decay boundary)");
		check(t.contains("[trace] gndclaim {} air={} count={}"), "gndclaim carries the post-add live count (the K4 jar-state receipt)");
		check(t.contains("private static final boolean TRACE = System.getenv(\"MESHAC_TRACE\") != null;"), "TRACE is the MESHAC_TRACE env switch, unset by default: the shadow is off by default");
		System.out.println(fails == 0 ? "DecayShadowWitnessTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
