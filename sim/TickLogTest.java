package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
/** Positive control round 3: server-tick cheats. AntiKnockback (three melee hits, no push)
    and Blink (movement packets held back while tick-ends continue, then released in one
    burst) must signal through the real Movement.tick/arrive/check against the same doubles.
    Honest knockback movement, frozen-client stalls and tunnel bursts must stay silent.
    Every check() result, including warmup, is asserted null in log-only mode. */
public class TickLogTest {
 static int checks;
 static final java.util.List<double[]> hist=new java.util.ArrayList<>();
 static void ck(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void advance(long ms){TestClock.ms+=ms;TestClock.ns+=ms*1000000;}
 static ServerboundMovePlayerPacket pos(double x,double y,double z,boolean g){return ServerboundMovePlayerPacket.pos(x,y,z,g);}
 static void check(ServerPlayer p,double x,double y,double z,boolean g){double[] r=Movement.check(p,pos(x,y,z,g));p.setPos(x,y,z);hist.add(r);}
 static void allNull(int from,String m){for(int i=from;i<hist.size();i++){checks++;if(hist.get(i)!=null)throw new AssertionError(m+": packet#"+i+" corrected in log-only mode");}}
 static ServerPlayer fresh(){Level.reset();Config.enforce=true;Verdict.next=Verdict.Step.LOG;Verdict.signals.clear();hist.clear();var p=new ServerPlayer("test");p.setPos(0,70,0);for(int i=0;i<120;i++)check(p,0,70,0,true);allNull(0,"warmup");Verdict.signals.clear();return p;}
 static long sig(String prefix){return Verdict.signals.stream().filter(x->x.startsWith(prefix)).count();}
 static void walk(ServerPlayer p,double[] x,int n){for(int i=0;i<n;i++){advance(50);Movement.arrive(p,pos(x[0]+=0.2,70,0,true));Movement.tickEnd(p);check(p,x[0],70,0,true);}}
 public static void main(String[] a)throws Exception{
  // 1. AntiKnockback: three melee hits, the player never moves.
  {
   var p=fresh();
   p.hurtTime=10;p.lastDamage=new DamageSource(new LivingEntity(),null,"player");
   for(int round=0;round<3;round++){Movement.tick(p);advance(500);Movement.tick(p);}
   ck(sig("antiknockback")>0,"ignoring knockback flagged as antiknockback: "+Verdict.signals);
  }
  // 2. Honest knockback: the player is pushed each time.
  {
   var p=fresh();double x=0;
   p.hurtTime=10;p.lastDamage=new DamageSource(new LivingEntity(),null,"player");
   for(int round=0;round<3;round++){Movement.tick(p);p.setPos(x+=0.5,70,0);advance(500);Movement.tick(p);}
   ck(Verdict.signals.isEmpty(),"honest knockback movement stays silent: "+Verdict.signals);
  }
  // 3. Blink: packets held while tick-ends continue, then released in one burst.
  {
   var p=fresh();double[] x={0};int m=hist.size();
   walk(p,x,3);
   for(int i=0;i<12;i++){advance(50);Movement.tickEnd(p);} // 600 ms of silence, ticks continue
   Movement.arrive(p,pos(x[0],70,0,true));                 // first held packet lands: 600 ms gap
   for(int i=0;i<6;i++){advance(10);Movement.arrive(p,pos(x[0],70,0,true));} // the rest in one 60 ms burst
   for(int i=0;i<7;i++)check(p,x[0],70,0,true);
   ck(sig("blink")>0,"held-then-burst packets flagged as blink: "+Verdict.signals);
   allNull(m,"blink LOG");
  }
  // 4. Honest frozen client: movement AND tick-ends stall together, then resume.
  {
   var p=fresh();double[] x={0};int m=hist.size();
   walk(p,x,3);
   advance(600);                                            // everything quiet: no ticks counted
   Movement.arrive(p,pos(x[0],70,0,true));check(p,x[0],70,0,true);
   walk(p,x,3);
   ck(Verdict.signals.isEmpty(),"frozen-client stall stays silent: "+Verdict.signals);
   allNull(m,"frozen stall LOG");
  }
  // 5. Honest tunnel burst: held packets land close together but the client's ticks stalled too.
  {
   var p=fresh();double[] x={0};int m=hist.size();
   walk(p,x,3);
   advance(600);
   for(int i=0;i<7;i++){advance(10);Movement.arrive(p,pos(x[0],70,0,true));check(p,x[0],70,0,true);}
   ck(Verdict.signals.isEmpty(),"tunnel burst stays silent: "+Verdict.signals);
   allNull(m,"tunnel burst LOG");
  }
  System.out.println("PASS tick-positive-control checks="+checks);
 }
}
