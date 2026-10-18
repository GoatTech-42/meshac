import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
public final class GroundReplayTest {
	static String k4last = "";
	static int k4Run(String javaBin, String cp, String log) throws Exception {
		Path f = Files.createTempFile("k4nc", ".log");
		Files.writeString(f, log);
		Process pr = new ProcessBuilder(javaBin, "-cp", cp, "GroundReplay", f.toString()).redirectErrorStream(true).start();
		k4last = new String(pr.getInputStream().readAllBytes());
		return pr.waitFor();
	}

	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	static void eq(long[] r, long claims, long un, long max, long flags, long arms, String m) {
		check(r[0] == claims && r[1] == un && r[2] == max && r[3] == flags && r[4] == arms,
			m + " got claims=" + r[0] + " un=" + r[1] + " max=" + r[2] + " flags=" + r[3] + " arms=" + r[4]);
	}
	public static void main(String[] a) throws Exception {
		// interleaved spoof under the 3-of-5 flag: count 1,1,2,2,3 - one arm at >=2 (third claim), one flag at 3
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true)), 5, 3, 2), 5, 3, 3, 1, 1, "interleaved spoof arms below the flag then flags");
		// eviction: five supported claims drain the window; a refill re-flags (rising edges counted separately)
		eq(GroundReplay.score(List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2), 13, 6, 3, 2, 2, "drained window re-arms and re-flags");
		// clear: exempt/grace ground claim resets the window and the edges
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2), 4, 4, 2, 0, 2, "clear resets count and edges");
		// tighter window: W=3 T=2 - supported claim evicts the oldest unsupported
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true)), 3, 2, 2), 5, 3, 2, 2, 2, "W=3 T=2 eviction re-triggers edges");
		// armAt == flagAt behaves like the flag itself
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 3), 3, 3, 3, 1, 1, "arm at the flag level mirrors the flag");
		// decay policy: a clear forgives only the oldest unsupported claim - the same stream that stays
		// sub-flag under full clear now flags (grace-boundary persistence catches the grace-farming spoofer)
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 4, 4, 3, 1, 2, "decay keeps all but the oldest evidence across a grace boundary; arm re-fires after dipping below");
		// decay on an empty window is a no-op, same as clear
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(false), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 2, 1, 1, 0, 0, "decay on a zero window is a no-op");
		// decay drops the flag latch only when the surviving count falls below the level: flag re-fires, arm does not
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 4, 4, 3, 2, 1, "decay re-flags without re-arming while the arm level holds");
		// consecutive grace boundaries (double self-damage): two decays, arm drops only after the second, re-arms on the next claim
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 4, 4, 3, 1, 2, "two consecutive decays forgive two oldest claims");
		// K4 (reviewer FIX 2): the jar latches arm/flag, so agreement uses maxCount + presence, not edge counts.
		// The drained-window stream has TWO flag edges and TWO arm edges but ONE latched presence each.
		long[] k4 = GroundReplay.score(List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2);
		check(k4[3] == 2 && k4[4] == 2, "K4: edge counts stay available for tuning");
		check(k4[2] == 3 && k4[3] > 0 && k4[4] > 0, "K4 agreement: maxCount=3, flag presence + first-arm presence both PRESENT");
		long[] k4z = GroundReplay.score(List.of(GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false)), 5, 3, 2);
		check(k4z[2] == 0 && k4z[3] == 0 && k4z[4] == 0, "K4 agreement: a clean stream has maxCount 0 and both presences absent");
		// === D-ROW UNIT FIXTURES (grace-boundary persistence, patch 6) - hand-built SYNTHETIC streams: they pin policy semantics only. They are NOT the battery D-row evidence set and establish NO real legit FP safety; that evidence comes from the rig rows in rig/BATTERY-SHADOW-DIVERGENCE-PLAN.md ===
		// D1 grace-farm cheat stream: spoof, spoof, grace-clear, spoof, spoof.
		List<GroundReplay.Ev> d1 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true));
		long[] d1Live = GroundReplay.score(d1, 5, 3, 2), d1Decay = GroundReplay.score(d1, 5, 3, 2, true);
		// DIVERGENCE IS THE EVIDENCE: live policy misses (maxCount 2, flag absent), decay flags (count reaches 3)
		eq(d1Live, 4, 4, 2, 0, 2, "D1 live: grace-farming beats the full clear (the known hole)");
		eq(d1Decay, 4, 4, 3, 1, 2, "D1 decay: the same stream FLAGS - grace-boundary persistence catches the farmer");
		check(d1Live[3] == 0 && d1Decay[3] > 0, "D1: the live-vs-decay divergence on one synthetic stream (semantics pin, not battery evidence)");
		// D2 legit knockback landing: all supported claims around a grace clear - both policies spotless
		List<GroundReplay.Ev> d2 = List.of(
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.clear(),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false));
		eq(GroundReplay.score(d2, 5, 3, 2), 4, 0, 0, 0, 0, "D2 live: legit knockback landing is clean");
		eq(GroundReplay.score(d2, 5, 3, 2, true), 4, 0, 0, 0, 0, "D2 decay: legit knockback landing stays clean");
		// D3 legit medium dip with borderline claims around the exit: decay must NOT invent an FP -
		// maxCount stays 2 under both policies (flagAt=3): flag presence ABSENT on both; arm presence
		// = 1 on both (count reaches STRONG_ARM_AT=2 - the log-only shadow arm, not a verdict)
		List<GroundReplay.Ev> d3 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false));
		long[] d3Live = GroundReplay.score(d3, 5, 3, 2), d3Decay = GroundReplay.score(d3, 5, 3, 2, true);
		eq(d3Live, 5, 2, 2, 0, 1, "D3 live: borderline claims die at the clear");
		eq(d3Decay, 5, 2, 2, 0, 1, "D3 decay: forgiveness keeps the same sub-flag ceiling - no invented FP");
		// D4 double grace-farm (consecutive boundaries): decay forgives one claim per boundary, not the window
		List<GroundReplay.Ev> d4 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true),
			GroundReplay.Ev.clear(), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true));
		eq(GroundReplay.score(d4, 5, 3, 2, true), 4, 4, 3, 1, 2, "D4 decay: two boundaries forgive exactly two claims");
		// capture-format pins: the K4 receipt fields parse, and pre-0025 lines (no counts) still parse
		java.util.regex.Matcher mc = GroundReplay.CLAIM.matcher("[trace] gndclaim Bot air=true count=3");
		check(mc.find() && mc.group(1).equals("Bot") && mc.group(2).equals("true") && mc.group(3).equals("3"), "gndclaim with the jar count parses (K4 receipt)");
		java.util.regex.Matcher mc2 = GroundReplay.CLAIM.matcher("[trace] gndclaim Bot air=false");
		check(mc2.find() && mc2.group(3) == null, "pre-0025 gndclaim (no count) still parses");
		java.util.regex.Matcher mk = GroundReplay.CLEAR.matcher("[trace] gndclear Bot decayCount=2");
		check(mk.find() && mk.group(1).equals("Bot") && mk.group(2).equals("2"), "gndclear with the shadow decayCount parses");
		java.util.regex.Matcher mk2 = GroundReplay.CLEAR.matcher("[trace] gndclear Bot");
		check(mk2.find() && mk2.group(2) == null, "pre-0025 gndclear (no decayCount) still parses");
		java.util.regex.Matcher mr = GroundReplay.RESET.matcher("[trace] gndreset Bot cause=post-flag count=2 decayCount=4");
		check(mr.find() && mr.group(1).equals("Bot") && mr.group(2).equals("post-flag") && mr.group(3).equals("2") && mr.group(4).equals("4"), "gndreset with both pre-clear counts parses");
		// K4 tracker fixtures: mixed capture fails closed in BOTH orders; mismatch fails; clean passes; receiptless = NOT RUN (diagnostic, not a pass)
		GroundReplay.K4 k1 = new GroundReplay.K4();
		k1.claim("B", true, null); k1.claim("B", true, "2");
		check(k1.failed() && k1.mixed(), "K4 fails closed: uncounted-then-counted");
		GroundReplay.K4 k2 = new GroundReplay.K4();
		k2.claim("B", true, "1"); k2.claim("B", true, null);
		check(k2.failed() && k2.mixed(), "K4 fails closed: counted-then-uncounted");
		GroundReplay.K4 k2b = new GroundReplay.K4();
		k2b.clear("B", null); k2b.claim("B", true, "1");
		check(k2b.failed() && k2b.mixed(), "K4 fails closed: uncounted gndclear before counted gndclaim");
		GroundReplay.K4 k3 = new GroundReplay.K4();
		k3.claim("B", true, "1"); k3.claim("B", false, "1"); k3.claim("B", true, "2");
		check(!k3.failed() && !k3.notRun(), "K4 clean counted stream passes");
		GroundReplay.K4 k4b = new GroundReplay.K4();
		k4b.claim("B", true, "2");
		check(k4b.failed() && !k4b.mixed(), "K4 count mismatch fails (not mixed)");
		GroundReplay.K4 k5 = new GroundReplay.K4();
		k5.claim("B", true, null);
		check(!k5.failed() && k5.notRun(), "K4 receiptless = NOT RUN (diagnostic), not a fail");
		GroundReplay.K4 k6 = new GroundReplay.K4();
		k6.claim("B", true, "1"); k6.claim("B", true, "2"); k6.reset("B", "post-flag", "2", "2"); k6.claim("B", true, "1");
		check(!k6.failed(), "K4 reset receipts agree when both counts match");
		// D5/D6: a HARD reset (gndreset) wipes under BOTH policies; a soft boundary (gndclear) decays only under the candidate
		List<GroundReplay.Ev> d5 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.reset(),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true));
		eq(GroundReplay.score(d5, 5, 3, 2, true), 4, 4, 2, 0, 2, "D5 hard reset under decay: wiped - no flag, arm re-edges");
		eq(GroundReplay.score(d5, 5, 3, 2), 4, 4, 2, 0, 2, "D5 hard reset under live: identical");
		List<GroundReplay.Ev> d6 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true));
		eq(GroundReplay.score(d6, 5, 3, 2, true), 4, 4, 3, 1, 2, "D6 soft boundary under decay: forgives one - the same stream FLAGS");
		eq(GroundReplay.score(d6, 5, 3, 2), 4, 4, 2, 0, 2, "D6 soft boundary under live: full clear - sub-flag");
		// === K4 fail-closed NEGATIVE CONTROLS: the 0028 gate must actually fire, not just exist in text ===
		String javaBin = Path.of(System.getProperty("java.home"), "bin", "java").toString();
		String cp = System.getProperty("java.class.path");
		check(k4Run(javaBin, cp, "[trace] gndclaim Bot air=true count=7\n") == 1, "K4 negative control: a mismatched jar count exits 1 (harness computes 1, receipt claims 7)");
		check(k4last.contains("K4 FAILS CLOSED: jar-vs-harness state disagreement"), "K4 mismatch prints the disagreement line");
		check(k4Run(javaBin, cp, "[trace] gndclaim Bot air=true count=1\n[trace] gndclaim Bot air=true\n") == 1, "K4 negative control: a mixed counted/uncounted capture exits 1");
		check(k4last.contains("K4 FAILS CLOSED: counted and uncounted capture lines BOTH present"), "K4 mixed capture prints the fail-closed line (0029 both-order wording)");
		check(k4Run(javaBin, cp, "[trace] gndclaim Bot air=true\n") == 0, "receiptless capture exits 0 (diagnostic only, never a pass)");
		check(k4last.contains("K4 NOT RUN"), "receiptless capture is labeled K4 NOT RUN");
		check(k4Run(javaBin, cp, "[trace] gndclaim Bot air=true count=1\n[trace] gndclaim Bot air=true count=2\n") == 0, "positive control: consistent jar counts exit 0");
		check(!k4last.contains("FAILS CLOSED") && k4last.contains("2 claim receipts (0 mismatches)"), "positive control: agreement prints, no fail-closed");
		System.out.println(fails == 0 ? "GroundReplayTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
