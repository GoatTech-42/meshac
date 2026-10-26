package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.level.Level;
/** Scripted service-double regression of real Movement, not Minecraft physics or real Verdict. */
public class AirChainPortTest {
 static int checks;
 static void ck(boolean b,String m) { checks++; if(!b) throw new AssertionError(m); }
 static void adv(long ms) { TestClock.ms+=ms; TestClock.ns+=ms*1000000L; }
 static void send(ServerPlayer p,double y,boolean ground) { adv(50); Movement.check(p,ServerboundMovePlayerPacket.pos(0,y,0,ground));p.setPos(0,y,0); }
 static ServerPlayer fresh() { Level.reset(); Verdict.signals.clear();ServerPlayer p=new ServerPlayer("test");p.setPos(0,70,0);for(int i=0;i<25;i++)send(p,70,true);adv(4100);return p; }
 static boolean signal(String s) { return Verdict.signals.stream().anyMatch(x->x.contains(s)); }
 static double field(ServerPlayer p,String name) throws Exception { var f=Movement.class.getDeclaredField("STATE");f.setAccessible(true);Object st=((java.util.Map<?,?>)f.get(null)).get(p.getUUID());var v=st.getClass().getDeclaredField(name);v.setAccessible(true);return ((Number)v.get(st)).doubleValue(); }
 static void jump(ServerPlayer p) { send(p,70.42,false);send(p,70.7532,false);send(p,71.001336,false); }
 static void geometry(String label,double x,double z,net.minecraft.world.phys.AABB shape,boolean supported) throws Exception {
  ServerPlayer p=fresh(); Level.shapes=java.util.List.of(shape); Level.queries.clear();
  // A stationary claimed-ground packet gives one unambiguous test probe at the claimed box.
  // x/z translate the claimed position away from the player's accepted origin.
  adv(50); Movement.check(p,ServerboundMovePlayerPacket.pos(x,70,z,true));
  var strip=Level.queries.stream().filter(q -> Math.abs(q.box().minY-69.9)<1e-8 && Math.abs(q.box().maxY-70)<1e-8).toList();
  ck(!strip.isEmpty(),label+": support-strip query exists");
  for (var q:strip) {
   var b=q.box(); ck(q.entity()==p,label+": entity identity");
   ck(Math.abs(b.minX-(x-.3))<1e-8 && Math.abs(b.maxX-(x+.3))<1e-8 && Math.abs(b.minZ-(z-.3))<1e-8 && Math.abs(b.maxZ-(z+.3))<1e-8,label+": translated claimed footprint");
  }
  var sf=Movement.class.getDeclaredField("STATE");sf.setAccessible(true);Object st=((java.util.Map<?,?>)sf.get(null)).get(p.getUUID());var f=st.getClass().getDeclaredField("lastClaimAir");f.setAccessible(true);
  ck(f.getBoolean(st)==!supported,label+": geometry determines reset guard");
 }
 public static void main(String[] args) throws Exception {
  boolean fixed=Boolean.parseBoolean(args[0]);
  ServerPlayer p=fresh();Level.support=false;jump(p);double anchor=field(p,"airY0");
  send(p,71.001336,true);ck(!signal("nofall"),"one unsupported claim below existing consecutive threshold");
  ck((field(p,"airY0")==anchor)==fixed,"fake landing reset differential");
  send(p,71.421336,false);ck((field(p,"airY0")==anchor)==fixed,"packet after fake landing reset differential");
  send(p,71.754536,false);ck(signal("high jump")==fixed,"published fake-landing reproducer differential");
  p=fresh();jump(p);send(p,71.001336,true);send(p,71.421336,false);send(p,71.754536,false);
  ck(!signal("high jump"),"supported landing reanchors");ck(Verdict.signals.isEmpty(),"supported two-hop control silent");
  p=fresh();Level.support=false;jump(p);ck(Verdict.signals.isEmpty(),"ordinary jump segment silent");
  p=fresh();Level.support=false;send(p,68,true);send(p,66,true);ck(!signal("nofall"),"two claims below threshold");send(p,64,true);ck(signal("nofall"),"third consecutive claim retains original nofall detection");
  p=fresh();Level.support=false;send(p,70,true);p.water=true;send(p,70.2,false);ck(field(p,"airY0")==70.2,"water exemption still resets");
  p.water=false;send(p,70.3,false);ck(!signal("high jump"),"exempt exit no spurious high jump");
  p=fresh();Level.support=false;send(p,70,true);Movement.forget(p.getUUID());send(p,70,false);ck(field(p,"airY0")==0,"forget gets fresh unanchored state");
  if(fixed) {
   geometry("full block",.1,.1,new net.minecraft.world.phys.AABB(-1,69,-1,1,70,1),true);
   geometry("lower slab top",.1,.1,new net.minecraft.world.phys.AABB(-1,69.5,-1,1,70,1),true);
   geometry("partial ledge overlap",.1,.1,new net.minecraft.world.phys.AABB(.399,69.5,-1,1,70,1),true);
   geometry("ledge outside footprint",.1,.1,new net.minecraft.world.phys.AABB(.401,69.5,-1,1,70,1),false);
   geometry("touch-only edge",0,0,new net.minecraft.world.phys.AABB(.3,69.5,-1,1,70,1),false);
   geometry("within strip depth",.1,.1,new net.minecraft.world.phys.AABB(-1,69,-1,1,69.901,1),true);
   geometry("below strip depth",.1,.1,new net.minecraft.world.phys.AABB(-1,69,-1,1,69.899,1),false);
  }
  System.out.println("PASS fixed="+fixed+" checks="+checks);
 }
}
