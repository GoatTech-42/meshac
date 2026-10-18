package dev.meshac.veil;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Notices a viewer the veil has not seen before (join, or the new entity after a respawn) or one that is now in a different level (dimension change), from the tick loop alone.
 * No Fabric event is needed: the event for a level change is named differently in 1.21.11 and 26.x. Pure.
 */
public final class ViewerEpoch {
	private final Map<Integer, Integer> level = new ConcurrentHashMap<>();
	/** True when a new no-cull window should start for this viewer. */
	public boolean observe(int viewer, int levelKey) {
		Integer old = level.put(viewer, levelKey);
		return old == null || old != levelKey;
	}
	public void forget(int viewer) { level.remove(viewer); }
	public int size() { return level.size(); }
	/** Drop viewers that are no longer online (respawned entities, left players). */
	public int retainOnly(Set<Integer> present) { int n = 0; for (Integer k : level.keySet()) if (!present.contains(k)) { level.remove(k); n++; } return n; }
}
