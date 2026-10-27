package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.Level;
public class NoFallLogTest {
 static int checks;
 static void ck(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static void send(ServerPlayer p,double y,boolean g){TestClock.ms+=50;TestClock.ns+=50000000;Movement.check(p,ServerboundMovePlayerPacket.pos(0,y,0,g));p.setPos(0,y,0);}
 static ServerPlayer fresh(){Level.reset();Config.enforce=true;Verdict.next=Verdict.Step.LOG;Verdict.signals.clear();var p=new ServerPlayer("test");p.setPos(0,70,0);for(int i=0;i<25;i++)send(p,70,true);return p;}
 static Object state(ServerPlayer p)throws Exception{var f=Movement.class.getDeclaredField("STATE");f.setAccessible(true);return ((java.util.Map<?,?>)f.get(null)).get(p.getUUID());}
 static long armed(ServerPlayer p)throws Exception{var s=state(p);var f=s.getClass().getDeclaredField("nfAt");f.setAccessible(true);return f.getLong(s);}
 static void peak(ServerPlayer p)throws Exception{var s=state(p);var f=s.getClass().getDeclaredField("fallPeak");f.setAccessible(true);f.setDouble(s,80);}
 static void land(ServerPlayer p)throws Exception{p.setPos(0,60,0);Level.support=true;peak(p);Movement.tick(p);}
 public static void main(String[] a)throws Exception{
  for(boolean status:new boolean[]{false,true})for(boolean log:new boolean[]{true,false}){
   var p=fresh();Level.support=false;Verdict.next=log?Verdict.Step.LOG:Verdict.Step.HOLD;
   if(status)for(int i=0;i<3;i++)Movement.check(p,ServerboundMovePlayerPacket.status(true));else{send(p,68,true);send(p,66,true);send(p,64,true);}
   ck(Verdict.signals.stream().anyMatch(x->x.startsWith("nofall")),"real Movement nofall trigger status="+status);
   ck((armed(p)!=0)==!log,"LOG does not arm status="+status);
   land(p);ck((p.fallDamageCalls!=0)==!log,"LOG does not apply delayed damage status="+status);
  }
  var p=fresh();Level.support=false;Verdict.next=Verdict.Step.HOLD;send(p,68,true);send(p,66,true);send(p,64,true);ck(armed(p)!=0,"enforcing armed before policy change");Config.enforce=false;land(p);ck(armed(p)==0,"policy log-only clears pending");ck(p.fallDamageCalls==0,"policy log-only cancels delayed damage");
  System.out.println("PASS nofall-log-safety checks="+checks);
 }
}
