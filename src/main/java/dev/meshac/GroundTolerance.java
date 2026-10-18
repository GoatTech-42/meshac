package dev.meshac;

/**
 * Candidate per-shape tolerance for the ground-spoof check, compare mode only (nothing is enforced).
 * The check today looks for collision in the 0.1 strip under the feet. Several partial shapes sit 0.125 off (snow layers, chests, soul sand).
 * The candidate widens the strip to 0.13 only when the thing found has a partial top (not a whole block height), so a real NoFall gap
 * over open air or over full blocks gets no extra room.
 */
public final class GroundTolerance {
	private GroundTolerance() {}
	public static final double STRIP = 0.10, CANDIDATE = 0.13;

	/** A top that is not a whole number of blocks: slab, snow layer, carpet, chest, soul sand, trapdoor, fence-less partial shapes. */
	public static boolean partialTop(double topY) {
		double f = topY - Math.floor(topY);
		return f > 0.001 && f < 0.999;
	}

	/** Would the candidate have accepted a claim that the 0.1 strip called air? strip10Air: current rule says air. hit13: collision exists in the 0.13 strip. */
	public static boolean wouldPass(boolean strip10Air, boolean hit13, double supportTopY) {
		return strip10Air && hit13 && partialTop(supportTopY);
	}

	/** Classify-mode tolerance: the candidate rule plus a bound on the feet-to-support gap (a claim floating above a partial block is not tolerance, it is a spoof). */
	public static final double GAP_EPS = 0.005;
	public static boolean tolerated(boolean strip10Air, boolean hit13, double supportTopY, double feetY) {
		return wouldPass(strip10Air, hit13, supportTopY) && feetY - supportTopY <= CANDIDATE + GAP_EPS;
	}

	public static String line(String player, String support, double supportTopY, double feetY, int buf) {
		return String.format(java.util.Locale.ROOT, "[groundtol] would-have-passed %s support=%s top=%.4f feetY=%.4f gap=%.4f buf=%d", player, support, supportTopY, feetY, feetY - supportTopY, buf);
	}

	/** Classify-mode tolerated line: its own shape, never the compare-mode marker, so log scrapers can tell a verdict-relevant tolerance from a would-have-passed observation. */
	public static String toleratedLine(String player, String support, double supportTopY, double feetY, int buf) {
		return String.format(java.util.Locale.ROOT, "[groundtol] tolerated %s support=%s top=%.4f feetY=%.4f gap=%.4f buf=%d", player, support, supportTopY, feetY, feetY - supportTopY, buf);
	}
}
