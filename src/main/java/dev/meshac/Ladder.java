package dev.meshac;

/** The punishment maths, with no game code in it so it can be replayed and tested offline. See docs/PUNISHMENT-DESIGN.md. */
public final class Ladder {
	public enum Step { SETBACK, HOLD, REMOVE }

	/** Tunable numbers. All come from config. clusterMs: flags closer together than this are one cause (a lag stall hits many checks in one burst). */
	public record Params(double halfLifeSec, double holdAt, double removeAt, int minClusters, int minClustersTierA, int tierAWeight, long clusterMs) {
		public static Params defaults() { return new Params(8, 3, 6, 3, 2, 3, 150); }
	}

	/** Heat of one player: exponential decay, flags bunched inside one burst count once (the heaviest), removal needs several independent causes. */
	public static final class Heat {
		public double heat; long at; long clusterAt; double clusterMax; int clusters, tierAClusters;
		public double now(long t, Params p) { return heat * Math.pow(0.5, Math.max(0, t - at) / 1000.0 / p.halfLifeSec()); }
		public Step add(String check, int weight, long t, Params p) {
			double h = now(t, p);
			if (h < 0.25) { h = 0; clusters = 0; tierAClusters = 0; clusterMax = 0; clusterAt = 0; }
			if (clusterAt == 0 || t - clusterAt > p.clusterMs()) { clusterAt = t; clusters++; clusterMax = 0; if (weight >= p.tierAWeight()) tierAClusters++; }
			else if (weight >= p.tierAWeight() && clusterMax < p.tierAWeight()) tierAClusters++;
			if (weight > clusterMax) { h += weight - clusterMax; clusterMax = weight; }
			heat = h; at = t;
			if (tierAClusters >= p.minClustersTierA() && h >= p.removeAt() - 1) return Step.REMOVE; // two separate tier A bursts: no need to wait for full heat
			if (h >= p.removeAt() && clusters >= p.minClusters()) return Step.REMOVE;
			if (h >= p.holdAt()) return Step.HOLD;
			return Step.SETBACK;
		}
		public void reset() { heat = 0; clusters = 0; tierAClusters = 0; clusterMax = 0; clusterAt = 0; }
		public boolean confirmedTierA() { return tierAClusters >= 2; }
	}

	/** Offences forgive themselves: each one counts 0.5^(age / halfLifeDays). */
	public static double effectiveOffences(double[] agesDays, double halfLifeDays) {
		double n = 0; for (double a : agesDays) n += Math.pow(0.5, a / halfLifeDays); return n;
	}

	/** A fresh offence counts 0.99995 after a minute, not 1, because it already started to decay. Without this tolerance floor() put a repeat offence minutes later back on the kick rung. */
	static final double TOLERANCE = 0.05;
	/** Rung 1 is a kick. Rung r >= 2 is a tempban of base * growth^(r-2) minutes, capped. Tier A evidence skips one rung. */
	public static int rung(double effective, int kicks, boolean skip) { return (int) Math.floor(effective + TOLERANCE) + 1 + (skip ? 1 : 0); }
	public static long tempMinutes(int rung, int kicks, double base, double growth, double max) {
		int idx = rung - 1 - kicks; // 0 = first tempban
		return (long) Math.min(max, base * Math.pow(growth, Math.max(0, idx)));
	}
	public static boolean permanent(boolean permanentEnabled, boolean tierAConfirmed, int rung, int permRung) { return permanentEnabled && tierAConfirmed && rung >= permRung; }
}
