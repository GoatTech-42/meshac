package dev.meshac.veil;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Per viewer "do not cull yet" windows: after join, respawn, dimension change. Pure. A tick counter that went backwards (new server in the same JVM) counts as expired. */
public final class GraceTable {
	private final Map<Integer, Long> since = new ConcurrentHashMap<>();
	public void start(int viewer, long tick) { since.put(viewer, tick); }
	public void forget(int viewer) { since.remove(viewer); }
	public int size() { return since.size(); }
	public boolean active(int viewer, long tick, long window) { Long j = since.get(viewer); return j != null && !expired(j, tick, window); }
	private static boolean expired(long start, long tick, long window) { return tick < start || tick - start >= window; }
	/** Drop finished windows so entries of players that died or left do not pile up. */
	public int sweep(long tick, long window) {
		int n = 0; for (Iterator<Map.Entry<Integer, Long>> it = since.entrySet().iterator(); it.hasNext();) { if (expired(it.next().getValue(), tick, window)) { it.remove(); n++; } }
		return n;
	}
}
