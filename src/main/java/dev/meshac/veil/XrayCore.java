package dev.meshac.veil;

import java.util.SplittableRandom;

/**
 * Pure logic of chunkveil (no Minecraft types, so it can be unit tested with plain javac).
 * The Minecraft adapter (VeilAdapter) feeds it ids and neighbour data.
 * Decision per block in an outgoing section: keep the real state, or send a replacement.
 */
public final class XrayCore {
	/** What the adapter knows about one position. */
	public interface Blocks {
		boolean hidden(int x, int y, int z);      // is a hidden-list block (ore, chest, spawner)
		boolean transparent(int x, int y, int z); // air, fluid, glass, any non-full or non-opaque block, or unloaded neighbour
	}

	/** Exposed = at least one of the 6 neighbours is transparent. An unloaded neighbour counts as transparent (fail open: never hide something that could be visible). */
	public static boolean exposed(Blocks b, int x, int y, int z) {
		return b.transparent(x + 1, y, z) || b.transparent(x - 1, y, z) || b.transparent(x, y + 1, z)
			|| b.transparent(x, y - 1, z) || b.transparent(x, y, z + 1) || b.transparent(x, y, z - 1);
	}

	/** Hide when the block is in the hidden list and buried. */
	public static boolean shouldHide(Blocks b, int x, int y, int z) {
		return b.hidden(x, y, z) && !exposed(b, x, y, z);
	}

	/**
	 * Should a buried plain-stone position be shown as a fake ore? Deterministic in (seed, position) so every player and every re-send sees the same fake
	 * (a differing fake would reveal itself by flickering). ratio is the fraction of buried stone turned into fake ore, 0..1.
	 */
	public static boolean fakeOreAt(long worldSeedSalt, int x, int y, int z, double ratio) {
		if (ratio <= 0) return false;
		long h = worldSeedSalt ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
		return new SplittableRandom(h).nextDouble() < ratio;
	}

	/** The 6 neighbour offsets that must be re-sent when a block changes (only those that are hidden and now exposed matter, the adapter filters). */
	public static final int[][] NEIGHBOURS = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
}
