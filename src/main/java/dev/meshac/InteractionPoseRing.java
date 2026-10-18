package dev.meshac;
/** Fixed primitive ring. Rotation writes allocate no pose, arrays or Vec3. */
public final class InteractionPoseRing {
 private final long[] at; private final Object[] dim;
 private final double[] x,y,z,dx,dy,dz; private final boolean[] present;
 private int next;
 public InteractionPoseRing(int capacity){
  if(capacity<1)throw new IllegalArgumentException("capacity");
  at=new long[capacity];dim=new Object[capacity];x=new double[capacity];y=new double[capacity];z=new double[capacity];
  dx=new double[capacity];dy=new double[capacity];dz=new double[capacity];present=new boolean[capacity];
 }
 public void add(long time,Object dimension,double ex,double ey,double ez,float yaw,float pitch){
  int i=next;next=(next+1)%at.length;at[i]=time;dim[i]=dimension;x[i]=ex;y[i]=ey;z[i]=ez;
  double yr=Math.toRadians(yaw),pr=Math.toRadians(pitch);
  dx[i]=-Math.sin(yr)*Math.cos(pr);dy[i]=-Math.sin(pr);dz[i]=Math.cos(yr)*Math.cos(pr);present[i]=true;
 }
 public int capacity(){return at.length;}
 public boolean valid(int i,long now,Object dimension){return present[i]&&CombatEvidence.recentTick(now,at[i],750)&&java.util.Objects.equals(dimension,dim[i]);}
 public double x(int i){return x[i];}public double y(int i){return y[i];}public double z(int i){return z[i];}
 public double endX(int i,double range){return x[i]+dx[i]*range;}
 public double endY(int i,double range){return y[i]+dy[i]*range;}
 public double endZ(int i,double range){return z[i]+dz[i]*range;}
}
