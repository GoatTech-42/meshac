package dev.meshac;

/**
 * Pure math of the sustained-speed budget (candidate4), no Minecraft types, so legit traces can be replayed in tests.
 * Movement.java feeds it values read from the player and applies the returned balance.
 */
public final class SustainBudget {
	private SustainBudget() {}
	/** Flat sprint-jump on this rig averaged 7.09 bps; blocks per tick at attribute ratio 1.0. */
	public static final double BASE_PER_TICK = 0.37;
	public static final double LIMIT = 4.0, CAP = 6.0;

	/**
	 * Vanilla LivingEntity.getBlockSpeedFactor() = lerp(MOVEMENT_EFFICIENCY, Block.getSpeedFactor(), 1.0) (checked with javap on 26.2).
	 * Anything that raises movement_efficiency (Soul Speed on soul sand, plugin or datapack modifiers) removes part or all of the block slowdown.
	 */
	public static double effectiveSlow(double blockFactor, double movementEfficiency) {
		double e = Math.max(0, Math.min(1, movementEfficiency));
		return blockFactor + (1.0 - blockFactor) * e;
	}

	/** Per-packet horizontal cap in blocks. groundBoth: the player and the packet both claim ground. Same arithmetic Movement used inline; moved here so legit traces can be replayed. */
	public static double packetCap(boolean groundBoth, double speedRatio, double friction, int slowTicks, double slowEff, boolean bedrock) {
		double cap = (groundBoth ? 0.34 : 0.62) * speedRatio * (friction > 0.61 ? 3 : 1) * (slowTicks >= 5 ? Math.max(slowEff, 0.65) : 1.0);
		return cap * (bedrock ? 1.30 : 1.12); // sprint start lags the speed attribute by a tick; Bedrock movement packets are rounder
	}

	/** Budget in blocks per tick. */
	public static double perTick(double speedRatio, boolean iceMomentum, int slowTicks, double slow, boolean bedrock, boolean web, boolean gliding, double glideH) {
		double s = BASE_PER_TICK * speedRatio * (iceMomentum ? 1.6 : 1) * (slowTicks >= 5 ? Math.max(slow, 0.65) : 1.0);
		s *= bedrock ? 1.30 : 1.00;
		if (gliding) s = Math.max(s, glideH + 0.05);
		if (web) s = 0.12;
		return s;
	}

	/** One packet: returns the new balance. dtS is clamped here to [0.05, 1.0]: a stalled link releasing held packets is charged one tick per packet at least, and a long gap credits at most one second. */
	public static double step(double bal, double h, double perTick, double dtS) {
		double dt = Math.min(1.0, Math.max(0.05, dtS));
		return Math.max(0, Math.min(CAP, bal + h - perTick * 20 * dt));
	}
	/**
	 * Balance on a packet that is exempt (water, boost, knockback, boat). The first packet of an exempt stretch halves what was banked, later ones keep it:
	 * zeroing it let anyone who touched water or took a hit every few seconds hold an 8 bps cheat under the limit, while keeping all of it could flag the
	 * first normal step after a legit sprint that ended near the limit. Halving per stretch needs two quick stretches to forgive a cheat's whole bank, and forgives a legit one at once.
	 */
	public static double onExempt(double bal, boolean wasExempt) { return wasExempt ? bal : bal * 0.5; }

	/** Log line for the Bedrock compare mode (the Java budget run beside the real Bedrock one, log only). */
	public static String shadowLine(String player, double bal, double perTick) {
		return String.format(java.util.Locale.ROOT, "[bedrocktol] would-have-flagged %s balance=%.2f budget=%.3f/tick", player, bal, perTick);
	}

	public static boolean flagged(double bal) { return bal >= LIMIT; }
}
