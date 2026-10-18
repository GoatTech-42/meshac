import dev.meshac.ActionTraceBudget;
public final class ActionTraceBudgetTest {
 static void ck(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){ActionTraceBudget b=new ActionTraceBudget();
  for(int i=0;i<200;i++)ck(b.take(1));ck(!b.take(1));ck(b.dropped()==1);
  ck(b.take(1001));ck(b.take(0));ck(ActionTraceBudget.text("x\ny\rz").equals("x y z"));
  ck(ActionTraceBudget.text("x".repeat(1000)).length()==512);
  System.out.println("ActionTraceBudgetTest: global200/s cap, drop count, reset and bounded single-line text PASS");
 }
}
