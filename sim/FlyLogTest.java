package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.Level;
/** Positive control: cheated air movement must be caught, honest arcs must stay silent.
    Feeds scripted packet streams through the real Movement.check against the same doubles
    as NoFallLogTest. Log-only mode must record and NEVER correct: every single packet
    result, including warmup, is asserted null (a correction anywhere fails the suite).
    HOLD mode must set back. */
public class FlyLogTest {
 static int checks;
 static final java.util.List<double[]> hist=new java.util.ArrayList<>();
 static void ck(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static double[] send(ServerPlayer p,double x,double y,double z,boolean g){TestClock.ms+=50;TestClock.ns+=50000000;double[] r=Movement.check(p,ServerboundMovePlayerPacket.pos(x,y,z,g));p.setPos(x,y,z);hist.add(r);return r;}
 static void allNull(int from,String m){for(int i=from;i<hist.size();i++){checks++;if(hist.get(i)!=null)throw new AssertionError(m+": packet#"+i+" corrected in log-only mode");}}
 static ServerPlayer fresh(){Level.reset();Config.enforce=true;Verdict.next=Verdict.Step.LOG;Verdict.signals.clear();hist.clear();var p=new ServerPlayer("test");p.setPos(0,70,0);for(int i=0;i<120;i++)send(p,0,70,0,true);allNull(0,"warmup");Verdict.signals.clear();return p;}
 static long sig(String prefix){return Verdict.signals.stream().filter(x->x.startsWith(prefix)).count();}
 static void arc(ServerPlayer p,double y,double x,double step){double dy=0.42;send(p,x+=step,y+=dy,0,false);while(true){dy=(dy-0.08)*0.98;double ny=y+dy;if(ny<=70){send(p,x+=step,70,0,true);break;}send(p,x+=step,y=ny,0,false);}}
 public static void main(String[] a)throws Exception{
  // 1. Hover: a Flight cheat holds y in mid-air long past the jump apex.
  {
   var p=fresh();double y=70;int m=hist.size();
   send(p,0,y+=0.42,0,false); // jump off the ground
   for(int i=0;i<15;i++)send(p,0,y,0,false);
   ck(sig("fly")>0,"hover triggers fly signal: "+Verdict.signals);
   allNull(m,"hover LOG");
   ck(sig("nofall")==0&&sig("speed")==0&&sig("step")==0&&sig("microhop")==0,"hover flags only fly: "+Verdict.signals);
  }
  // 2. Ascend: climbing in mid-air with no jump left.
  {
   var p=fresh();double y=70;int m=hist.size();
   send(p,0,y+=0.42,0,false);
   for(int i=0;i<15;i++)send(p,0,y+=0.25,0,false);
   ck(sig("high")>0||sig("fly")>0,"mid-air ascend flagged: "+Verdict.signals);
   allNull(m,"ascend LOG");
  }
  // 3. Air jump: fresh upward kicks in mid-air.
  {
   var p=fresh();double y=70;int m=hist.size();
   send(p,0,y+=0.42,0,false);
   send(p,0,y,0,false);send(p,0,y,0,false);
   send(p,0,y+=0.5,0,false);
   send(p,0,y,0,false);
   send(p,0,y+=0.5,0,false);
   ck(sig("air")>0,"mid-air kicks flagged as air jump: "+Verdict.signals);
   allNull(m,"air jump LOG");
  }
  // 4. Honest vanilla jump arc: exact gravity, no signal, no correction.
  {
   var p=fresh();int m=hist.size();arc(p,70,0,0);
   ck(Verdict.signals.isEmpty(),"honest jump arc stays silent: "+Verdict.signals);
   allNull(m,"honest arc LOG");
  }
  // 5. Honest sprint-jump: gravity arc plus fast horizontal motion.
  {
   var p=fresh();int m=hist.size();arc(p,70,0,0.28);
   ck(Verdict.signals.isEmpty(),"honest sprint-jump stays silent: "+Verdict.signals);
   allNull(m,"honest sprint-jump LOG");
  }
  // 6. Enforcing mode: the same hover would actually set the player back.
  {
   var p=fresh();Verdict.next=Verdict.Step.HOLD;double y=70;
   send(p,0,y+=0.42,0,false);
   double[] r=null;for(int i=0;i<15&&r==null;i++)r=send(p,0,y,0,false);
   ck(sig("fly")>0,"enforcing hover still signals: "+Verdict.signals);
   ck(r!=null,"enforcing hover returns a setback");
   ck(r!=null&&r[0]==0&&r[2]==0&&r[1]>=70&&r[1]<=70.42,"setback returns to last good ground, got "+(r==null?"null":r[1]));
  }
  System.out.println("PASS fly-positive-control checks="+checks);
 }
}
