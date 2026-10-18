package dev.meshac.veil;

/**
 * Pure spatial index for the anti-ESP candidate search (no Minecraft types). Replaces "every loaded entity for every viewer" with "entities in the cells within radius".
 * Entities go into 16x16 block columns (cell = floor(x) >> 4, floor(z) >> 4; y is ignored, the caller does the exact 3D test). Rebuilt each tick from the tracked set:
 * clear(), add() per entity, near() per viewer. Reuses its arrays, so a rebuild allocates nothing once warm.
 * Positions are a pre-filter only: callers must keep a margin for movement since the build and re-check the live entity.
 */
public final class EntityGrid {
	private int[] ids = new int[1024], next = new int[1024]; private double[] xs = new double[1024], zs = new double[1024];
	private int n;
	private long[] cellKeys = new long[2048]; private int[] cellHead = new int[2048]; private int[] cellGen = new int[2048]; private int gen = 1, cells;

	private static long cell(int cx, int cz) { return ((long) cx << 32) ^ (cz & 0xFFFFFFFFL); }
	private static int mix(long k) { k ^= k >>> 33; k *= 0xff51afd7ed558ccdL; k ^= k >>> 33; return (int) k; }

	public int size() { return n; }
	public int idAt(int idx) { return ids[idx]; }

	public void clear() { n = 0; cells = 0; if (++gen == Integer.MAX_VALUE) { java.util.Arrays.fill(cellGen, 0); gen = 1; } }

	/** Adds an entity and returns its index (stable until clear()). */
	public int add(int id, double x, double z) {
		if (n == ids.length) grow();
		int idx = n++; ids[idx] = id; xs[idx] = x; zs[idx] = z;
		long key = cell((int) Math.floor(x) >> 4, (int) Math.floor(z) >> 4);
		int m = cellKeys.length - 1, i = mix(key) & m;
		while (cellGen[i] == gen && cellKeys[i] != key) i = (i + 1) & m;
		if (cellGen[i] != gen) {
			if ((cells + 1) * 2 > cellKeys.length) { growCells(); return reinsertLast(idx, key); }
			cellGen[i] = gen; cellKeys[i] = key; cellHead[i] = -1; cells++;
		}
		next[idx] = cellHead[i]; cellHead[i] = idx;
		return idx;
	}
	private int reinsertLast(int idx, long key) {
		int m = cellKeys.length - 1, i = mix(key) & m;
		while (cellGen[i] == gen && cellKeys[i] != key) i = (i + 1) & m;
		if (cellGen[i] != gen) { cellGen[i] = gen; cellKeys[i] = key; cellHead[i] = -1; cells++; }
		next[idx] = cellHead[i]; cellHead[i] = idx;
		return idx;
	}
	private void grow() {
		int c = ids.length * 2; ids = java.util.Arrays.copyOf(ids, c); next = java.util.Arrays.copyOf(next, c); xs = java.util.Arrays.copyOf(xs, c); zs = java.util.Arrays.copyOf(zs, c);
	}
	private void growCells() {
		long[] ok = cellKeys; int[] oh = cellHead, og = cellGen; int c = ok.length * 2;
		cellKeys = new long[c]; cellHead = new int[c]; cellGen = new int[c]; int m = c - 1; cells = 0;
		for (int j = 0; j < ok.length; j++) if (og[j] == gen) { int i = mix(ok[j]) & m; while (cellGen[i] == gen) i = (i + 1) & m; cellGen[i] = gen; cellKeys[i] = ok[j]; cellHead[i] = oh[j]; cells++; }
	}

	/**
	 * Writes the indexes of entities whose cell intersects the square [x-r, x+r] x [z-r, z+r] into out and returns how many there are in total.
	 * If the return value is larger than out.length only out.length were written: call again with a bigger array. Entities in a cell are returned even when
	 * they lie slightly outside the radius (cell granularity); the caller does the exact test.
	 */
	public int near(double x, double z, double r, int[] out) {
		int x0 = (int) Math.floor(x - r) >> 4, x1 = (int) Math.floor(x + r) >> 4, z0 = (int) Math.floor(z - r) >> 4, z1 = (int) Math.floor(z + r) >> 4;
		int m = cellKeys.length - 1, cnt = 0;
		for (int cx = x0; cx <= x1; cx++) for (int cz = z0; cz <= z1; cz++) {
			long key = cell(cx, cz); int i = mix(key) & m;
			while (cellGen[i] == gen && cellKeys[i] != key) i = (i + 1) & m;
			if (cellGen[i] != gen) continue;
			for (int e = cellHead[i]; e >= 0; e = next[e]) { if (cnt < out.length) out[cnt] = e; cnt++; }
		}
		return cnt;
	}
}
