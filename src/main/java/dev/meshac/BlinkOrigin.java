package dev.meshac;

/** Remembers where a possible Blink burst began, so a confirmed Blink can put the player back there. Pure, no Minecraft. A snapshot only counts for the burst it was taken for. */
public final class BlinkOrigin {
	public enum Act { DEFER, ORIGIN, SIGNAL_ONLY }
	/** Position packets we wait for the snapshot before giving up and signalling without a setback. */
	public static final int MAX_DEFER = 3;
	private long snapMark;
	private int deferred;
	public double x, y, z;

	/** Main thread, for every position packet: arrivedNs is its arrival stamp, burstMark the stamp of the first packet of the current candidate burst (0 = none). */
	public void snapshot(Long arrivedNs, long burstMark, boolean init, double gx, double gy, double gz) {
		if (arrivedNs == null || burstMark == 0 || arrivedNs.longValue() != burstMark || !init) return;
		snapMark = burstMark; x = gx; y = gy; z = gz;
	}

	/** A Blink was detected on the network thread. Called for each packet until it returns something other than DEFER. */
	public Act onHit(long burstMark, boolean positionPacket) {
		if (snapMark != 0 && snapMark == burstMark) { snapMark = 0; deferred = 0; return Act.ORIGIN; }
		if (positionPacket && ++deferred <= MAX_DEFER) return Act.DEFER; // the main thread has not reached the first packet of the burst yet
		deferred = 0;
		return Act.SIGNAL_ONLY;
	}
}
