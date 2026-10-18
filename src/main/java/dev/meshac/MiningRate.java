package dev.meshac;
/** Progress comes from BlockState.getDestroyProgress(player, level, pos), not hardness alone. */
public final class MiningRate {
 private MiningRate() {}
 /** Existing slow-block allowance is8/s. One-tick blocks have a bounded20/s allowance.
  * Two-tick blocks interpolate to10/s; invalid/unknown progress retains the old cost. */
 public static double cost(float progress) {
  if (progress == Float.POSITIVE_INFINITY) return 0.4; // zero-hardness blocks may divide by zero
  if (!Float.isFinite(progress) || progress <= 0) return 1;
  double ticks = Math.ceil(1.0 / progress);
  return Math.min(1.0, Math.max(1.0, ticks) / 2.5);
 }
 public static boolean exceeded(double work) { return work > 8.000001; }
}
