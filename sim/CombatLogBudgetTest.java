import dev.meshac.CombatLogBudget;
import static dev.meshac.CombatLogBudget.Kind.*;
public final class CombatLogBudgetTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[] args){
  CombatLogBudget b=new CombatLogBudget();int evidence=0,summary=0,trace=0;
  for(int i=0;i<60000;i++){if(b.allow(EVIDENCE,i))evidence++;if(b.allow(SUMMARY,i))summary++;if(b.allow(TRACE,i))trace++;}
  check(evidence==12 && summary==6 && trace==120,"fixed volume cap");
  check(b.takeSuppressed(EVIDENCE)==59988,"dropped count");check(b.takeSuppressed(EVIDENCE)==0,"drop drain");
  check(!b.allow(EVIDENCE,59_999_999_999L),"not reset early");
  check(b.allow(EVIDENCE,60_000_000_000L),"minute boundary");
  check(b.allow(EVIDENCE,1),"clock reset must not starve indefinitely");
  CombatLogBudget another=new CombatLogBudget();check(another.allow(TRACE,1),"player isolation");
  System.out.println("CombatLogBudgetTest:7 assertions PASS");
 }
}
