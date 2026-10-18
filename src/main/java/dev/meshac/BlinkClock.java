package dev.meshac;

/**
 * Blink burst detection, pure so the timing rule runs in sim tests without a server. Fed on the network thread with one
 * arrive() per position packet and one tickEnd() per client tick-end packet. The rule (moved verbatim out of Movement):
 * the client holds every movement packet back and releases them in one burst. A real stall (lag, a frozen client) holds
 * the client's tick-end packets back as well; Blink does not touch them. Movement packets quiet for 500 ms right after
 * the player was moving, tick-end packets still arriving on time, then most of the held packets landing inside 150 ms.
 */
public final class BlinkClock {
	public static final double QUIET_MS = 500;               // movement-packet gap that opens a candidate burst
	public static final long BURST_WINDOW_NS = 150_000_000L; // the held packets must land inside this window
	public static final double TICK_END_MIN = 0.7;           // fraction of expected tick-end packets that must have arrived during the gap
	public static final double BURST_MIN_FRACTION = 0.6;     // fraction of the gap's expected packets that must land in the window
	public static final int BURST_MIN_PACKETS = 6;

	public long lastPosAt, burstStart, burstMark, tickEnds, teAtLastPos;
	public int burstN;
	public double burstGap, ax = Double.NaN, az;
	public boolean lastMoving;

	public void tickEnd() { tickEnds++; }

	/** One position packet arrived at now (System.nanoTime) claiming (x, z). Returns true the moment a burst is confirmed. */
	public boolean arrive(long now, double x, double z) {
		boolean hit = false;
		if (lastPosAt != 0) {
			double gapMs = (now - lastPosAt) / 1e6;
			if (gapMs >= QUIET_MS && lastMoving) {
				long te = tickEnds - teAtLastPos;
				if (te >= TICK_END_MIN * gapMs / 50) { burstStart = now; burstMark = now; burstN = 1; burstGap = gapMs; } else burstStart = 0;
			} else if (burstStart != 0) {
				if (now - burstStart < BURST_WINDOW_NS) { if (++burstN >= Math.max(BURST_MIN_PACKETS, (int) (BURST_MIN_FRACTION * burstGap / 50))) { hit = true; burstStart = 0; } }
				else burstStart = 0;
			}
		}
		lastMoving = !Double.isNaN(ax) && Math.hypot(x - ax, z - az) > 0.1;
		ax = x; az = z; lastPosAt = now; teAtLastPos = tickEnds;
		return hit;
	}
}
