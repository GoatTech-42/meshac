import java.nio.file.*;
import java.util.*;

/** Source witness for the monitor invariant in Movement: every setback return, every hold/NoFall arming and the fall damage sit behind the monitor gate, and entry/tick clear all three pending fields together. A real-class fixture needs a live server; this guards the structure until the rig runs it. */
public final class MonitorSourceWitnessTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) throws Exception {
		List<String> L = Files.readAllLines(Paths.get("src/main/java/dev/meshac/Movement.java"));
		int returns = 0;
		for (int i = 0; i < L.size(); i++) {
			String l = L.get(i);
			if (l.contains("return new double[]")) {
				returns++;
				boolean gated = false;
				for (int k = Math.max(0, i - 25); k <= i; k++) { String p = L.get(k); if (p.contains("acts(") || p.contains("holdReplays(")) gated = true; }
				check(gated, "setback return at line " + (i + 1) + " has no monitor gate within 25 lines");
			}
			if (l.contains("s.freezeUntil = System.currentTimeMillis()")) check(l.contains("s.freezeCheck") , "hold armed without recording its check, line " + (i + 1));
			if (l.contains("s.nfAt = System.currentTimeMillis()")) check(l.contains("armNoFall("), "nfAt armed without MonitorGate.armNoFall, line " + (i + 1));
			if (l.contains("causeFallDamage")) check(l.contains("MonitorGate.damage("), "causeFallDamage without MonitorGate.damage, line " + (i + 1));
		}
		check(returns >= 5, "expected the setback return sites, found " + returns);
		String t = String.join("\n", L);
		check(t.contains("private static void monitorClear(S s) { if (Config.get().monitor()) { s.freezeUntil = 0; s.nfAt = 0; s.fdMax = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; } }"), "monitorClear clears all pending nofall state together (incl. the P3-strong shadow)");
		int c = t.indexOf("public static double[] check("), tk = t.indexOf("public static void tick(");
		check(c > 0 && t.indexOf("monitorClear(s);", c) > 0 && t.indexOf("monitorClear(s);", c) < c + 700, "check() clears pending actions at entry");
		check(tk > 0 && t.indexOf("monitorClear(s);", tk) > 0 && t.indexOf("monitorClear(s);", tk) < tk + 700, "tick() clears pending actions at entry");
		check(t.contains("clearPendingActions() { for (S s : STATE.values()) { s.freezeUntil = 0; s.nfAt = 0; s.fdMax = 0; s.nfStrongAt = 0; s.fdStrongMax = 0; } }"), "clearPendingActions clears all pending nofall state (incl. the P3-strong shadow: a stale shadow arm would emit a spurious WOULD-fire and K2 reads any legit WOULD-fire as a kill)");
  String cfg = Files.readString(Paths.get("src/main/java/dev/meshac/Config.java"));
  // 0095: anchors refreshed for the 0091-0094 commit-lock shape (publish is "cur = new Snapshot(", not "cur = next;"; the file-absent branch captures its copy inside the lock). The INVARIANTS pinned are unchanged.
  int parse = cfg.indexOf("public static AdminLog.ReloadResult load()"), commit = cfg.indexOf("cur = new Snapshot(", parse), persist = cfg.indexOf("Files.writeString(p, output)", parse);
  int lock = cfg.indexOf("synchronized (POLICY_LOCK)", parse), wipe = cfg.indexOf("Movement.clearPendingActions(); Verdict.clearPendingActions();", parse);
  check(parse >= 0 && commit > parse && persist > commit, "reload separates config application from persistence");
  check(lock > parse && wipe > lock && wipe < commit, "reload clears pending before active swap on both file branches (inside the commit lock, before publish)");
  int copy = cfg.indexOf("if (!existed) { next = g.fromJson(g.toJson(cur.cfg), Config.class); output = documented(g, next); }", parse);
  check(copy > lock && copy < wipe, "file-absent branch rejoins the common pending-clear path (copy captured under the commit lock, before the wipe)");
  check(cfg.indexOf("return AdminLog.ReloadResult.PARSE_FAILED;",parse) < commit, "parse rejection precedes active state change");
  check(cfg.indexOf("return AdminLog.ReloadResult.APPLIED_NOT_SAVED;",persist) > persist, "persistence failure has distinct result after apply");
		System.out.println(fails == 0 ? "MonitorSourceWitnessTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
