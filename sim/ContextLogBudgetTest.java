import dev.meshac.ContextLogBudget;
public final class ContextLogBudgetTest{
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 public static void main(String[] args){
  var b=new ContextLogBudget();var c=new ContextLogBudget.Caller[40];int n=0;
  for(int i=0;i<40;i++){c[i]=new ContextLogBudget.Caller();if(b.take(c[i],0))n++;}
  ck(n==20&&b.dropped()==20,"global40caller burst capped20");
  ck(!b.take(c[0],1)&&c[0].dropped()==1,"same caller drops tracked");
  ck(!b.take(c[0],1_000_000_000L),"global reset doesn't bypass caller5s");
  ck(b.take(c[20],1_000_000_000L),"suppressed caller eligible nextglobalwindow");
  ck(b.take(c[0],5_000_000_000L),"exact5s caller boundary");
  ck(c[0].dropped()==2,"counts survive success");
  ck(b.take(c[0],-1),"clock rollback reset");
  var run=new ContextLogBudget();int out=0;
  for(int second=0;second<60;second++)for(int i=0;i<40;i++)if(run.take(new ContextLogBudget.Caller(),second*1_000_000_000L))out++;
  ck(out==1200,"reconnect callers still global20/s");
  System.out.println("ContextLogBudgetTest: global20/s, caller5s, dropped counts and rollback PASS");
 }
}
