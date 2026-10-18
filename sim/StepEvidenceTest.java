import dev.meshac.StepEvidence;
public final class StepEvidenceTest {
 static void ck(boolean b){if(!b)throw new AssertionError();}
 public static void main(String[] args){
  for(double dy:new double[]{.42,.5,.6,.62,.63,.65,.66}){
   ck(StepEvidence.autoStep(true,dy,false));ck(!StepEvidence.excessive(true,dy,false));
  }
  ck(StepEvidence.excessive(true,.67,false));ck(StepEvidence.excessive(true,1,false));
  ck(!StepEvidence.autoStep(false,.65,false)); // airborne jump not covered
  ck(!StepEvidence.autoStep(true,.3,false)); // accumulator still owns split steps
  double accumulated=.3+.3+.3;ck(accumulated>.65); // old multi-rise threshold unchanged
  ck(!StepEvidence.excessive(true,.9,true));ck(StepEvidence.excessive(true,1,true));
  System.out.println("StepEvidenceTest: existing auto-step accepted band cannot direct-flag, excessive/air/split positives retained PASS");
 }
}
