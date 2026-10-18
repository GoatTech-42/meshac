import dev.meshac.CombatEvidence;
public final class MiningLookRefreshTest {
 static void ck(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  long aimTick=-1000; // began dig looking slightly away
  ck(!CombatEvidence.recentTick(100,aimTick,10));
  aimTick=101; // rotation packet now aims at actual dig block
  ck(CombatEvidence.recentTick(102,aimTick,10)); // break after subsequent flick
  ck(!CombatEvidence.recentTick(112,aimTick,10)); // aim doesn't exempt forever
  ck(!CombatEvidence.recentTick(90,aimTick,10)); // reset/future stamp not accepted
  System.out.println("MiningLookRefreshTest: refreshed aim, expiry, reset and off-target control PASS");
 }
}
