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

	private static final class Pair { boolean hidden; int fails, passes; long lastCombat = Long.MIN_VALUE / 2; }
	private final Map<Long, Pair> pairs = new HashMap<>();
	private final Params p;
	public EspCore(Params p) { this.p = p; }

	private static long key(int viewer, int entity) { return ((long) viewer << 32) ^ (entity & 0xFFFFFFFFL); }

	public void attacked(int viewer, int entity, long tick) { pairs.computeIfAbsent(key(viewer, entity), k -> new Pair()).lastCombat = tick; }
	public boolean hidden(int viewer, int entity) { Pair s = pairs.get(key(viewer, entity)); return s != null && s.hidden; }
	public void forget(int viewer, int entity) { pairs.remove(key(viewer, entity)); }

	/**
	 * @param distance viewer eye to entity box distance
	 * @param exempt   adapter-computed: vehicle/passenger, boss, glowing, team member, spectator/creative viewer, leashed...
	 * @param visible  multi-point LOS result including the velocity lead (see design), true if any sample point is visible
	 */
	public Action update(int viewer, int entity, long tick, double distance, boolean exempt, boolean visible) {
		Pair s = pairs.computeIfAbsent(key(viewer, entity), k -> new Pair());
		boolean forceShow = exempt || distance <= p.nearRadius || tick - s.lastCombat <= p.recentCombatTicks;
		if (forceShow) visible = true;
		if (visible) {
			s.fails = 0; s.passes++;
			if (s.hidden && s.passes >= p.showAfterChecks) { s.hidden = false; s.passes = 0; return Action.SHOW; }
			return Action.NONE;
		}
		s.passes = 0; s.fails++;
		if (!s.hidden && s.fails >= p.cullAfterChecks) { s.hidden = true; return Action.HIDE; }
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
	private static void add(java.util.List<Double> l, double x, double y, double z) { l.add(x); l.add(y); l.add(z); }
}
