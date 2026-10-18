package dev.meshac;
/** Coherent eye and rotation observation; no mixing old rotation with a new eye. */
public record InteractionPose(long at, Object dimension, double x, double y, double z, double dx, double dy, double dz) {
 public static InteractionPose of(long at,Object dimension,double x,double y,double z,float yaw,float pitch){
  double yr=Math.toRadians(yaw),pr=Math.toRadians(pitch);
  return new InteractionPose(at,dimension,x,y,z,-Math.sin(yr)*Math.cos(pr),-Math.sin(pr),Math.cos(yr)*Math.cos(pr));
 }
 public boolean valid(long now,Object dim){return CombatEvidence.recentTick(now,at,750)&&java.util.Objects.equals(dim,dimension);}
 public double endX(double range){return x+dx*range;}
 public double endY(double range){return y+dy*range;}
 public double endZ(double range){return z+dz*range;}
}
