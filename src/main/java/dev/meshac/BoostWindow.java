package dev.meshac;

/**
 * Pure rule for the post-boost exemption (wind charge, mace smash, riptide). The window opens when a boost is seen and stays open for
 * windowMs, then until the player touches the ground. Movement.java calls it once per packet.
 */
public final class BoostWindow {
	private BoostWindow() {}
	public static final long WINDOW_MS = 4000;
	/** Hard ceiling on the airborne extension. A wind charge or mace launch is over well inside this; without it one launch (or one wind charge thrown by anyone nearby) exempted a player from every movement check for as long as their client never claimed ground, so a Fly module that spoofs onGround=false was unmonitored. */
	public static final long MAX_AIR_MS = 15000;
	/** @param boostSeenAt wall-clock ms of the latest boost, 0 if none */
	/** Wind charges closer than this knock the player around whoever threw them (burst radius is about 1.2 blocks). */
	public static final double OTHER_CHARGE_DIST = 3.5;
	/**
	 * Does a wind charge near the player open the boost window? Their own charge, any distance up to the scan radius (they throw it and ride it).
	 * Someone else's charge (an ally, a breeze) only when it is close enough to have knocked them: a charge flying 10 blocks away is no reason to exempt anybody.
	 */
	public static boolean chargeCounts(boolean ownCharge, double distSq) { return ownCharge || distSq <= OTHER_CHARGE_DIST * OTHER_CHARGE_DIST; }

	public static boolean boosted(long nowMs, long boostSeenAt, boolean ground, boolean wasBoosted) {
		if (boostSeenAt != 0 && nowMs - boostSeenAt < WINDOW_MS) return true;
		return !ground && wasBoosted && boostSeenAt != 0 && nowMs - boostSeenAt < MAX_AIR_MS;
	}
}
