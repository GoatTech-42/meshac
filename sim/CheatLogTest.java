package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.Level;
/** Positive control round 2: speed, timer, step and microhop cheats must signal through
    the real Movement.check; honest walking and slow (laggy) cadence must stay silent.
    Same doubles and clock rebind as NoFallLogTest/FlyLogTest. Log-only must record and
    NEVER correct: every single packet result, including warmup, is asserted null.
    HOLD mode sets back. */
public class CheatLogTest {
 static int checks;
 static final java.util.List<double[]> hist=new java.util.ArrayList<>();
 static void ck(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static double[] sendAt(ServerPlayer p,double x,double y,double z,boolean g,long stepMs){TestClock.ms+=stepMs;TestClock.ns+=stepMs*1000000;double[] r=Movement.check(p,ServerboundMovePlayerPacket.pos(x,y,z,g));p.setPos(x,y,z);hist.add(r);return r;}
 static double[] send(ServerPlayer p,double x,double y,double z,boolean g){return sendAt(p,x,y,z,g,50);}
 static void allNull(int from,String m){for(int i=from;i<hist.size();i++){checks++;if(hist.get(i)!=null)throw new AssertionError(m+": packet#"+i+" corrected in log-only mode");}}
 static ServerPlayer fresh(){Level.reset();Config.enforce=true;Verdict.next=Verdict.Step.LOG;Verdict.signals.clear();hist.clear();var p=new ServerPlayer("test");p.setPos(0,70,0);for(int i=0;i<120;i++)send(p,0,70,0,true);allNull(0,"warmup");Verdict.signals.clear();return p;}
 static long sig(String prefix){return Verdict.signals.stream().filter(x->x.startsWith(prefix)).count();}
 public static void main(String[] a)throws Exception{
  // 1. Airborne speed above the vanilla cap.
  {
   var p=fresh();double y=70;double x=0;int m=hist.size();
   send(p,x,y+=0.42,0,false);
   for(int i=0;i<8;i++)send(p,x+=0.8,y,0,false);
   ck(sig("speed")>0,"airborne speed above cap flagged: "+Verdict.signals);
   allNull(m,"speed LOG");
  }
  // 2. Timer: packets arriving every 25 ms claim 50 ms each.
  {
   var p=fresh();int m=hist.size();
   for(int i=0;i<60;i++)sendAt(p,0,70,0,true,25);
   ck(sig("timer")>0,"fast cadence flagged as timer: "+Verdict.signals);
   allNull(m,"timer LOG");
  }
  // 3. Step hack: several ground rises above the microhop band adding past one block.
  {
   var p=fresh();double y=70;int m=hist.size();
   for(int i=0;i<3;i++)send(p,0,y+=0.31,0,true);
   ck(sig("step")>0,"ground step accumulation flagged: "+Verdict.signals);
   ck(sig("microhop")==0&&sig("jump")==0,"step run flags only step: "+Verdict.signals);
   allNull(m,"step LOG");
  }
  // 4. Microhop: repeated small hops claiming ground over open air.
  {
   var p=fresh();double y=70;Level.support=false;int m=hist.size();
   send(p,0,y+=0.2,0,true);
   send(p,0,y+=0.2,0,true);
   ck(sig("microhop")>0,"repeated micro hops flagged: "+Verdict.signals);
   allNull(m,"microhop LOG");
  }
  // 5. Honest ground walking stays silent.
  {
   var p=fresh();double x=0;int m=hist.size();
   for(int i=0;i<40;i++)send(p,x+=0.2,70,0,true);
   ck(Verdict.signals.isEmpty(),"honest walking stays silent: "+Verdict.signals);
   allNull(m,"walk LOG");
  }
  // 6. Honest slow cadence (laggy client) trips no timer.
  {
   var p=fresh();int m=hist.size();
   for(int i=0;i<30;i++)sendAt(p,0,70,0,true,80);
   ck(Verdict.signals.isEmpty(),"honest slow cadence stays silent: "+Verdict.signals);
   allNull(m,"slow cadence LOG");
  }
  // 7. Enforcing mode: the same air speed run sets the player back.
  {
   var p=fresh();Verdict.next=Verdict.Step.HOLD;double y=70;double x=0;
   send(p,x,y+=0.42,0,false);
   double[] r=null;for(int i=0;i<8&&r==null;i++)r=send(p,x+=0.8,y,0,false);
   ck(sig("speed")>0,"enforcing speed still signals: "+Verdict.signals);
   ck(r!=null,"enforcing speed returns a setback");
  }
  System.out.println("PASS cheat-positive-control checks="+checks);
 }
}
