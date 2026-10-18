package dev.meshac.veil;

/**
 * Leak 1 (no-update change next to a chunk border). A planned block on the border depends on one block in the chunk next door. The plan stores, per border block, which of those
 * outward neighbours were see-through; a send re-reads only those few and replans when one differs. Cost: one lookup per border block per send, bounded by 4 faces. Pure.
 */
public final class BorderGuard {
	private BorderGuard() {}
	public static final int WEST = 1, EAST = 2, NORTH = 4, SOUTH = 8;
	/** Probe for the block just across the border: dx -1 west, +1 east; dz -1 north, +1 south. */
	public interface Probe { boolean transparentAcross(int dx, int dz); }

	/** idx = x | y << 4 | z << 8, the same packing as Veil.Mod. */
	public static int faces(int idx) {
		int x = idx & 15, z = (idx >> 8) & 15, f = 0;
		if (x == 0) f |= WEST; if (x == 15) f |= EAST; if (z == 0) f |= NORTH; if (z == 15) f |= SOUTH;
		return f;
	}
	public static boolean isBorder(int idx) { return faces(idx) != 0; }
	/** Bit set for each touched face whose outward neighbour is see-through now. */
	public static int bits(int idx, Probe p) {
		int f = faces(idx), b = 0;
		if ((f & WEST) != 0 && p.transparentAcross(-1, 0)) b |= WEST;
		if ((f & EAST) != 0 && p.transparentAcross(1, 0)) b |= EAST;
		if ((f & NORTH) != 0 && p.transparentAcross(0, -1)) b |= NORTH;
		if ((f & SOUTH) != 0 && p.transparentAcross(0, 1)) b |= SOUTH;
		return b;
	}
	/** True while the stored bits still match: interior blocks always do, no lookup made. */
	public static boolean unchanged(int idx, int storedBits, Probe p) { return !isBorder(idx) || bits(idx, p) == storedBits; }

	/** Probe for the planned entry at position i of the plan's border list. */
	public interface IndexedProbe { boolean transparentAcross(int i, int dx, int dz); }

	/** Bits for every entry of idx, for storing in the plan. */
	public static int[] compute(int[] idx, IndexedProbe p) {
		int[] out = new int[idx.length];
		for (int i = 0; i < idx.length; i++) { final int k = i; out[i] = bits(idx[i], (dx, dz) -> p.transparentAcross(k, dx, dz)); }
		return out;
	}
	/** True while every stored entry still matches. Stops at the first difference. */
	public static boolean allUnchanged(int[] idx, int[] stored, IndexedProbe p) {
		for (int i = 0; i < idx.length; i++) { final int k = i; if (bits(idx[i], (dx, dz) -> p.transparentAcross(k, dx, dz)) != stored[i]) return false; }
		return true;
	}
}
