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
 * Usage: java -cp <suite-out> GroundReplay <trace-log>...
 * Columns: window flagAt armAt | claims unsupported maxCount flags strongArms | players with >=1 strong-arm. */
public final class GroundReplay {
	/** One event: claim (unsupported?) or clear. */
	public static final class Ev { final boolean claim, air; Ev(boolean claim, boolean air) { this.claim = claim; this.air = air; }
		public static Ev claim(boolean air) { return new Ev(true, air); } public static Ev clear() { return new Ev(false, false); } }

	/** Replay one event stream. Returns {claims, unsupported, maxCount, flagRisingEdges, armRisingEdges}. */
	public static long[] score(List<Ev> events, int window, int flagAt, int armAt) {
		GroundEvidence ev = new GroundEvidence(window, flagAt);
		long claims = 0, un = 0, max = 0, flags = 0, arms = 0;
		boolean wasFlag = false, wasArm = false;
		for (Ev e : events) {
			if (!e.claim) { ev.clear(); wasFlag = false; wasArm = false; continue; }
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
		System.out.println("window flagAt armAt | claims unsupported maxCount flags strongArms | players with >=1 strong-arm");
		for (int w = 3; w <= 7; w++) for (int t = 2; t <= Math.min(w, 4); t++) for (int a = 2; a <= t; a++) {
			long[] tot = new long[5];
			List<String> armed = new ArrayList<>();
			for (var e : per.entrySet()) {
				long[] r = score(e.getValue(), w, t, a);
				for (int i = 0; i < 5; i++) tot[i] += r[i];
				tot[2] -= r[2]; if (r[2] > tot[2]) tot[2] = r[2]; // maxCount is a max across players, not a sum
				if (r[4] > 0) armed.add(e.getKey() + "(" + r[4] + ")");
			}
			System.out.printf("%d %d %d | %d %d %d %d %d | %s%n", w, t, a, tot[0], tot[1], tot[2], tot[3], tot[4], armed.isEmpty() ? "-" : String.join(",", armed));
		}
	}
}
