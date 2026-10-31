package dev.meshac;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
/** Earn multi-target strikes through real Combat.check, never inject private state. */
public class CombatForgetTest {
 static int checks;static void ck(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static String hit(ServerPlayer p,int id,long at){TestClock.ms=at;return Combat.check(p,new Entity(id));}
 static void strike(ServerPlayer p,long at){ck(hit(p,1,at)==null,"first ordinary aimed hit silent");ck(hit(p,2,at+20)==null,"first multi-target pair only strike");}
 static int size(String name)throws Exception{var f=Combat.class.getDeclaredField(name);f.setAccessible(true);return ((java.util.Map<?,?>)f.get(null)).size();}
 public static void main(String[] args)throws Exception{
  var a=new ServerPlayer();var b=new ServerPlayer();Combat.tick(a,1);Combat.tick(b,1);
  strike(a,100000);strike(b,101000);ck(size("MULTI")==2,"two UUIDs earned separate multi-target records");
  Combat.forget(a.getUUID());if(args.length==0)ck(size("MULTI")==1&&size("STATE")==1&&size("TRACK")==1,"forget removes own entries from all three maps");
  ck(hit(b,1,102000)==null,"other UUID normal gap silent");ck("two targets in one tick".equals(hit(b,2,102020)),"other UUID earned strike survives forget");
  // Same UUID/player object reconnects cold within10s; the third party's time does not clear its stale record.
  a=new ServerPlayer(a.getUUID());strike(a,103000);ck("two targets in one tick".equals(hit(a,1,103040)),"new identity must earn own second pair");
  Combat.forget(a.getUUID());Combat.forget(b.getUUID());ck(size("MULTI")==0&&size("STATE")==0&&size("TRACK")==0,"all cleanup maps empty after both disconnects");
  Combat.forget(a.getUUID());ck(size("MULTI")==0,"forget absent UUID idempotent");
  System.out.println("PASS combat-forget checks="+checks);
 }
}
