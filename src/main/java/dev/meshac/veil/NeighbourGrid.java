package dev.meshac.veil;

import java.util.Arrays;

/**
 * Memo for the block lookups that anti-xray makes while it plans one section: a 16x16x16 section plus a one block border (18x18x18). Every position is classified at most once,
 * so the neighbours that ore in a vein share are not looked up again. Positions outside the box go straight to the classifier, so a wrong box can only cost speed.
 * Pure: the adapter supplies the classifier. One instance per planning thread, reuse it with reset().
 */
public final class NeighbourGrid implements XrayCore.Blocks {
	public static final int HIDDEN = 1, TRANSPARENT = 2, STONE = 4;
	private static final int N = 18, KNOWN = 0x80;
	public interface Classifier { int classify(int x, int y, int z); } // bit set of HIDDEN, TRANSPARENT, STONE

	private final byte[] memo = new byte[N * N * N];
	private Classifier src; private int ox, oy, oz;
	public int lookups; // classifier calls since reset, for tests and profiling

	/** Box origin is the section's minimum corner; the border reaches one block beyond on each side. */
	public NeighbourGrid reset(int sectionMinX, int sectionMinY, int sectionMinZ, Classifier c) {
		Arrays.fill(memo, (byte) 0); src = c; ox = sectionMinX - 1; oy = sectionMinY - 1; oz = sectionMinZ - 1; lookups = 0; return this;
	}

	public int classify(int x, int y, int z) {
		int dx = x - ox, dy = y - oy, dz = z - oz;
		if (dx < 0 || dy < 0 || dz < 0 || dx >= N || dy >= N || dz >= N) { lookups++; return src.classify(x, y, z); }
		int i = (dy * N + dz) * N + dx; int m = memo[i];
		if (m != 0) return m & 7;
		lookups++; int v = src.classify(x, y, z) & 7; memo[i] = (byte) (v | KNOWN); return v;
	}
	@Override public boolean hidden(int x, int y, int z) { return (classify(x, y, z) & HIDDEN) != 0; }
	@Override public boolean transparent(int x, int y, int z) { return (classify(x, y, z) & TRANSPARENT) != 0; }
}
