import dev.meshac.MiningRate;
/** Hardness fixtures: minecraft.wiki/w/Breaking and /w/Instant_mining.
 * Actual hook uses server getDestroyProgress, this formula only constructs test fixtures. */
public final class MiningRateTest {
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 static float progress(double hardness,double speed,boolean harvest){return hardness==0?Float.POSITIVE_INFINITY:(float)(speed/hardness/(harvest?30:100));}
 static double work(float p,int n){double w=0;for(int i=0;i<n;i++)w+=MiningRate.cost(p);return w;}
 public static void main(String[] args){
  float stone=progress(1.5,8,true); // diamond, no efficiency/haste
  float effStone=progress(1.5,(8+26)*1.4,true); // efficiencyV+hasteII
  float dirt=progress(.5,8+10,true); // diamond shovel efficiencyIII
  float torch=progress(0,1,true);
  ck(stone<1 && effStone>1 && dirt>1,"real hardness fixtures discriminate progress");
  for(float p:new float[]{effStone,dirt,torch}){
   ck(!MiningRate.exceeded(work(p,20)),"legal20tick insta rate, no hardness exemption");
   ck(MiningRate.exceeded(work(p,21)),"insta automation21/s still exceeds bounded cap");
  }
  ck(!MiningRate.exceeded(work(stone,8)),"slow baseline8 retained");
  ck(MiningRate.exceeded(work(stone,9)),"slow9 remains positive");
  ck(!MiningRate.exceeded(work(.5f,10)),"two-tick10/s allowance");
  ck(MiningRate.exceeded(work(.5f,11)),"two-tick11 positive");
  ck(MiningRate.cost(Float.NaN)==1 && MiningRate.cost(0)==1 && MiningRate.cost(-1)==1,"unknown retains conservative existing weight");
  ck(!MiningRate.exceeded(work(effStone,10)+work(stone,4)),"mixed block work");
  System.out.println("MiningRateTest: stone/dirt/torch hardness, efficiencyV+hasteII, mixed work and positives PASS");
 }
}
