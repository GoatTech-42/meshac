package dev.meshac;

/** Pure gates for the movement actions that outlive one signal: a hold that keeps replaying setbacks, and the delayed NoFall damage. Monitor mode is absolute: nothing acts, whatever the egregiousChecks list says. */
public final class MonitorGate {
	private MonitorGate() {}
	/** Monitor mode wins over everything, including a check listed in egregiousChecks. */
	public static boolean acts(boolean monitor, boolean egregious) { return !monitor; }
	/** A hold keeps putting the player back only while it is running and its own check is enforced. */
	public static boolean holdReplays(long nowMs, long holdUntilMs, boolean enforced) { return enforced && nowMs < holdUntilMs; }
	/** A NoFall flag arms the delayed fall damage only when nofall is enforced. */
	public static boolean armNoFall(boolean enforced) { return enforced; }
	/** A pending NoFall record is processed (and may apply damage) only while nofall is enforced; otherwise it is dropped. */
	public static boolean processNoFall(long nfAt, boolean enforced) { return enforced && nfAt != 0; }
	public static boolean damage(boolean enforced, double drop, double fdMax) { return enforced && drop > 3 && fdMax < 3; }
}
