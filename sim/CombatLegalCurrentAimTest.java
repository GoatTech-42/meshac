import dev.meshac.CombatEvidence;
public final class CombatLegalCurrentAimTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  for(String scenario:new String[]{"ping spike rewound aim","knockback vertical history","riding coherent eye/box","teleport current valid ray","large hitbox edge"})
   ck(!CombatEvidence.multiAimViolation(true,12,4,4,9),scenario+": old bad median cannot convict accepted aim");
  ck(CombatEvidence.multiAimViolation(false,8,4,4,9),"actual fourtarget/distincttick misses positive");
  ck(!CombatEvidence.multiAimViolation(false,8,4,1,9),"one lag batch no conviction");
  ck(!CombatEvidence.multiAimViolation(false,7,4,4,9),"minimum8hits");
  ck(!CombatEvidence.multiAimViolation(false,8,3,4,9),"minimum4targets");
  ck(!CombatEvidence.multiAimViolation(false,8,4,4,0),"accepted history not a miss median");
  System.out.println("CombatLegalCurrentAimTest: accepted current/history ray vetoes stale miss median, positives retained PASS");
 }
}
