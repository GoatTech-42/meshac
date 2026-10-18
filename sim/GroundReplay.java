import dev.meshac.GroundEvidence;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline scorer for rig ground-claim traces. Replays the capture lines emitted by the patched jar
 * ("[trace] gndclaim <name> air=<bool>" per scored claim, "[trace] gndclear <name>" per exempt/grace
 * window reset) through the REAL GroundEvidence for every candidate (window, flagAt, armAt) and prints
 * a score table, so the window/threshold and the P3-strong arm level are chosen from battery traces
 * without re-running the rig. Pre-patch logs cannot be rescored: old groundctx lines only carry the
 * live window's buf=, not the raw claim stream. Full resets (teleport/soft-return/post-flag) ARE
 * captured as gndreset lines with both windows' pre-clear counts, and replay as exact full clears in
 * BOTH policy tables - a soft gndclear decays under the candidate, a hard gndreset wipes.
 *
 * Two policy tables: the live policy (full clear at every captured gndclear) and the decay-on-clear
 * candidate (P3-FOLLOWUPS "grace-boundary persistence": a grace/exempt reset forgives only the OLDEST
 * unsupported claim instead of clearing, so a spoofer that re-arms grace by self-damage or medium dips
 * keeps most of its evidence). Decay is scored through the real GroundEvidence.decay(); the live jar
 * never calls it - this table is the measurement stage, not a shipped behavior. Soft boundaries
 * (gndclear) decay under the candidate; hard resets (gndreset) wipe under BOTH policies, exactly as
 * the jar behaves - no uncaptured-clear conservatism remains in captured spans.
 *
 * Usage: java -cp <suite-out> GroundReplay <trace-log>...
 * Columns: window flagAt armAt | claims unsupported maxCount flags strongArms | players with >=1 strong-arm.
 * The final section prints the K4 agreement metrics (reviewer FIX 2): the jar latches arm/flag, so agreement with
 * jar-captured state is per-player maxCount equality + flag presence/absence + first-arm presence - edge counts
 * are tuning signal only, never the agreement metric. When the capture carries the jar's own counts (gndclaim
 * count= post-add live count; gndclear decayCount= pre-decay shadow count; gndreset both counts),
 * the parse loop ALSO replays every receipt through real GroundEvidence instances and reports per-receipt
 * mismatches - a captured jar-state comparison, not harness-vs-harness. It FAILS CLOSED (exit 1) on any
 * mismatch or a mixed counted/uncounted capture IN EITHER ORDER; with zero receipts it says K4 NOT RUN
 * and exits 0 - that rc0 is DIAGNOSTIC, not a K4 pass: consumers must check for the NOT RUN line. */
public final class GroundReplay {
	/** One event: claim (unsupported?) or clear. */
	public static final class Ev { final boolean claim, air, hard; Ev(boolean claim, boolean air, boolean hard) { this.claim = claim; this.air = air; this.hard = hard; }
		public static Ev claim(boolean air) { return new Ev(true, air, false); } public static Ev clear() { return new Ev(false, false, false); } public static Ev reset() { return new Ev(false, false, true); } }

	/** Replay one event stream under the live clear policy. Returns {claims, unsupported, maxCount, flagRisingEdges, armRisingEdges}. */
	public static long[] score(List<Ev> events, int window, int flagAt, int armAt) { return score(events, window, flagAt, armAt, false); }

	/** Replay one event stream. decayOnClear=true scores the grace-boundary persistence candidate: a captured
	 * clear decays the window by its oldest unsupported claim instead of clearing it, and the flag/arm latches
	 * only drop when the surviving count actually falls below their level. */
	public static long[] score(List<Ev> events, int window, int flagAt, int armAt, boolean decayOnClear) {
		GroundEvidence ev = new GroundEvidence(window, flagAt);
		long claims = 0, un = 0, max = 0, flags = 0, arms = 0;
		boolean wasFlag = false, wasArm = false;
		for (Ev e : events) {
			if (!e.claim) {
				if (e.hard || !decayOnClear) { ev.clear(); wasFlag = false; wasArm = false; } // gndreset: both policies wipe
				else { ev.decay(); wasFlag = ev.flagged(); wasArm = ev.unsupported() >= armAt; } // gndclear: the candidate forgives one
				continue;
			}
			ev.add(e.air); claims++; if (e.air) un++;
			int c = ev.unsupported();
			if (c > max) max = c;
			boolean f = ev.flagged(), a = c >= armAt;
			if (f && !wasFlag) flags++;
			if (a && !wasArm) arms++;
			wasFlag = f; wasArm = a;
		}
		return new long[] {claims, un, max, flags, arms};
	}

	static final Pattern CLAIM = Pattern.compile("\\[trace] gndclaim (\\S+) air=(true|false)(?: count=(\\d+))?");
	static final Pattern CLEAR = Pattern.compile("\\[trace] gndclear (\\S+)(?: decayCount=(\\d+))?");
	static final Pattern RESET = Pattern.compile("\\[trace] gndreset (\\S+) cause=(\\S+) count=(\\d+) decayCount=(\\d+)");

	/** K4 receipt tracker: replays capture receipts through REAL GroundEvidence instances (live policy +
	 *  decay shadow) and fails closed on any mismatch or a mixed counted/uncounted capture in EITHER order.
	 *  Package-visible for GroundReplayTest. */
	static final class K4 {
		final TreeMap<String, GroundEvidence> liveJar = new TreeMap<>(), decayJar = new TreeMap<>();
		int claimReceipts, claimMismatch, clearReceipts, clearMismatch;
		boolean receiptsSeen, uncountedSeen;
		final List<String> lines = new ArrayList<>();
		void claim(String name, boolean air, String count) {
			GroundEvidence g = liveJar.computeIfAbsent(name, k -> new GroundEvidence(5, 3));
			GroundEvidence d = decayJar.computeIfAbsent(name, k -> new GroundEvidence(5, 3));
			g.add(air); d.add(air);
			if (count == null) { uncountedSeen = true; return; } // count = the jar's live window AFTER the add
			receiptsSeen = true; claimReceipts++;
			if (g.unsupported() != Integer.parseInt(count)) { claimMismatch++; lines.add("K4 claim mismatch " + name + ": jar=" + count + " harness=" + g.unsupported()); }
		}
		void clear(String name, String decayCount) {
			GroundEvidence g = liveJar.get(name); if (g != null) g.clear();
			GroundEvidence d = decayJar.computeIfAbsent(name, k -> new GroundEvidence(5, 3));
			if (decayCount == null) { uncountedSeen = true; d.decay(); return; } // decayCount = the jar's shadow BEFORE its decay
			receiptsSeen = true; clearReceipts++;
			if (d.unsupported() != Integer.parseInt(decayCount)) { clearMismatch++; lines.add("K4 boundary mismatch " + name + ": jarDecay=" + decayCount + " harness=" + d.unsupported()); }
			d.decay();
		}
		void reset(String name, String cause, String count, String decayCount) {
			receiptsSeen = true; clearReceipts++;
			GroundEvidence g = liveJar.computeIfAbsent(name, k -> new GroundEvidence(5, 3));
			GroundEvidence d = decayJar.computeIfAbsent(name, k -> new GroundEvidence(5, 3));
			if (g.unsupported() != Integer.parseInt(count)) { claimMismatch++; lines.add("K4 reset mismatch " + name + " (" + cause + "): jar=" + count + " harness=" + g.unsupported()); }
			if (d.unsupported() != Integer.parseInt(decayCount)) { clearMismatch++; lines.add("K4 reset mismatch " + name + " (" + cause + "): jarDecay=" + decayCount + " harness=" + d.unsupported()); }
			g.clear(); d.clear();
		}
		boolean mixed() { return receiptsSeen && uncountedSeen; } // BOTH categories, either order
		boolean failed() { return mixed() || claimMismatch + clearMismatch > 0; }
		boolean notRun() { return !receiptsSeen; }
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0) { System.err.println("usage: GroundReplay <trace-log>..."); System.exit(2); }
		TreeMap<String, List<Ev>> per = new TreeMap<>();
		K4 k4 = new K4();
		for (String f : args) for (String ln : Files.readAllLines(Path.of(f))) {
			Matcher m = CLAIM.matcher(ln);
			if (m.find()) {
				String name = m.group(1); boolean air = Boolean.parseBoolean(m.group(2));
				per.computeIfAbsent(name, k -> new ArrayList<>()).add(Ev.claim(air));
				k4.claim(name, air, m.group(3));
				continue;
			}
			m = CLEAR.matcher(ln);
			if (m.find()) {
				String name = m.group(1);
				per.computeIfAbsent(name, k -> new ArrayList<>()).add(Ev.clear());
				k4.clear(name, m.group(2));
				continue;
			}
			m = RESET.matcher(ln);
			if (m.find()) { // a captured full reset: EXACT semantics - both harness windows wipe, like the jar
				String name = m.group(1);
				per.computeIfAbsent(name, k -> new ArrayList<>()).add(Ev.reset());
				k4.reset(name, m.group(2), m.group(3), m.group(4));
			}
		}
		System.out.println("K4 jar-state comparison: " + k4.claimReceipts + " claim receipts (" + k4.claimMismatch + " mismatches), " + k4.clearReceipts + " boundary/reset receipts (" + k4.clearMismatch + " mismatches)");
		for (String l : k4.lines) System.out.println("  " + l);
		if (k4.mixed()) { System.out.println("K4 FAILS CLOSED: counted and uncounted capture lines BOTH present (either order - concatenated jars?) - no agreement reading"); System.exit(1); }
		if (k4.claimMismatch + k4.clearMismatch > 0) { System.out.println("K4 FAILS CLOSED: jar-vs-harness state disagreement - no agreement reading"); System.exit(1); }
		if (k4.notRun()) System.out.println("K4 NOT RUN: no jar counts in capture - presence-semantics reading only; exit code 0 here is DIAGNOSTIC, NOT a K4 pass - consumers must check for this line, not rc0");
		if (per.isEmpty()) { System.out.println("no gndclaim lines found (jar older than the P3-strong draft? capture ships with it)"); return; }
		System.out.println("players: " + per.keySet());
		printTable(per, false);
		System.out.println();
		System.out.println("policy=decay-on-clear (grace-boundary persistence candidate, P3-FOLLOWUPS; measurement only - the live jar still full-clears):");
		printTable(per, true);
		System.out.println();
		System.out.println("K4 agreement metrics at the live jar config (window=5 flagAt=3 armAt=2 = Movement.java GROUND_WINDOW/GROUND_FLAG_AT/STRONG_ARM_AT):");
		System.out.println("the jar LATCHES (nfAt/nfStrongAt stay set until a reset), so harness-vs-jar agreement is per-player maxCount equality + flag");
		System.out.println("presence/absence + first-arm presence. The flag/arm edge counts in the tables above are window-tuning signal only (reviewer FIX 2).");
		System.out.println("player | claims unsupported maxCount | flag first-arm");
		for (var e : per.entrySet()) {
			long[] r = score(e.getValue(), 5, 3, 2);
			System.out.printf("%s | %d %d %d | %s %s%n", e.getKey(), r[0], r[1], r[2], r[3] > 0 ? "PRESENT" : "absent", r[4] > 0 ? "PRESENT" : "absent");
		}
	}

	private static void printTable(TreeMap<String, List<Ev>> per, boolean decayOnClear) {
		System.out.println("window flagAt armAt | claims unsupported maxCount flags strongArms | players with >=1 strong-arm");
		for (int w = 3; w <= 7; w++) for (int t = 2; t <= Math.min(w, 4); t++) for (int a = 2; a <= t; a++) {
			long[] tot = new long[5];
			List<String> armed = new ArrayList<>();
			for (var e : per.entrySet()) {
				long[] r = score(e.getValue(), w, t, a, decayOnClear);
				for (int i = 0; i < 5; i++) tot[i] += r[i];
				tot[2] -= r[2]; if (r[2] > tot[2]) tot[2] = r[2]; // maxCount is a max across players, not a sum
				if (r[4] > 0) armed.add(e.getKey() + "(" + r[4] + ")");
			}
			System.out.printf("%d %d %d | %d %d %d %d %d | %s%n", w, t, a, tot[0], tot[1], tot[2], tot[3], tot[4], armed.isEmpty() ? "-" : String.join(",", armed));
		}
	}
}
