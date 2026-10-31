package dev.meshac;
public class ActualInventoryHookTest extends dev.meshac.mixin.ServerGamePacketListenerImplMixin {
 public void teleport(double x,double y,double z,float yaw,float pitch){}
 static int checks;static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 static java.lang.reflect.Method method;
 org.spongepowered.asm.mixin.injection.callback.CallbackInfo invoke(net.minecraft.network.protocol.game.ServerboundContainerClickPacket p,boolean main)throws Exception{
  player.world.server.main=main;var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("click",true);method.invoke(this,p,ci);return ci;
 }
 public static void main(String[] args)throws Exception{
  method=dev.meshac.mixin.ServerGamePacketListenerImplMixin.class.getDeclaredMethod("meshac$click",net.minecraft.network.protocol.game.ServerboundContainerClickPacket.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);
  var h=new ActualInventoryHookTest();h.player=new net.minecraft.server.level.ServerPlayer();var p=new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(0,45,0);
  Meshac.skip=false;Config.exempt=true;Totem.refuse=false;
  for(int i=0;i<10;i++){
   TestClock.ms=100000+i*1000;Totem.calls=0;var n=h.invoke(p,false);ck(!n.isCancelled()&&Totem.calls==0,"network phase records only");
   TestClock.ms+=50;var m=h.invoke(p,true);ck(!m.isCancelled()&&Totem.calls==1&&Totem.at==100000+i*1000,"exempt main replay forwards own network timestamp");
  }
  Config.exempt=false;TestClock.ms=200000;h.invoke(p,false);TestClock.ms+=500;h.invoke(p,true);ck(Totem.at==200000,"after exemption no stale arrivals replayed");
  TestClock.ms=201000;h.invoke(p,true);ck(Totem.at==201000,"unpaired main click uses clock fallback");
  ck(Verdict.calls==0,"exempt and sparse legal stream has no inventory signals");
  // Packet flags are kept separate from Inventory timing and passed to Totem.
  for(int[] fields:new int[][]{{0,45,0},{0,4,40},{1,45,0},{0,4,0}}){
   Totem.refuse=true;int before=h.player.menu.syncs;TestClock.ms+=1000;
   var ci=h.invoke(new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(fields[0],fields[1],fields[2]),true);
   ck(Totem.slot==fields[1]&&Totem.offhand==(fields[0]==0&&(fields[1]==45||fields[2]==40)),"exact slot/offhand forwarded");
   ck(ci.isCancelled()&&h.player.menu.syncs==before+1,"refusal cancels and resyncs once");
   Totem.refuse=false;before=h.player.menu.syncs;TestClock.ms+=1000;ci=h.invoke(p,true);ck(!ci.isCancelled()&&h.player.menu.syncs==before,"nonrefusal neither cancels nor resyncs");
  }
  Totem.calls=0;h.player.creative=true;TestClock.ms=300000;h.invoke(p,false);TestClock.ms+=9;var ci=h.invoke(p,true);ck(Totem.calls==0&&!ci.isCancelled(),"creative main click not sent to Totem");
  h.player.creative=false;TestClock.ms=301000;h.invoke(p,false);TestClock.ms+=9;h.invoke(p,true);ck(Totem.at==301000,"creative arrival was consumed rather than replayed");
  Meshac.skip=true;Totem.calls=0;TestClock.ms=400000;h.invoke(p,false);TestClock.ms+=50;ci=h.invoke(p,true);ck(Totem.calls==0&&!ci.isCancelled(),"skip gate bypasses both phases");
  Meshac.skip=false;TestClock.ms=401000;h.invoke(p,true);ck(Totem.at==401000,"skip did not leave stale arrival");
  Inventory.forget(h.player.getUUID());System.out.println("PASS actual-inventory-hook checks="+checks);
 }
}
