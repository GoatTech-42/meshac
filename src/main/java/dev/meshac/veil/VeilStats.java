package dev.meshac.veil;

/** Counters for /mesh veil and a start-up self-check. Nothing is saved: a restart starts from zero. Pure, no Minecraft. Written from the server thread, read from command threads, so the fields are volatile and a read may be a tick stale. */
public final class VeilStats {
	public volatile long ticks, tickNanos, worstTickNanos, hides, shows, rays, budgetSkips, candidates, fallbackScans, lastTick = -1;
	private long winStart = -1, winHides, winShows; private volatile double hidesPerTick;

	/** One espTick finished. */
	public void tick(long serverTick, long nanos, int candidatesSeen, int raysUsed, int hidesNow, int showsNow, int skipped, boolean usedFallback) {
		ticks++; tickNanos += nanos; if (nanos > worstTickNanos) worstTickNanos = nanos;
		candidates += candidatesSeen; rays += raysUsed; hides += hidesNow; shows += showsNow; budgetSkips += skipped; if (usedFallback) fallbackScans++;
		lastTick = serverTick;
		if (winStart < 0) winStart = serverTick;
		winHides += hidesNow; winShows += showsNow;
		long span = serverTick - winStart;
		if (span >= 200) { hidesPerTick = span > 0 ? (double) winHides / span : 0; winStart = serverTick; winHides = 0; winShows = 0; }
	}
	/** Hides per server tick over the last full 200 tick window (10 s). Zero until one window has passed. */
	public double hidesPerTick() { return hidesPerTick; }
	public double avgTickMicros() { return ticks == 0 ? 0 : tickNanos / 1000.0 / ticks; }
	public void reset() { ticks = tickNanos = worstTickNanos = hides = shows = rays = budgetSkips = candidates = fallbackScans = 0; lastTick = -1; winStart = -1; winHides = winShows = 0; hidesPerTick = 0; }

	/** State of the tracked-set index, as shown to staff. */
	public static String indexState(boolean indexOk, long fallbackScans) { return indexOk ? (fallbackScans == 0 ? "tracked index ok" : "tracked index ok, " + fallbackScans + " fallback scans") : "tracked index OFF, full scan in use"; }

	public String line(int hiddenPairs, int pairsTracked, boolean indexOk) {
		return String.format(java.util.Locale.ROOT, "anti-ESP: %d hidden pairs of %d tracked, %.2f hides/tick (10 s), esp tick avg %.0f us worst %.0f us over %d ticks, %d rays, %d new culls skipped for budget, %s",
			hiddenPairs, pairsTracked, hidesPerTick, avgTickMicros(), worstTickNanos / 1000.0, ticks, rays, budgetSkips, indexState(indexOk, fallbackScans));
	}

	/** Start-up check of the pure parts with a tiny synthetic scene. Returns null when fine, otherwise a short reason. The caller turns the veil off on a reason: a veil that cannot pass this must not touch players. */
	public static String selfCheck() { return selfCheck(new EspCore.Params()); }
	/** Same check with explicit parameters, so a test can prove a bad setting is caught. */
	public static String selfCheck(EspCore.Params params) {
		try {
			EspCore esp = new EspCore(params);
			long t = 1000;
			EspCore.Action a = EspCore.Action.NONE;
			for (int i = 0; i < 3; i++) a = esp.update(1, 2, t += 4, 50, false, false);
			if (a != EspCore.Action.HIDE || !esp.hidden(1, 2) || !esp.anyHidden()) return "three failed checks did not hide a far entity";
			if (esp.update(1, 2, t += 4, 50, false, true) != EspCore.Action.SHOW || esp.anyHidden()) return "a visible entity was not shown again";
			for (int i = 0; i < 5; i++) if (esp.update(1, 3, t += 4, 4, false, false) == EspCore.Action.HIDE) return "an entity inside the near radius was hidden";
			for (int i = 0; i < 5; i++) if (esp.update(1, 4, t += 4, 50, true, false) == EspCore.Action.HIDE) return "an exempt entity was hidden";
			esp.attacked(1, 5, t += 4);
			for (int i = 0; i < 5; i++) if (esp.update(1, 5, t += 4, 50, false, false) == EspCore.Action.HIDE) return "an entity the viewer just attacked was hidden";
			esp.forgetViewer(1); if (esp.size() != 0 || esp.anyHidden()) return "forgetViewer left pairs behind";
			EntityGrid g = new EntityGrid(); g.clear();
			int i0 = g.add(10, 5, 5), i1 = g.add(11, -300, 40), i2 = g.add(12, 100, -100);
			int[] out = new int[8]; int n = g.near(0, 0, 136, out);
			java.util.Set<Integer> got = new java.util.HashSet<>(); for (int k = 0; k < n; k++) got.add(g.idAt(out[k]));
			if (!got.contains(10) || !got.contains(12)) return "grid lost an entity within range";
			if (got.contains(11)) return "grid returned an entity far out of range";
			if (i0 < 0 || i1 < 0 || i2 < 0) return "grid gave a bad index";
			g.clear(); if (g.near(0, 0, 136, out) != 0) return "grid kept entities after clear";
			return null;
		} catch (Throwable x) { return "self-check threw " + x; }
	}
}
