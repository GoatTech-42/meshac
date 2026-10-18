package dev.meshac.veil;

import java.util.HashMap;
import java.util.Map;

/**
 * Pure logic of the entity cull: per (viewer, entity) hysteresis state machine. No Minecraft types.
 * The adapter calls update() every checkEveryTicks with the result of a multi-point LOS test and applies the returned action.
 */
public final class EspCore {
	public enum Action { NONE, HIDE, SHOW }

	public static final class Params {
		public int cullAfterChecks = 3;   // consecutive failed LOS checks before hiding (3 checks * 4 ticks = 200 ms)
		public int showAfterChecks = 1;   // one pass shows it again
		public double nearRadius = 8;     // never cull inside this distance
		public int recentCombatTicks = 60; // never cull an entity the viewer attacked in the last 3 s
	}

	private static final class Pair { boolean hidden; int fails, passes; long lastCombat = Long.MIN_VALUE / 2; long lastTick; }

	/**
	 * Open addressing long -> Pair map. The old HashMap<Long, Pair> boxed a Long and ran a lambda per update() call: 50000 pair decisions per cycle at 100 viewers x 500 entities.
	 * Linear probing, backward-shift delete (no tombstones), power-of-two table, load factor 0.5.
	 */
	private static final class PairMap {
		long[] keys = new long[1024]; Pair[] vals = new Pair[1024]; int size;
		private static int mix(long k) { k ^= k >>> 33; k *= 0xff51afd7ed558ccdL; k ^= k >>> 33; return (int) k; }
		Pair get(long k) { int m = keys.length - 1; for (int i = mix(k) & m; vals[i] != null; i = (i + 1) & m) if (keys[i] == k) return vals[i]; return null; }
		Pair getOrCreate(long k) {
			int m = keys.length - 1; int i = mix(k) & m;
			for (; vals[i] != null; i = (i + 1) & m) if (keys[i] == k) return vals[i];
			if ((size + 1) * 2 > keys.length) { grow(); return getOrCreate(k); }
			Pair p = new Pair(); keys[i] = k; vals[i] = p; size++; return p;
		}
		private void grow() {
			long[] ok = keys; Pair[] ov = vals; keys = new long[ok.length * 2]; vals = new Pair[ok.length * 2]; size = 0; int m = keys.length - 1;
			for (int n = 0; n < ok.length; n++) if (ov[n] != null) { int i = mix(ok[n]) & m; while (vals[i] != null) i = (i + 1) & m; keys[i] = ok[n]; vals[i] = ov[n]; size++; }
		}
		Pair remove(long k) {
			int m = keys.length - 1; int i = mix(k) & m;
			while (vals[i] != null && keys[i] != k) i = (i + 1) & m;
			if (vals[i] == null) return null;
			Pair out = vals[i]; vals[i] = null; size--;
			for (int j = (i + 1) & m; vals[j] != null; j = (j + 1) & m) { // backward shift: re-seat the run after the hole
				int h = mix(keys[j]) & m;
				if (((j - h) & m) >= ((j - i) & m)) { keys[i] = keys[j]; vals[i] = vals[j]; vals[j] = null; i = j; }
			}
			return out;
		}
		void clear() { java.util.Arrays.fill(vals, null); size = 0; }
	}

	private final PairMap pairs = new PairMap();
	private int hiddenCount;
	private final Params p;
	public EspCore(Params p) { this.p = p; }

	private static long key(int viewer, int entity) { return ((long) viewer << 32) ^ (entity & 0xFFFFFFFFL); }

	public void attacked(int viewer, int entity, long tick) { Pair s = pairs.getOrCreate(key(viewer, entity)); s.lastCombat = tick; s.lastTick = tick; }
	public boolean hidden(int viewer, int entity) { Pair s = pairs.get(key(viewer, entity)); return s != null && s.hidden; }
	/** True while any pair is hidden. O(1). */
	public boolean anyHidden() { return hiddenCount > 0; }
	public int size() { return pairs.size; }
	public int hiddenPairs() { return hiddenCount; }
	public void clear() { pairs.clear(); hiddenCount = 0; }
	public void forget(int viewer, int entity) { Pair s = pairs.remove(key(viewer, entity)); if (s != null && s.hidden) hiddenCount--; }
	/** Drop every pair of one viewer (disconnect). Entity ids in pairs are not enumerable per viewer, so this walks the table once. */
	public void forgetViewer(int viewer) {
		java.util.ArrayList<Long> drop = new java.util.ArrayList<>();
		for (int i = 0; i < pairs.keys.length; i++) if (pairs.vals[i] != null && (int) (pairs.keys[i] >> 32) == viewer) drop.add(pairs.keys[i]);
		for (long k : drop) { Pair s = pairs.remove(k); if (s != null && s.hidden) hiddenCount--; }
	}
	/** Hidden pairs are not swept by age, so a hidden target that died or logged out would stay in the table (and keep anyHidden true) for good. Call now and then with a live check: pairs whose entity is gone are dropped, nothing is sent (the entity does not exist). Returns the number dropped. */
	public int dropGoneHidden(java.util.function.IntPredicate alive) {
		java.util.ArrayList<Long> drop = new java.util.ArrayList<>();
		for (int i = 0; i < pairs.keys.length; i++) { Pair s = pairs.vals[i]; if (s != null && s.hidden && !alive.test((int) pairs.keys[i])) drop.add(pairs.keys[i]); }
		for (long k : drop) { Pair s = pairs.remove(k); if (s != null && s.hidden) hiddenCount--; }
		return drop.size();
	}
	/**
	 * Drop pairs not updated for maxAgeTicks (entity died or left range, viewer left). Hidden pairs are NOT dropped here: the caller must show those first (a hidden entity that is
	 * gone is harmless, one still alive would stay hidden forever), so only visible pairs are swept. Returns the number removed.
	 */
	public int sweep(long tick, long maxAgeTicks) {
		java.util.ArrayList<Long> drop = new java.util.ArrayList<>();
		for (int i = 0; i < pairs.keys.length; i++) { Pair s = pairs.vals[i]; if (s != null && !s.hidden && tick - s.lastTick > maxAgeTicks) drop.add(pairs.keys[i]); }
		for (long k : drop) pairs.remove(k);
		return drop.size();
	}

	/**
	 * @param distance viewer eye to entity box distance
	 * @param exempt   adapter-computed: vehicle/passenger, boss, glowing, team member, spectator/creative viewer, leashed...
	 * @param visible  multi-point LOS result including the velocity lead (see design), true if any sample point is visible
	 */
	public Action update(int viewer, int entity, long tick, double distance, boolean exempt, boolean visible) {
		Pair s = pairs.getOrCreate(key(viewer, entity)); s.lastTick = tick;
		boolean forceShow = exempt || distance <= p.nearRadius || tick - s.lastCombat <= p.recentCombatTicks;
		if (forceShow) visible = true;
		if (visible) {
			s.fails = 0; s.passes++;
			if (s.hidden && s.passes >= p.showAfterChecks) { s.hidden = false; hiddenCount--; s.passes = 0; return Action.SHOW; }
			return Action.NONE;
		}
		s.passes = 0; s.fails++;
		if (!s.hidden && s.fails >= p.cullAfterChecks) { s.hidden = true; hiddenCount++; return Action.HIDE; }
		return Action.NONE;
	}

	/** Voxel-free slab test: does the segment a->b hit the AABB? Used by the adapter's ray code and tested here. Returns true if blocked by the box. */
	public static boolean segmentHitsBox(double ax, double ay, double az, double bx, double by, double bz,
			double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		double t0 = 0, t1 = 1;
		double[] a = {ax, ay, az}, d = {bx - ax, by - ay, bz - az}, lo = {minX, minY, minZ}, hi = {maxX, maxY, maxZ};
		for (int i = 0; i < 3; i++) {
			if (Math.abs(d[i]) < 1e-12) { if (a[i] < lo[i] || a[i] > hi[i]) return false; continue; }
			double ta = (lo[i] - a[i]) / d[i], tb = (hi[i] - a[i]) / d[i];
			if (ta > tb) { double t = ta; ta = tb; tb = t; }
			t0 = Math.max(t0, ta); t1 = Math.min(t1, tb);
			if (t0 > t1) return false;
		}
		return true;
	}

	/** Sample points of the target box for the LOS test: centre + 8 corners shrunk by 5% + 4 face centres at mid height. Returns n*3 doubles. */
	public static double[] samplePoints(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		double cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, cz = (minZ + maxZ) / 2;
		double sx = (maxX - minX) * 0.45, sy = (maxY - minY) * 0.45, sz = (maxZ - minZ) * 0.45;
		java.util.ArrayList<Double> l = new java.util.ArrayList<>();
		add(l, cx, cy, cz);
		for (int i = -1; i <= 1; i += 2) for (int j = -1; j <= 1; j += 2) for (int k = -1; k <= 1; k += 2) add(l, cx + i * sx, cy + j * sy, cz + k * sz);
		add(l, cx + sx, cy, cz); add(l, cx - sx, cy, cz); add(l, cx, cy, cz + sz); add(l, cx, cy, cz - sz);
		double[] r = new double[l.size()]; for (int i = 0; i < r.length; i++) r[i] = l.get(i);
		return r;
	}
	/** Number of doubles samplePointsInto writes: 13 points x 3. */
	public static final int SAMPLE_DOUBLES = 13 * 3;
	/**
	 * Same points as samplePoints, written into a caller buffer of at least SAMPLE_DOUBLES. samplePoints boxed 39 Doubles and grew an ArrayList on every line of sight test
	 * (up to 200 tests per tick, per viewer and entity pair); this allocates nothing. Returns the number of doubles written.
	 */
	public static int samplePointsInto(double[] o, double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		double cx = (minX + maxX) / 2, cy = (minY + maxY) / 2, cz = (minZ + maxZ) / 2;
		double sx = (maxX - minX) * 0.45, sy = (maxY - minY) * 0.45, sz = (maxZ - minZ) * 0.45;
		int n = 0;
		o[n++] = cx; o[n++] = cy; o[n++] = cz;
		for (int i = -1; i <= 1; i += 2) for (int j = -1; j <= 1; j += 2) for (int k = -1; k <= 1; k += 2) { o[n++] = cx + i * sx; o[n++] = cy + j * sy; o[n++] = cz + k * sz; }
		o[n++] = cx + sx; o[n++] = cy; o[n++] = cz; o[n++] = cx - sx; o[n++] = cy; o[n++] = cz; o[n++] = cx; o[n++] = cy; o[n++] = cz + sz; o[n++] = cx; o[n++] = cy; o[n++] = cz - sz;
		return n;
	}
	private static void add(java.util.List<Double> l, double x, double y, double z) { l.add(x); l.add(y); l.add(z); }
}
