import dev.meshac.ActionEvidence;
public final class ActionEvidenceTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  for(int gap: new int[]{0,1,5,10,19}) ck(!ActionEvidence.steadyGap(gap,gap),"batched/subtick gap isn't a reliable beat");
  ck(ActionEvidence.steadyGap(100,100),"100ms script positive retained");
  ck(ActionEvidence.steadyGap(50,55),"bounded regular beat retained");
  ck(!ActionEvidence.steadyGap(100,120),"irregular sorting");
  ck(!ActionEvidence.steadyGap(-1,-1),"clock reversal");
  ck(!ActionEvidence.cancel(false,false),"enforcement off");
  ck(!ActionEvidence.cancel(true,true),"monitor/skipped LOG");
  ck(ActionEvidence.cancel(true,false),"enforced positive");
  ActionEvidence.Rise r=new ActionEvidence.Rise();
  for(int i=0;i<50;i++)ck(r.sample(1,100,true)==1,"lag burst is one tick");
  for(int i=101;i<=104;i++)ck(r.sample(1,i,true)==i-99,"sustained boatfly positive");
  ck(r.sample(2,105,true)==1,"new boat resets");
  ck(r.sample(2,106,false)==0,"water/ground reset");
  ck(r.sample(2,107,true)==1,"new unsupported streak");
  ck(r.sample(2,1,true)==1,"clock rollback reset");
  System.out.println("ActionEvidenceTest: batch, script positive, monitor, vehicle identity and distinct tick PASS");
 }
}
