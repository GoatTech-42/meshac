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
 * live window's buf=, not the raw claim stream. Setback/flag/teleport clears are not captured (rare);
 * scores those spans as if the window lived on - an over-count, i.e. conservative for FP reading.
 *
 * Two policy tables: the live policy (full clear at every captured gndclear) and the decay-on-clear
 * candidate (P3-FOLLOWUPS "grace-boundary persistence": a grace/exempt reset forgives only the OLDEST
 * unsupported claim instead of clearing, so a spoofer that re-arms grace by self-damage or medium dips
 * keeps most of its evidence). Decay is scored through the real GroundEvidence.decay(); the live jar
 * never calls it - this table is the measurement stage, not a shipped behavior. Captured clears do not
 * say which kind they were (exempt vs grace), so decay is applied at every captured clear; on legit
 * rows that retains more evidence than the real policy would, i.e. conservative for FP reading.
 *
 * Usage: java -cp <suite-out> GroundReplay <trace-log>...
 * Columns: window flagAt armAt | claims unsupported maxCount flags strongArms | players with >=1 strong-arm.
 * The final section prints the K4 agreement metrics (reviewer FIX 2): the jar latches arm/flag, so agreement with
 * jar-captured state is per-player maxCount equality + flag presence/absence + first-arm presence - edge counts
 * are tuning signal only, never the agreement metric. */
public final class GroundReplay {
	/** One event: claim (unsupported?) or clear. */
	public static final class Ev { final boolean claim, air; Ev(boolean claim, boolean air) { this.claim = claim; this.air = air; }
		public static Ev claim(boolean air) { return new Ev(true, air); } public static Ev clear() { return new Ev(false, false); } }

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
				if (decayOnClear) { ev.decay(); wasFlag = ev.flagged(); wasArm = ev.unsupported() >= armAt; }
				else { ev.clear(); wasFlag = false; wasArm = false; }
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

	private static final Pattern CLAIM = Pattern.compile("\\[trace] gndclaim (\\S+) air=(true|false)");
	private static final Pattern CLEAR = Pattern.compile("\\[trace] gndclear (\\S+)");

	public static void main(String[] args) throws Exception {
		if (args.length == 0) { System.err.println("usage: GroundReplay <trace-log>..."); System.exit(2); }
		TreeMap<String, List<Ev>> per = new TreeMap<>();
		for (String f : args) for (String ln : Files.readAllLines(Path.of(f))) {
			Matcher m = CLAIM.matcher(ln);
			if (m.find()) { per.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(Ev.claim(Boolean.parseBoolean(m.group(2)))); continue; }
			m = CLEAR.matcher(ln);
			if (m.find()) per.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(Ev.clear());
		}
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
