import dev.meshac.CombatEvidence;
import dev.meshac.CombatMissWindow;
public final class CombatSubjectTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  ck(CombatEvidence.sameSubject("overworld","overworld","a","a"),"same subject valid");
  ck(!CombatEvidence.sameSubject("nether","overworld","a","a"),"dimension no stale LOS/reach");
  ck(!CombatEvidence.sameSubject("overworld","overworld","b","a"),"recycled id cannot borrow blocked/visible box");
  CombatMissWindow w=new CombatMissWindow();
  for(int t=1;t<=3;t++)ck(!w.observe("old UUID",10,t,t*50_000_000L,false),"3miss no flag");
  ck(!w.observe("new UUID",10,4,200_000_000L,false),"same numeric id new entity resets");
  for(int t=5;t<=6;t++)ck(!w.observe("new UUID",10,t,t*50_000_000L,false),"new entity own evidence");
  ck(w.observe("new UUID",10,7,350_000_000L,false),"4distinct newentity misses positive");
  System.out.println("CombatSubjectTest: dimension, stable identity and recycled id evidence isolation PASS");
 }
}
