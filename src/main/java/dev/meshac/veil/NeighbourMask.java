package dev.meshac.veil;

/**
 * Leak 2 (a chunk planned before its neighbour loaded). The plan stores which of the four side neighbours were loaded; a send recomputes that mask and replans only when a
 * neighbour has newly loaded. Each plan can therefore be rebuilt at most four times for this reason. Pure.
 */
public final class NeighbourMask {
	private NeighbourMask() {}
	public static final int WEST = 1, EAST = 2, NORTH = 4, SOUTH = 8;
	public static int of(boolean west, boolean east, boolean north, boolean south) { return (west ? WEST : 0) | (east ? EAST : 0) | (north ? NORTH : 0) | (south ? SOUTH : 0); }
	/** A neighbour that was missing at plan time is loaded now. An unload does not trigger a rebuild: the plan stays valid and nothing is gained by thrashing. */
	public static boolean needsReplan(int stored, int now) { return (now & ~stored) != 0; }
	/** What to store after a replan: exactly the current mask. */
	public static int stored(int now) { return now; }
}
