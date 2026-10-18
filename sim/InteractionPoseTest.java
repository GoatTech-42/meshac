import dev.meshac.InteractionPose;
public final class InteractionPoseTest {
 static void ck(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  InteractionPose p=InteractionPose.of(1000,"world",1,1.62,0,0,0);
  ck(p.endX(8)==1 && p.endZ(8)==8); // saved forward ray at eyeX1, not moved eyeX3
  ck(p.x()!=3);ck(p.valid(1750,"world"));ck(!p.valid(1751,"world"));
  ck(!p.valid(999,"world"));ck(!p.valid(1001,"nether"));
  InteractionPose behind=InteractionPose.of(1000,"world",1,1.62,0,180,0);
  ck(behind.endZ(8)<0); // behind-target remains a miss, not universal acceptance
  System.out.println("InteractionPoseTest: coherent saved eye/ray, age/dimension and behind control PASS");
 }
}
