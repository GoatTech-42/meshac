package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.Level;
/** Real Movement, scripted clocks/policy/world. No live packet or thread proof. */
public class MovementArrivalTest {
 static int checks;
 static void ck(boolean value,String why){checks++;if(!value)throw new AssertionError(why);}
 static void advance(long ms){TestClock.ms+=ms;TestClock.ns+=ms*1000000;}
 static ServerboundMovePlayerPacket pos(){return ServerboundMovePlayerPacket.pos(0,70,0,true);}
 static void check(ServerPlayer p,ServerboundMovePlayerPacket packet){ck(Movement.check(p,packet)==null,"every packet must remain LOG-only");}
 public static void main(String[] args){
  Level.reset();Verdict.next=Verdict.Step.LOG;Config.exempt=false;
  var p=new ServerPlayer("arrival");p.setPos(0,70,0);
  for(int i=0;i<120;i++){advance(50);Movement.arrive(p,pos());check(p,pos());}
  Config.exempt=true;
  for(int i=0;i<100;i++){advance(50);Movement.arrive(p,pos());check(p,pos());}
  ck(Verdict.signals.isEmpty(),"exempt movement must not signal");
  Config.exempt=false;
  for(int i=0;i<50;i++){advance(1);Movement.arrive(p,pos());check(p,pos());}
  ck(Verdict.signals.stream().anyMatch(s->s.startsWith("timer ")),"current fast arrivals must signal after exemption ends, not replay stale exempt arrivals");
  Movement.forget(p.getUUID());
  Verdict.signals.clear();p=new ServerPlayer("interleaved");p.setPos(0,70,0);
  for(int i=0;i<120;i++){advance(50);Movement.arrive(p,pos());check(p,pos());}
  for(int i=0;i<50;i++){advance(1);Movement.arrive(p,pos());}
  for(int i=0;i<50;i++){advance(50);check(p,ServerboundMovePlayerPacket.status(true));check(p,pos());}
  ck(Verdict.signals.stream().anyMatch(s->s.startsWith("timer ")),"status-only packets must not consume queued position timestamps");
  Movement.forget(p.getUUID());
  System.out.println("PASS movement-arrival checks="+checks);
 }
}
