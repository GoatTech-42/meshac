package dev.meshac.veil;

/** Which block changes can alter an anti-xray plan. Everything else (crops, doors, lamps, water, redstone) can skip the cache invalidation and the neighbour scan. Pure. */
public final class ChangeFilter {
	private ChangeFilter() {}
	/** A plan depends on three things per block: hidden list membership, see-through-ness, and being fake-roll stone. If none of them differ or matter, the plan is the same. */
	public static boolean affectsPlan(boolean oldHidden, boolean nowHidden, boolean oldTransparent, boolean nowTransparent, boolean oldStone, boolean nowStone) {
		return oldHidden || nowHidden || oldStone || nowStone || oldTransparent != nowTransparent;
	}
}
