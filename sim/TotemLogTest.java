package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
/** Real Totem with scripted clock and verdict response, not native inventory hooks. */
public class TotemLogTest {
 static int checks;
 static void ck(boolean v,String why){checks++;if(!v)throw new AssertionError(why);}
 static long now=100000;
 static ServerPlayer fresh(){Verdict.calls=0;return new ServerPlayer();}
 static boolean popFill(ServerPlayer p,long delay){TestClock.ms=now;Totem.popped(p);now+=delay;return Totem.refuse(p,now,45,true);}
 public static void main(String[] args){
  for(Verdict.Step step:Verdict.Step.values()){
   var p=fresh();Verdict.next=step;
   ck(popFill(p,10)==(step!=Verdict.Step.LOG),"10ms pop-fill refusal must follow "+step);
   ck(Verdict.calls==1,"impossible pop-fill must signal exactly once");Totem.forget(p.getUUID());
   p=fresh();now+=1000;
   ck(!popFill(p,20),"first 20ms anticipation must be forgiven");ck(Verdict.calls==0,"first strike must not signal");now+=1000;
   ck(popFill(p,20)==(step!=Verdict.Step.LOG),"second fast fill refusal must follow "+step);ck(Verdict.calls==1,"second fast strike must signal");
   now+=1000;ck(!popFill(p,100),"normal fill must not refuse");now+=1000;
   ck(!popFill(p,20),"normal fill must reset fast streak");Totem.forget(p.getUUID());
   p=fresh();now+=1000;
   ck(!Totem.refuse(p,now,1,false),"ordinary click must not refuse");
   ck(Totem.refuse(p,now+5,45,true)==(step!=Verdict.Step.LOG),"5ms paired offhand-fill refusal must follow "+step);
   ck(Verdict.calls==1,"impossible pair must signal once");Totem.forget(p.getUUID());
  }
  System.out.println("PASS totem-policy checks="+checks);
 }
}
