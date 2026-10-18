package dev.meshac;

/** One-line format for the ground-spoof context trace (pure, so the format is tested). Fields are what a reviewer needs to tell a partial block or ledge from a spoof. */
public final class GroundTrace {
	private GroundTrace() {}
	public static String line(String player, boolean flagged, String feet, double feetTop, String below, double belowTop, String below2, boolean airBelow, boolean ground, boolean prevGround, int buf, double dy, double dtS, boolean exempt) {
		return String.format(java.util.Locale.ROOT, "[trace] groundctx %s flag=%b feet=%s/%.4f below=%s/%.4f below2=%s airBelow=%b ground=%b prevGround=%b buf=%d dy=%.3f dtS=%.3f exempt=%b",
			player, flagged, feet, feetTop, below, belowTop, below2, airBelow, ground, prevGround, buf, dy, dtS, exempt);
	}
}
