package dev.meshac;

/** Pure copy of the rule Movement uses to tell a server-ordered move (pearl, chorus fruit, Enderman, portal, /tp) from a client move. */
public final class TeleportDetect {
	private TeleportDetect() {}
	public static final double MIN_JUMP = 0.75;
	/** atStart/atClaim: the server position equals the previous packet's start or claim. offBy: distance to the nearer of the two. */
	public static boolean serverMoved(boolean init, boolean atStart, boolean atClaim, double offBy) {
		return !init || (!(atStart || atClaim) && offBy > MIN_JUMP);
	}
}
