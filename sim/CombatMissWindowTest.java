import dev.meshac.CombatMissWindow;
import dev.meshac.CombatEvidence;
public final class CombatMissWindowTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  CombatMissWindow w=new CombatMissWindow();
  for(int i=0;i<100;i++)check(!w.observe(1,1,1000+i,false),"coalesced attacks are one tick of evidence");
  w=new CombatMissWindow();
  for(int i=0;i<30;i++){check(!w.observe(1,i*2,1000+i*100,false),"isolated strafe miss");check(!w.observe(1,i*2+1,1050+i*100,true),"legal hit resets miss evidence");}
  w=new CombatMissWindow();for(int i=0;i<20;i++)check(!w.observe(i%2,i,1000+i*100,false),"target switches don't mix");
  w=new CombatMissWindow();for(int i=0;i<3;i++)check(!w.observe(1,i,1000+i*100,false),"below threshold");
  check(w.observe(1,3,1300,false),"four separate-tick same-target misses still flag");
  w=new CombatMissWindow();w.observe(1,1,1,false);w.observe(1,2,2,false);w.observe(1,3,3,false);
  check(!w.observe(1,4,5_000_000_000L,false),"fight gap resets");
  check(CombatEvidence.recentTick(100,99,15),"recent history");
  check(!CombatEvidence.recentTick(100,80,15),"stale history");
  check(!CombatEvidence.recentTick(100,101,15),"future history");
  System.out.println("CombatMissWindowTest: batch, strafe resets, switch isolation, timeout, real misses and history bounds PASS");
 }
}
