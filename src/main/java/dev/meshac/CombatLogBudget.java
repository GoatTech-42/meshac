package dev.meshac;

/** Per-player fixed monotonic minute windows. No timers, threads or unbounded keys. */
public final class CombatLogBudget {
	public enum Kind { EVIDENCE, SUMMARY, TRACE }
	private final long[] start = new long[3], suppressed = new long[3];
	private final int[] used = new int[3];
	private final boolean[] initialized = new boolean[3];
	private static final int[] LIMIT = {12, 6, 120};
	/** Rig-only: complete unsampled trace captures for offline analysis. Never set in production; EVIDENCE and SUMMARY stay capped regardless. */
	private static final boolean FULL_TRACE = System.getenv("MESHAC_TRACE_FULL") != null;
	public boolean allow(Kind kind, long ns) {
		int i = kind.ordinal();
		if (i == 2 && FULL_TRACE) return true;
		if (!initialized[i] || ns < start[i] || ns - start[i] >= 60_000_000_000L) {
			initialized[i] = true; start[i] = ns; used[i] = 0;
		}
		if (used[i] >= LIMIT[i]) { if (suppressed[i] != Long.MAX_VALUE) suppressed[i]++; return false; }
		used[i]++; return true;
	}
	/** Next permitted line can disclose dropped observations without an extra log line. */
	public long takeSuppressed(Kind kind) { int i=kind.ordinal();long n=suppressed[i];suppressed[i]=0;return n; }
}
