package dev.meshac;
/** Execute actual initializer against recording event doubles, then its callback with real Combat. */
public class DisconnectRoutingTest {
 static int checks;static void ck(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  new Meshac().onInitialize();
  ck(net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.callbacks.size()==1,"one cleanup callback registered");
  var a=new net.minecraft.server.level.ServerPlayer();var b=new net.minecraft.server.level.ServerPlayer();
  TestClock.ms=100000;ck(Combat.check(a,new net.minecraft.world.entity.Entity(1))==null,"A first ordinary hit");TestClock.ms+=20;ck(Combat.check(a,new net.minecraft.world.entity.Entity(2))==null,"A earned first pair only");
  TestClock.ms=101000;ck(Combat.check(b,new net.minecraft.world.entity.Entity(1))==null,"B first ordinary hit");TestClock.ms+=20;ck(Combat.check(b,new net.minecraft.world.entity.Entity(2))==null,"B earned first pair only");
  var callback=net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.callbacks.get(0);
  callback.call(new net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.Handler(a),new net.minecraft.server.MinecraftServer());
  var rejoined=new net.minecraft.server.level.ServerPlayer(a.getUUID());TestClock.ms=102000;ck(Combat.check(rejoined,new net.minecraft.world.entity.Entity(1))==null,"reconnect starts fresh");TestClock.ms+=20;ck(Combat.check(rejoined,new net.minecraft.world.entity.Entity(2))==null,"registered callback clears previous multi strike");
  TestClock.ms=103000;ck(Combat.check(b,new net.minecraft.world.entity.Entity(1))==null,"B ordinary gap");TestClock.ms+=20;ck("two targets in one tick".equals(Combat.check(b,new net.minecraft.world.entity.Entity(2))),"registered callback preserves B earned strike");
  System.out.println("PASS registered-disconnect-routing checks="+checks);
 }
}
