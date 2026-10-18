package dev.meshac.veil;

import java.util.function.IntPredicate;

/**
 * Which block entity entries to drop from a chunk packet. No Minecraft types.
 * A hidden chest or spawner is swapped to stone in the section the client receives, but the packet's block entity list still carries its position and type.
 * The packet constructor fills that list by walking chunk.getBlockEntities() in order, one entry per block entity, so list index i is entry i of that walk.
 * If the two sizes ever differ (a block entity added during the walk, or a version that skips entries) the index match cannot be trusted and nothing is dropped.
 */
public final class BlockEntityStrip {
	private BlockEntityStrip() {}
	public static final int[] NONE = new int[0];

	/** Section-relative index of a block position, the same packing the veil plan uses (x | y << 4 | z << 8). */
	public static int packed(int x, int y, int z) { return (x & 15) | (y & 15) << 4 | (z & 15) << 8; }

	/** Indices to remove from the list, highest first so removing one does not move the next. Empty when the sizes differ or nothing is hidden. */
	public static int[] drops(int listSize, int walkSize, IntPredicate hiddenAt) {
		if (listSize != walkSize || listSize == 0) return NONE;
		int n = 0; int[] tmp = new int[listSize];
		for (int i = listSize - 1; i >= 0; i--) if (hiddenAt.test(i)) tmp[n++] = i;
		return n == 0 ? NONE : java.util.Arrays.copyOf(tmp, n);
	}

	/** True when the section's plan masks the block at this packed index and the real block has a block entity. */
	public static boolean masked(int[] planIdx, boolean[] realHasEntity, int packed) {
		for (int i = 0; i < planIdx.length; i++) if (planIdx[i] == packed) return realHasEntity[i];
		return false;
	}
 /** Per packet: only masks from a successful private copy AND a completed write can strip entries. */
 public static final class Encoded {
  private final int[][] masks; private final boolean[] written;
  public Encoded(int sections) { masks = new int[sections][]; written = new boolean[sections]; }
  public void copied(int section, int[] appliedEntities) { masks[section] = appliedEntities.clone(); written[section] = false; }
  public void wrote(int section) { if (section >= 0 && section < masks.length) written[section] = true; }
  public boolean hidden(int section, int packed) {
   if (section < 0 || section >= masks.length || !written[section] || masks[section] == null) return false;
   for (int x : masks[section]) if (x == packed) return true;
   return false;
  }
 }
 /** Build a replacement without mutating original. Failure at any point leaves original intact; caller swaps only on success. */
 public static <T> java.util.List<T> filtered(java.util.List<T> original, int walkSize, IntPredicate hiddenAt) {
  if (original.size() != walkSize) return original;
  java.util.List<T> replacement = new java.util.ArrayList<>(original.size());
  for (int i = 0; i < original.size(); i++) if (!hiddenAt.test(i)) replacement.add(original.get(i));
  return replacement;
 }
}
