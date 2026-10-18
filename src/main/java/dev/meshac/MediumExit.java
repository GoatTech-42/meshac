package dev.meshac;

/** The one packet after leaving water, lava, a climbable or a gravity-bending effect carries that medium's momentum: a water-exit jump moves 0.3 out of a 0.02 swim packet. Only that first packet, within 300 ms of the last medium tick and not above 0.32 up, is waived from the fly and air-jump counters. Every later packet must follow the vanilla gravity chain from that dy, and each waiver needs a fresh medium tick, so a cheat that dips in and out gets at most 0.32 per real visit. Pure state, one per player. */
public final class MediumExit {
	public static final long WINDOW_MS = 300;
	public static final double MAX_EXIT_DY = 0.32;
	private long lastMediumMs;
	private boolean armed;
	/** Call on every packet processed while the server says the player is in the medium. */
	public void onMedium(long nowMs) { lastMediumMs = nowMs; armed = true; }
	/** Call once on every packet processed outside the medium. True only for the first one, if it is a vanilla exit step. */
	public boolean firstAfter(long nowMs, double dy) {
		boolean was = armed;
		armed = false;
		return was && nowMs - lastMediumMs <= WINDOW_MS && dy <= MAX_EXIT_DY;
	}
}
