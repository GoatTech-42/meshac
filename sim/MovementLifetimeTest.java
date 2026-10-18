import dev.meshac.MovementLifetime;
import dev.meshac.CombatEvidence;
public final class MovementLifetimeTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  ck(!MovementLifetime.reusable(10,10,"world","world",false),"mace death queued packets cannot reuse launch/fall/noClip integrator");
  ck(!MovementLifetime.reusable(10,11,"world","world",true),"respawn new entity same UUID resets old fallPeak/nfAt/arrival budget");
  ck(!MovementLifetime.reusable(11,11,"world","nether",true),"dimension reset");
  ck(MovementLifetime.reusable(11,11,"world","world",true),"living knockback does NOT exempt checks");
  ck(CombatEvidence.closest(12,2.9)==2.9,"Enderman target teleport: bounded historical legal reach retained");
  ck(CombatEvidence.eitherAim(false,true),"target teleport old coherent aim candidate retained");
  ck(!CombatEvidence.recentTick(100,80,15),"teleported target doesn't widen history");
  System.out.println("MovementLifetimeTest: death/respawn isolation, live knockback and target-teleport controls PASS");
 }
}
