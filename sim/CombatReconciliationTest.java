import dev.meshac.CombatReconciliation;
import static dev.meshac.CombatReconciliation.Decision.*;
public final class CombatReconciliationTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  CombatReconciliation r=new CombatReconciliation();
  for(int i=0;i<51;i++)r.position(i*50_000_000L,i*50_000_000L,0,i,70,0);
  var stalled=r.attack(2_500_000_000L,2_500_000_000L,0,2,-1,-1);
  var c=r.context(stalled,2_500_000_000L);check(c.positions().size()==51,"2.5s time history");check(c.decision()==AMBIGUOUS_TRANSPORT,"unknown attack timestamp must not allow old hit");
  var grounded=r.attack(2_500_000_001L,2_500_000_001L,0,2,2_000_000_000L,2_500_000_000L);
  check(r.context(grounded,2_500_000_001L).decision()==CHECK_ORIGINAL_WINDOW,"grounded existing window only");
  var old=r.attack(2_500_000_002L,2_500_000_002L,0,2,500_000_000L,2_500_000_000L);
  check(r.context(old,2_500_000_002L).decision()==AMBIGUOUS_TRANSPORT,"2s interval must not widen rewind");
  var dimension=r.attack(2_500_000_003L,2_500_000_003L,1,2,-1,-1);
  check(r.context(dimension,2_500_000_003L).decision()==INSUFFICIENT_HISTORY,"dimension isolation");
  r=new CombatReconciliation();for(int i=0;i<1000;i++)r.position(i,i,0,0,0,0);
  check(r.positionCount()==256,"burst memory bound");
  CombatReconciliation.Attack first=null;for(int i=0;i<1000;i++){var a=r.attack(2000+i,2000+i,0,1,-1,-1);if(i==0)first=a;}
  check(r.attackCount()==64,"attack memory bound");check(!r.context(first,3000).attackRetained(),"overflow not silently replayable");
  r.context(first,3_000_000_000L);check(r.positionCount()==0 && r.attackCount()==0,"expire by time, not ticks");
  r.position(100,100,0,0,0,0);r.position(50,50,1,0,0,0);check(r.positionCount()==1,"clock discontinuity clears");
  try{r.attack(100,101,0,1,-1,-1);throw new AssertionError("future receive");}catch(IllegalArgumentException expected){}
  r.clear();check(r.positionCount()==0 && r.attackCount()==0,"disconnect cleanup");
  System.out.println("CombatReconciliationTest: time retention, bounded bursts, original rewind, ambiguous stall, loss, dimension and expiry PASS");
 }
}
