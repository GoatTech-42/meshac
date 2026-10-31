package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
/** Real Verdict and Ladder; external case writes, screens and connections are doubles. */
public class VerdictTierTest {
 static int checks;
 static void ck(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void scenario(boolean tierA,boolean skip,boolean perma,double offences,String expected,long minutes) {
  var p=new ServerPlayer(); Config.cur=new Config(); Config.cur.skip=skip;Config.cur.perma=perma;
  Cases.effective=offences;Cases.last=null;Cases.adds=0;
  int weight=tierA?3:2;
  Verdict.Step result=Verdict.Step.SETBACK;
  for(int i=0;i<(tierA?2:4);i++){TestClock.ms+=500;result=Verdict.signal(p,tierA?"fly":"combat","scripted",weight);}
  ck(result==Verdict.Step.REMOVE,"fixture must earn real Ladder removal");
  ck(Cases.last!=null && Cases.last.action.equals(expected),"tierA="+tierA+" skip="+skip+" perma="+perma+" offences="+offences+" expected "+expected+" got "+(Cases.last==null?"none":Cases.last.action));
  ck(Cases.adds==1,"removal must create exactly one case");
  ck(p.connection.calls==1,"removal must disconnect exactly once");
  if(expected.equals("tempban"))ck(Cases.last.until==TestClock.ms+minutes*60000,"tempban duration must match earned rung");
  Verdict.forget(p.getUUID());
 }
 public static void main(String[] args){
  if(args.length>0 && args[0].equals("permanent")) { scenario(true,true,true,4,"ban",0); return; }
  scenario(true,true,false,0,"tempban",10);
  scenario(true,false,false,0,"kick",0);
  scenario(false,true,false,0,"kick",0);
  scenario(true,true,true,4,"ban",0);
  scenario(false,true,true,4,"tempban",2160);
  scenario(true,true,false,4,"tempban",10080);
  var monitor=new ServerPlayer();Config.cur=new Config();Config.cur.monitor=true;Cases.last=null;
  for(int i=0;i<2;i++){TestClock.ms+=500;ck(Verdict.signal(monitor,"fly","scripted",3)==Verdict.Step.LOG,"monitor mode must stay LOG");}
  ck(Cases.last==null && monitor.connection.calls==0,"tier-A evidence must not create monitor-mode removals");
  System.out.println("PASS verdict-tier checks="+checks);
 }
}
