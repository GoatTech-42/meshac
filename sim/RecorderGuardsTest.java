import dev.meshac.CombatRecorder;
import dev.meshac.CallerCommandBudget;
import java.util.UUID;
public final class RecorderGuardsTest {
 static void check(boolean b,String why){if(!b)throw new AssertionError(why);}
 public static void main(String[]args){
  CallerCommandBudget b=new CallerCommandBudget();for(int i=0;i<4;i++)check(b.allow("a",i),"four pages");check(!b.allow("a",5),"cap");check(b.allow("b",5),"caller isolation");check(b.allow("a",60_000_000_000L),"minute reset");
  b=new CallerCommandBudget();for(int i=0;i<64;i++)check(b.allow("caller"+i,0),"slots");check(!b.allow("overflow",0)&&b.size()==64,"bounded caller map");
  var r=new CombatRecorder(()->false);try{r.start(UUID.randomUUID(),0);throw new AssertionError("cross-thread allowed");}catch(IllegalStateException expected){}
  try{r.page(UUID.randomUUID(),0,0);throw new AssertionError("cross-thread dump");}catch(IllegalStateException expected){}
  System.out.println("RecorderGuardsTest: caller isolation, limits, bounded keys and thread guards PASS");
 }
}
