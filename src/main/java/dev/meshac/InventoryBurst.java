package dev.meshac;

/**
 * The "too many item moves in 100 ms" rule as a pure class (no Minecraft types), so legit batches can be replayed in sim.
 * Vanilla client, read from AbstractContainerScreen in the 26.2 jar:
 *  - releasing a drag (quickCraftToSlots) sends one start packet, one packet per dragged slot and one end packet, all in one call: a drag over a full inventory is about 30 packets in one tick.
 *  - shift + double click sends a shift-click (QUICK_MOVE) for every slot that holds the same item, in one loop.
 * Neither is a script. So: drag packets are not counted, a run of shift-clicks on the same item counts once, and nothing counts for a moment after a packet backlog landed (Ticks.bunched).
 * What still counts: every other click, and every shift-click that changes item. A script that empties a chest of mixed loot in one tick is still over the limit.
 * Known cost: a chest holding one single item type, emptied in one tick by a script, now reads like a shift double click.
 */
public final class InventoryBurst {
	public static final int MAX_IN_WINDOW = 10;
	public static final long WINDOW_MS = 100;
	public enum Kind { QUICK_CRAFT, QUICK_MOVE, OTHER }

	/** Click kind text as ActionTrace.clickKind returns it (ContainerInput in 26.x, ClickType before). Anything unknown counts as an ordinary click. */
	public static Kind kind(String name) {
		if (name == null) return Kind.OTHER;
		switch (name) { case "QUICK_CRAFT": return Kind.QUICK_CRAFT; case "QUICK_MOVE": return Kind.QUICK_MOVE; default: return Kind.OTHER; }
	}

	private long start; private int count; private boolean lastMove; private int lastItem;

	/** @param item identity of the item in the clicked slot, or -1 when unknown (never joins a run). @return true when this click goes over the limit; the count restarts. */
	public boolean observe(long now, Kind k, int item, boolean bunched) {
		if (k == Kind.QUICK_CRAFT) return false;
		if (bunched) { start = 0; count = 0; lastMove = false; return false; }
		if (now - start > WINDOW_MS) { start = now; count = 0; lastMove = false; }
		boolean sameRun = k == Kind.QUICK_MOVE && lastMove && item != -1 && item == lastItem;
		lastMove = k == Kind.QUICK_MOVE; lastItem = item;
		if (sameRun) return false;
		if (++count > MAX_IN_WINDOW) { count = 0; return true; }
		return false;
	}
	public void reset() { start = 0; count = 0; lastMove = false; }
}
