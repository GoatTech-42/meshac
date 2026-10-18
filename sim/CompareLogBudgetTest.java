import dev.meshac.CompareLogBudget;
public final class CompareLogBudgetTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  CompareLogBudget b=new CompareLogBudget();CompareLogBudget.Caller[] c=new CompareLogBudget.Caller[40];int lines=0;
  for(int i=0;i<40;i++){c[i]=new CompareLogBudget.Caller();if(b.take(c[i],100))lines++;}
  ck(lines==4 && b.suppressed()==36,"40player same tick capped4");
  ck(!b.take(c[0],101) && c[0].suppressed()==1,"caller suppression counted");
  ck(!b.take(c[0],120),"global reset doesn't bypass caller100tick cooldown");
  ck(b.take(c[4],120),"suppressed caller wasn't marked emitted");
  ck(c[4].suppressed()==1,"suppression cumulative not lost on successful emit");
  ck(b.take(c[0],200),"caller exact100tick boundary");
  ck(b.take(c[0],1),"server tick rollback/new server reset");
  ck(!b.take(c[0],1),"duplicate tick bounded");
  CompareLogBudget dense=new CompareLogBudget();int total=0;
  for(int t=0;t<1200;t++)for(int i=0;i<40;i++)if(dense.take(c[i],t))total++;
  ck(total<=240,"global minute max240 at20TPS");
  System.out.println("CompareLogBudgetTest: global burst, caller/tick windows, suppression and rollback PASS");
 }
}
