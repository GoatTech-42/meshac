package dev.meshac;
/** Share the EXISTING auto-step acceptance ceiling between accumulator and direct checks. */
public final class StepEvidence {
 private StepEvidence() {}
 public static double ceiling(boolean jumpBoost){return 0.66 + (jumpBoost?0.3:0);}
 public static boolean autoStep(boolean ground,double dy,boolean jumpBoost){return ground&&dy>=0.42&&dy<=ceiling(jumpBoost);}
 public static boolean excessive(boolean ground,double dy,boolean jumpBoost){return ground&&dy>ceiling(jumpBoost);}
}
