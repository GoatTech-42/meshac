package dev.meshac;

/** Small pure guards shared by action checks; no timing claim authenticates human input. */
public final class ActionEvidence {
 private ActionEvidence() {}
 public static boolean steadyGap(long gap, long previous) { return gap >= 20 && gap < 160 && previous >= 20 && Math.abs(gap - previous) < 8; }
 public static boolean cancel(boolean enforce, boolean logOnly) { return enforce && !logOnly; }
 /** Packet duplicates in one handling tick must not multiply vertical-rise evidence. */
 public static final class Rise {
  private int entity = Integer.MIN_VALUE, count; private long lastTick = Long.MIN_VALUE;
  public int sample(int entityId, long tick, boolean unsupportedRise) {
   if (entity != entityId || tick < lastTick || !unsupportedRise) { count = 0; lastTick = Long.MIN_VALUE; }
   entity = entityId;
   if (!unsupportedRise) return 0;
   if (tick != lastTick) { count++; lastTick = tick; }
   return count;
  }
 }
}
