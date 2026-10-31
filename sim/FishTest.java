package dev.meshac;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;

/** Real Fish with scripted nibble/clock/latency inputs, not native fishing proof. */
public class FishTest {
 static int checks;
 static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static final MinecraftServer server=new MinecraftServer();
 static ServerPlayer player(){var p=new ServerPlayer();server.players.add(p);return p;}
 static void tick(){Fish.tick(server);}
 static void bite(ServerPlayer p){p.fishing=new FishingHook();p.fishing.nibble=1;TestClock.ms+=1000;tick();}
 static void reel(ServerPlayer p,long gap,int latency){TestClock.ms+=gap;p.connection.ms=latency;Fish.use(p);p.fishing.nibble=0;}
 static void fast(ServerPlayer p){bite(p);reel(p,99,0);}
 static void noSignal(String m){ck(Verdict.calls==0,m);}
 static void resetSignals(){Verdict.calls=0;}
 public static void main(String[] args){
  var a=player();Fish.use(a);noSignal("unseen/no hook does not signal");tick();noSignal("absent hook tick does not signal");
  a.fishing=new FishingHook();Fish.use(a);noSignal("hook without observed bite does not signal");
  for(int i=0;i<3;i++){fast(a);noSignal("first three fast bites stay silent #"+i);Fish.use(a);noSignal("same bite cannot count twice #"+i);}
  fast(a);ck(Verdict.calls==1,"fourth fast bite signals once");ck(Verdict.detail.contains("99 ms")&&Verdict.detail.contains("4 times"),"signal describes clamped reaction and streak");
  resetSignals();for(int i=0;i<3;i++){fast(a);noSignal("signal clears streak #"+i);}fast(a);ck(Verdict.calls==1,"second four-fast streak signals");
  resetSignals();Fish.forget(a.getUUID());for(int i=0;i<3;i++)fast(a);
  bite(a);reel(a,100,0);noSignal("exact100ms reaction is slow and resets streak");
  for(int i=0;i<3;i++){fast(a);noSignal("slow reel reset #"+i);}fast(a);ck(Verdict.calls==1,"four new fast bites after reset signal");
  resetSignals();Fish.forget(a.getUUID());
  for(int i=0;i<3;i++){bite(a);reel(a,399,300);noSignal("300ms latency subtraction #"+i);}
  bite(a);reel(a,399,300);ck(Verdict.calls==1,"399-300=99ms qualifies");
  resetSignals();Fish.forget(a.getUUID());for(int i=0;i<3;i++)fast(a);
  bite(a);reel(a,400,1000);noSignal("latency capped300:400-300=100 resets");
  for(int i=0;i<3;i++){fast(a);noSignal("upper latency clamp reset #"+i);}fast(a);ck(Verdict.calls==1,"upper clamp allows later streak");
  resetSignals();Fish.forget(a.getUUID());for(int i=0;i<3;i++)fast(a);
  bite(a);reel(a,99,-100);ck(Verdict.calls==1,"negative latency clamps0 rather than extending reaction");
  resetSignals();Fish.forget(a.getUUID());for(int i=0;i<3;i++)fast(a);
  bite(a);reel(a,0,300);ck(Verdict.calls==1&&Verdict.detail.contains("0 ms"),"negative computed reaction displayed0");
  resetSignals();Fish.forget(a.getUUID());
  bite(a);TestClock.ms+=500;tick();reel(a,0,0);noSignal("continued nibble must not refresh bite start");
  for(int i=0;i<3;i++){fast(a);noSignal("continued-nibble slow reset #"+i);}fast(a);ck(Verdict.calls==1,"fresh nibble streak after slow reaction");
  resetSignals();Fish.forget(a.getUUID());bite(a);a.fishing.nibble=0;tick();reel(a,0,0);noSignal("nibble ending disarms bite");
  a.fishing.nibble=1;tick();reel(a,99,0);for(int i=0;i<2;i++)fast(a);noSignal("new nibble edge arms one event");fast(a);ck(Verdict.calls==1,"new nibble edge counted");
  resetSignals();Fish.forget(a.getUUID());bite(a);a.fishing=null;tick();a.fishing=new FishingHook();Fish.use(a);noSignal("hook absence clears pending bite");
  for(int i=0;i<3;i++){fast(a);noSignal("absent-hook no phantom event #"+i);}fast(a);ck(Verdict.calls==1,"four actual events after absent hook");
  resetSignals();Fish.forget(a.getUUID());var b=player();for(int i=0;i<3;i++)fast(a);fast(b);noSignal("UUID streaks isolated");
  Fish.forget(b.getUUID());fast(a);ck(Verdict.calls==1,"forget another UUID preserves streak");
  resetSignals();for(int i=0;i<3;i++)fast(b);Fish.forget(b.getUUID());fast(b);noSignal("forget removes own streak");
  bite(b);Fish.forget(b.getUUID());reel(b,0,0);noSignal("forget disarms pending bite");
  var c=player();c.creative=true;for(int i=0;i<8;i++){bite(c);reel(c,0,0);noSignal("creative use ignored #"+i);}Fish.forget(c.getUUID());
  System.out.println("PASS fish-state checks="+checks);
 }
}
