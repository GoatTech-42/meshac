package dev.meshac;

import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Scripted lifecycle checks against real Movement and existing doubles.
 * This does not validate reconnect hooks, threads, native clients or physics. */
public class LifecycleLogTest {
 static int checks;
 static void ck(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
 static void advance(long ms){TestClock.ms+=ms;TestClock.ns+=ms*1000000;}
 static ServerboundMovePlayerPacket pos(double x){return ServerboundMovePlayerPacket.pos(x,70,0,true);}
 static void check(ServerPlayer player,double x){ck(Movement.check(player,pos(x))==null,"every lifecycle packet must remain log-only");player.setPos(x,70,0);}
 static ServerPlayer fresh(){Level.reset();Config.enforce=true;Verdict.next=Verdict.Step.LOG;Verdict.signals.clear();var p=new ServerPlayer("lifecycle");p.setPos(0,70,0);for(int i=0;i<120;i++){advance(50);check(p,0);}Verdict.signals.clear();return p;}
 static void queueBlink(ServerPlayer p){
  double x=0;
  for(int i=0;i<3;i++){advance(50);Movement.arrive(p,pos(x+=0.2));Movement.tickEnd(p);check(p,x);}
  for(int i=0;i<12;i++){advance(50);Movement.tickEnd(p);}
  Movement.arrive(p,pos(x));
  for(int i=0;i<6;i++){advance(10);Movement.arrive(p,pos(x));}
 }
 static long blink(){return Verdict.signals.stream().filter(s->s.startsWith("blink")).count();}
 public static void main(String[] args){
  // Positive control proves the exact queued state is actionable before cleanup.
  var positive=fresh();queueBlink(positive);check(positive,0.6);
  ck(blink()==1,"queued blink must signal without forget");
  // Forget must discard already-earned, not merely warmup, pending work.
  var forgotten=fresh();queueBlink(forgotten);Movement.forget(forgotten.getUUID());check(forgotten,0.6);
  ck(blink()==0,"forget must discard queued blink before reused player identity checks again");
  // Forgetting another UUID must not erase this player's queued signal.
  var keeper=fresh();queueBlink(keeper);Movement.forget(java.util.UUID.randomUUID());check(keeper,0.6);
  ck(blink()==1,"forget of another UUID must preserve queued blink");
  // A second player's check must not consume the first player's pending signal.
  var first=fresh();queueBlink(first);var second=new ServerPlayer("other");second.setPos(0,70,0);
  check(second,0);ck(blink()==0,"queued blink must stay scoped to originating UUID");
  check(first,0.6);ck(blink()==1,"other player's check must not consume pending blink");
  Movement.forget(first.getUUID());Movement.forget(second.getUUID());
  System.out.println("PASS lifecycle-log checks="+checks);
 }
}
