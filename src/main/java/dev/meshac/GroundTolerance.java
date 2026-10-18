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

	public static String line(String player, String support, double supportTopY, double feetY, int buf) {
		return String.format(java.util.Locale.ROOT, "[groundtol] would-have-passed %s support=%s top=%.4f feetY=%.4f gap=%.4f buf=%d", player, support, supportTopY, feetY, feetY - supportTopY, buf);
	}
}
