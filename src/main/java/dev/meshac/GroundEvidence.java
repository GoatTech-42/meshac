package dev.meshac;

/** Sliding-window evidence for the nofall ground-spoof check. Pure: no Minecraft types, replayable in tests.
 *
 * Why a window: the old counter required 3 CONSECUTIVE unsupported ground claims and hard-reset on any
 * other packet. Rig round 2 (2026-10-17): a NoFall that interleaves one spoofed ground claim every couple
 * of ticks (and honest air packets between) never reached 3 in a row, so an 82-block fall took no damage
 * and raised no signal - and the same spoofed claims reset the fly/high-jump chains. The window counts
 * unsupported claims over the last W ground claims; honest air packets neither add nor erase.
 *
 * Window/threshold are PROPOSALS pending the zero-FP legit battery (sneak-edge jitter is the FP-risk row). */
public final class GroundEvidence {
	private final int window, threshold;
	private int ring, filled, count; // last `filled` claims as bits, newest at bit 0

	public GroundEvidence(int window, int threshold) {
		if (window < 1 || window > 30) throw new IllegalArgumentException("window " + window);
		if (threshold < 1 || threshold > window) throw new IllegalArgumentException("threshold " + threshold + " of " + window);
		this.window = window; this.threshold = threshold;
	}

	/** Record one ground claim: true when the probe found only air below (unsupported). */
	public void add(boolean unsupported) {
		if (filled == window) { if ((ring >> (window - 1) & 1) != 0) count--; } // evict oldest
		else filled++;
		ring = (ring << 1 | (unsupported ? 1 : 0)) & ((1 << window) - 1);
		if (unsupported) count++;
	}

	/** Unsupported claims inside the window. */
	public int unsupported() { return count; }
	/** True when the window holds at least `threshold` unsupported claims. */
	public boolean flagged() { return count >= threshold; }
	/** Fresh start (exempt/grace ground claim, setback, teleport). */
	public void clear() { ring = 0; filled = 0; count = 0; }
}
