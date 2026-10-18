package dev.meshac;

/** The punishment maths, with no game code in it so it can be replayed and tested offline. See docs/PUNISHMENT-DESIGN.md. */
public final class Ladder {
	public enum Step { SETBACK, HOLD, REMOVE }

	/** Tunable numbers. All come from config. clusterMs: flags closer together than this are one cause (a lag stall hits many checks in one burst). */
	public record Params(double halfLifeSec, double holdAt, double removeAt, int minClusters, int minClustersTierA, int tierAWeight, long clusterMs) {
		public static Params defaults() { return new Params(8, 3, 6, 3, 2, 3, 150); }
		/** Invalid values fail CLOSED: each bad knob falls back to the reviewed default, never to a stricter or looser value. */
		public Params sanitized() {
			Params d = defaults();
			return new Params(
				!(halfLifeSec > 0) || !Double.isFinite(halfLifeSec) ? d.halfLifeSec() : halfLifeSec,
				!(holdAt > 0) || !Double.isFinite(holdAt) ? d.holdAt() : holdAt,
				!(removeAt > 0) || !Double.isFinite(removeAt) ? d.removeAt() : removeAt,
				minClusters <= 0 ? d.minClusters() : minClusters,
				minClustersTierA <= 0 ? d.minClustersTierA() : minClustersTierA,
				tierAWeight <= 0 ? d.tierAWeight() : tierAWeight,
				clusterMs <= 0 ? d.clusterMs() : clusterMs);
		}
		/** Comma-joined names of the knobs sanitized() would replace; empty when the params are valid. For the caller's diagnostic log. */
		public static String problems(Params p) {
			StringBuilder b = new StringBuilder();
			if (!(p.halfLifeSec() > 0) || !Double.isFinite(p.halfLifeSec())) b.append(",halfLifeSeconds");
			if (!(p.holdAt() > 0) || !Double.isFinite(p.holdAt())) b.append(",holdAt");
			if (!(p.removeAt() > 0) || !Double.isFinite(p.removeAt())) b.append(",removeAt");
			if (p.minClusters() <= 0) b.append(",requireClusters");
			if (p.minClustersTierA() <= 0) b.append(",requireClustersTierA");
			if (p.tierAWeight() <= 0) b.append(",tierAWeight");
			if (p.clusterMs() <= 0) b.append(",clusterMs");
			return b.length() == 0 ? "" : b.substring(1);
		}
	}

	/** Heat of one player: exponential decay, flags bunched inside one burst count once (the heaviest), removal needs several independent causes. */
	public static final class Heat {
		public double heat; long at; long clusterAt; double clusterMax; int clusters, tierAClusters;
		// Policy epoch: the params this evidence was collected under. A config change (e.g. /mesh reload) must never
		// RECLASSIFY existing clusters under a new threshold, so a params mismatch wipes the evidence (fail closed) instead.
		private double pHalf, pHold, pRem; private int pMinc, pMinta, pTaw; private long pCms; private boolean bound;
		private boolean epochStale(Params p) {
			return bound && (pHalf != p.halfLifeSec() || pHold != p.holdAt() || pRem != p.removeAt() || pMinc != p.minClusters() || pMinta != p.minClustersTierA() || pTaw != p.tierAWeight() || pCms != p.clusterMs());
		}
		private void bind(Params p) { pHalf = p.halfLifeSec(); pHold = p.holdAt(); pRem = p.removeAt(); pMinc = p.minClusters(); pMinta = p.minClustersTierA(); pTaw = p.tierAWeight(); pCms = p.clusterMs(); bound = true; }
		public double now(long t, Params p) { return heat * Math.pow(0.5, Math.max(0, t - at) / 1000.0 / p.halfLifeSec()); }
		public Step add(String check, int weight, long t, Params p) {
			if (epochStale(p)) reset(); // policy changed: old evidence is never reclassified under the new thresholds
			bind(p);
			double h = now(t, p);
			// Intentional (decision relayed via parent 10-18, option a of the lane-A design question): tier-A evidence decays WITH the heat.
			// Below the 0.25 floor (~25-28 s after the last flag at the default 8 s half-life) the next flag wipes tierAClusters too, so
			// 'tier A confirmed' only survives inside one active build-up. Strictest option, chosen while enforcement is log-only;
			// revisit a separate, longer tier-A evidence lifetime (option b) before any enforcement-ON proposal.
			if (h < 0.25) { h = 0; clusters = 0; tierAClusters = 0; clusterMax = 0; clusterAt = 0; }
			if (clusterAt == 0 || t - clusterAt > p.clusterMs()) { clusterAt = t; clusters++; clusterMax = 0; if (weight >= p.tierAWeight()) tierAClusters++; }
			else if (weight >= p.tierAWeight() && clusterMax < p.tierAWeight()) tierAClusters++;
			if (weight > clusterMax) { h += weight - clusterMax; clusterMax = weight; }
			heat = h; at = t;
			// Shortcut reachability: two tier-A bursts add at least 2 * tierAWeight heat, so two MINIMUM-weight bursts clear the removeAt - 1 floor
			// only when removeAt <= 2 * tierAWeight + 1. Weight 3 is the only tier-A weight signalled today (Glide.java:58 and :71, both weight 3);
			// with those bursts the branch is live on default (6 <= 7) and strict (5 <= 7) and subsumed on the shipped lenient preset (removeAt 8 > 7:
			// two weight-3 bursts cap at 5.95 < 7 and the third burst trips the generic gate in the same add() call). A future weight-4+ tier-A
			// signal would clear the lenient floor and make the branch live there too. Documented behavior, unchanged (Box lane A, 10-18).
			if (tierAClusters >= p.minClustersTierA() && h >= p.removeAt() - 1) return Step.REMOVE; // two separate tier A bursts: no need to wait for full heat
			if (h >= p.removeAt() && clusters >= p.minClusters()) return Step.REMOVE;
			if (h >= p.holdAt()) return Step.HOLD;
			return Step.SETBACK;
		}
		public void reset() { heat = 0; clusters = 0; tierAClusters = 0; clusterMax = 0; clusterAt = 0; bound = false; }
		public boolean confirmedTierA(Params p) { return tierAClusters >= p.minClustersTierA(); } // honors the configured bar (requireClustersTierA), not a hardcoded 2
	}

	/** Offences forgive themselves: each one counts 0.5^(age / halfLifeDays). */
	public static double effectiveOffences(double[] agesDays, double halfLifeDays) {
		if (!(halfLifeDays > 0) || !Double.isFinite(halfLifeDays)) return 0; // invalid half-life: fail closed (no punishment inflation); the caller validates and logs
		double n = 0;
		for (double a : agesDays) {
			if (Double.isNaN(a)) continue; // corrupt case data: the offence is not counted (fail closed); the caller diagnoses
			n += Math.pow(0.5, Math.max(0, a) / halfLifeDays); // a future-dated case (clock skew, hand-edited cases.json) counts as fresh, never as more than 1 offence
		}
		return n;
	}

	/** A fresh offence counts 0.99995 after a minute, not 1, because it already started to decay. Without this tolerance floor() put a repeat offence minutes later back on the kick rung. */
	static final double TOLERANCE = 0.05;
	/** Rung 1 is a kick. Rung r >= 2 is a tempban of base * growth^(r-2) minutes, capped. Tier A evidence skips one rung. */
	public static int rung(double effective, int kicks, boolean skip) { if (!Double.isFinite(effective) || effective < 0) effective = 0; return (int) Math.floor(effective + TOLERANCE) + 1 + (skip ? 1 : 0); } // non-finite/negative input fails closed to the kick rung
	public static long tempMinutes(int rung, int kicks, double base, double growth, double max) {
		int idx = rung - 1 - kicks; // 0 = first tempban
		return (long) Math.min(max, base * Math.pow(growth, Math.max(0, idx)));
	}
	public static boolean permanent(boolean permanentEnabled, boolean tierAConfirmed, int rung, int permRung) { return permanentEnabled && tierAConfirmed && rung >= permRung; }
}
