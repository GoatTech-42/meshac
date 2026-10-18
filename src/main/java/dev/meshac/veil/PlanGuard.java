package dev.meshac.veil;

import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/** Order and cost of the checks that decide whether a cached anti-xray plan may be used. Cheapest first, each later check only runs when the earlier ones pass. Pure. */
public final class PlanGuard {
	private PlanGuard() {}
	public static final int OK = 0, OWN_STALE = 1, NEIGHBOUR_LOADED = 2, BORDER_CHANGED = 3;

	/**
	 * @param ownFresh   the planned blocks of this section still hold what the plan expects (already existed)
	 * @param storedMask NeighbourMask at plan time; 15 means all four were loaded, so the mask check is skipped without a lookup
	 * @param nowMask    computes the mask now (4 chunk lookups), called only when storedMask != 15
	 * @param borderOk   the outward neighbours of planned border blocks are unchanged (BorderGuard), called last
	 */
	public static int check(BooleanSupplier ownFresh, int storedMask, IntSupplier nowMask, BooleanSupplier borderOk) {
		if (!ownFresh.getAsBoolean()) return OWN_STALE;
		if (storedMask != 15 && NeighbourMask.needsReplan(storedMask, nowMask.getAsInt())) return NEIGHBOUR_LOADED;
		if (!borderOk.getAsBoolean()) return BORDER_CHANGED;
		return OK;
	}
}
