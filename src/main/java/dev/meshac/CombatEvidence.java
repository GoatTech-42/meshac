package dev.meshac;

/** Small evidence reducers shared by live combat checks and pure tests. */
public final class CombatEvidence {
	private CombatEvidence() {}
	/** Server-tick age only, no claim about client clock or network delay. */
	public static boolean recentTick(long now, long sample, int maxAge) {
		return sample <= now && now - sample <= maxAge;
	}
 /** Dimension+stable entity identity bind a historical observation, not a recycled numeric id. */
 public static boolean sameSubject(Object dimension, Object sampledDimension, Object identity, Object sampledIdentity) {
  return java.util.Objects.equals(dimension, sampledDimension) && java.util.Objects.equals(identity, sampledIdentity);
 }
	/** Historical samples may add a legal candidate, never erase the current one. */
	public static double closest(double currentBest, double historical) {
		return Math.min(currentBest, historical);
	}
	/** An accepted ray at either coherent eye/box pair remains accepted. */
	public static boolean eitherAim(boolean current, boolean historical) {
		return current || historical;
	}
 /** A rolling history of misses cannot turn a currently accepted ray into a conviction. */
 public static boolean multiAimViolation(boolean accepted, int hits, int targets, int ticks, double medianMiss) {
  return !accepted && hits >= 8 && targets >= 4 && ticks >= 4 && medianMiss > 0.9;
 }
	/** Consecutive evidence ends on every nonqualifying sample. */
	public static int nextConsecutive(int previous, boolean qualifies) {
		return qualifies ? previous + 1 : 0;
	}
}
