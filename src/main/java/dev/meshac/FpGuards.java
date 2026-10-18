package dev.meshac;

/** Small pure rules that keep vanilla movement from tripping the fly and microhop checks. No Minecraft types, so real traces can be replayed in tests. */
public final class FpGuards {
	private FpGuards() {}
	/** Vanilla climbs out of powder snow at exactly 0.2 blocks per tick while the player holds jump (LivingEntity: jumping inside powder snow sets dy to 0.2). */
	public static final double POWDER_CLIMB_DY = 0.21;
	/** A position packet that arrived more than 1.7 ticks after the previous one covers more than one tick of motion. */
	public static final double STALE_DT = 0.085;

	/** Rising slowly next to powder snow: the vanilla climb, 0.2 per tick, which also shows up as fly, high jump and microhop. */
	public static boolean powderRise(double dy, boolean powderNear) { return powderNear && dy > 0 && dy <= POWDER_CLIMB_DY; }

	/** The powder climb, strictly: the body must overlap powder snow (the feet cell or the cell above) - vanilla only climbs while inside it (LivingEntity travel treats powder as climbable). "Near" is not enough: a hack next to a powder column must not inherit the band. */
	public static boolean powderClimb(double dy, boolean bodyInPowder) { return bodyInPowder && dy > 0 && dy <= POWDER_CLIMB_DY; }

	/** Struggle-climbing out of powder snow kicks dy ~0.63 (mutant319 FP MA-65NE 2026-10-11). The jump check exempts this BAND only, not any big dy near powder - rig round 2: a hack big-jumped beside powder under the blanket exemption. PROPOSAL band; the zero-FP legit battery gates it. */
	public static final double POWDER_JUMP_LO = 0.60, POWDER_JUMP_HI = 0.70;
	/** True when a ground-leaving dy near powder is the vanilla struggle-climb kick, not a boosted jump. */
	public static boolean powderJumpBand(double dy, boolean powderNear) { return powderNear && dy >= POWDER_JUMP_LO && dy <= POWDER_JUMP_HI; }

	/** Falling, and the previous packet spanned several ticks: its dy is a multi-tick drop, so the expected next dy built from it is too low and a normal one-tick drop looks like hovering. */
	public static boolean staleFall(double dy, double prevDtS) { return dy < 0 && prevDtS > STALE_DT; }

	/** Geyser (with or without Floodgate) gives Bedrock players a UUID with a zero high half. The name prefix is not used: a Java client can claim any name. */
	public static boolean bedrockUuid(long mostSigBits) { return mostSigBits == 0L; }

	/** Milliseconds the fly check stays off after leaving a medium that bends gravity (slow falling, water, web...): the first packets outside still carry its dy. */
	public static final long MEDIUM_GRACE_MS = 400;

	/** The fly check does not run while the server says the player is in a medium or under an effect that changes the gravity chain, nor just after leaving it. Keyed on server state only, never on dy. */
	public static boolean flyMediumExempt(boolean inMediumNow, long msSinceMedium) { return inMediumNow || msSinceMedium < MEDIUM_GRACE_MS; }

	/** Fly buffer after one mid-air packet. expect is the vanilla next dy from the previous one. */
	public static int flyBuf(int buf, double dy, double expect, boolean powderRise, boolean staleFall) {
		boolean over = dy > expect + 0.03 && !powderRise && !staleFall;
		return over ? buf + (dy > expect + 0.3 ? 3 : 1) : Math.max(0, buf - 1);
	}


	/** Kick count with the 2.5 s window: outside the window the count restarts. */
	public static int airKickCount(int ajN, long nowMs, long ajAt) { return nowMs - ajAt > 2500 ? 1 : ajN + 1; }

	/** Two kicks inside 8 packets, or four inside 2.5 s, is not vanilla. */
	public static boolean airJumpFlag(int sinceJump, int ajN) { return (sinceJump <= 8 && ajN >= 2) || ajN >= 4; }

	/** Powder snow (like cobweb or a berry bush) is a "stuck" block: after every move vanilla multiplies the motion by (0.9, 1.5, 0.9) and zeroes the velocity, so a falling player drops (0 - 0.08) * 0.98 * 1.5 = -0.1176 every tick, not the free-fall chain. dy -0.118 right after a -0.118 packet is vanilla. */
	public static final double POWDER_STUCK_DY = -0.08 * 0.98 * 1.5;

	/** Expected next dy. When the body overlaps powder snow (the feet cell or the cell above), the stuck drop is the floor of what vanilla produces. */
	public static double stuckExpect(double expect, boolean bodyInPowder) { return bodyInPowder ? Math.max(expect, POWDER_STUCK_DY) : expect; }

	/** Standing on something inside powder snow: the move is blocked by the floor, so dy is 0 (or a hair under) while the free-fall chain still expects -0.078. Needs server state for both the medium and the support, never dy alone. */
	public static boolean restingInPowder(boolean bodyInPowder, boolean supportedBelow, double dy) { return bodyInPowder && supportedBelow && dy <= 0.001 && dy >= POWDER_STUCK_DY - 0.03; }

	/** A fresh upward kick in mid-air: more than 0.3 above the dy the vanilla gravity chain expects from the previous packet. */
	public static boolean airKick(double dy, double expect) { return dy > expect + 0.3; }

	/** Cobweb scales the next move by 0.25 horizontally (javap WebBlock: Vec3(0.25, 0.05, 0.25)). A legit sprint-jump packet is about 0.36 x speed ratio, so the one packet after entering a web is about 0.09 x ratio (speed II about 0.13). The cap is the old flat 0.10 scaled by the ratio, never below 0.10 and never above 0.25 (ratio 2.5). A non-finite, zero or negative ratio (bad attribute) gives the old flat 0.10. */
	public static double webCap(double speedRatio) {
		if (Double.isNaN(speedRatio) || Double.isInfinite(speedRatio) || speedRatio <= 1.0) return 0.10;
		return 0.10 * Math.min(speedRatio, 2.5);
	}

	/** A body stuck in a cobweb moves almost nowhere vertically (x0.05) and is never rising faster than a hair: a packet that rises is not a web packet. */
	public static boolean webDyOk(double dy) { return dy <= 0.05; }

	/** A honey slide moves down at 0.05 per tick (HoneyBlock.doSlideMovement) or falls into it: a packet that hovers or rises is not a slide. */
	public static boolean honeyDyOk(double dy) { return dy <= -0.04; }

	/** Mediums that bend the gravity chain for as long as the player is in them: effects, fluids, climbables, bubble columns, scaffolding. */
	public static boolean hardMedium(boolean slowFalling, boolean levitation, boolean water, boolean lava, boolean climbable, boolean bubble, boolean scaffolding) { return slowFalling || levitation || water || lava || climbable || bubble || scaffolding; }

	/** Cobweb or honey exempts a packet only when the body box overlaps one AND the packet moves the way stuck or sliding does. Applies to this packet alone. */
	public static boolean stuckNow(double dy, boolean webBody, boolean honeySlideBody) { return (webBody && webDyOk(dy)) || (honeySlideBody && honeyDyOk(dy)); }

	/** Server-side states that bend the gravity chain and are exempt from the whole movement block: water, lava, climbables, levitation, slow falling. */
	public static boolean bendsGravity(boolean water, boolean lava, boolean climbable, boolean levitation, boolean slowFalling) { return water || lava || climbable || levitation || slowFalling; }
}
