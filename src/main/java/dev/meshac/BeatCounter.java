package dev.meshac;

/**
 * The "item moves on a steady beat" rule of Inventory as a pure class so it can be replayed.
 * Same rule as before: 5 equal gaps (ActionEvidence.steadyGap: 20 to 159 ms, within 8 ms of the previous gap) from a standing start signal, and a beat that goes on signals again every 2 moves.
 * What changed: in one stretch of inventory activity (no pause of IDLE_MS or more) only the first SESSION_SIGNALS signals come at that pace, then at most one per RESIGNAL_MS. A tick-locked sorter (a client mod that sorts by sending one click per client tick) keeps a beat for a minute and used to
 * give one signal per 2 clicks, hundreds a minute. A macro emptying one chest (27 stacks) gets the signals it got before, found at the same click.
 * Timing alone cannot tell a macro from a sorting mod at the same pace. This bounds how much heat the pace can build, it does not claim to tell them apart.
 */
public final class BeatCounter {
	public static final int SESSION_SIGNALS = 10;
	public static final long RESIGNAL_MS = 10000, IDLE_MS = 3000;
	private long lastGap; private int even, sessionSignals; private long lastSignalAt = Long.MIN_VALUE / 2;

	/** @param gap ms since the previous counted click @return true when this click should signal */
	public boolean observe(long gap, long now) {
		if (gap >= IDLE_MS) sessionSignals = 0; // a pause: the next chest or the next sort starts fresh
		boolean steady = ActionEvidence.steadyGap(gap, lastGap);
		lastGap = gap;
		if (!steady) { even = 0; return false; } // one irregular gap does not hand back the allowance: network jitter breaks a tick-locked beat now and then
		if (++even < 5) return false;
		even = 3; // keep counting, as before
		if (sessionSignals >= SESSION_SIGNALS && now - lastSignalAt < RESIGNAL_MS) return false;
		sessionSignals++; lastSignalAt = now;
		return true;
	}
	public void reset() { lastGap = 0; even = 0; sessionSignals = 0; lastSignalAt = Long.MIN_VALUE / 2; }
}
