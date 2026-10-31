package dev.meshac;
/** Real hook, Inventory, Totem, Verdict and Ladder. Config/effects are controlled doubles. */
public class ActualMonitorPipelineTest extends dev.meshac.mixin.ServerGamePacketListenerImplMixin {
 public void teleport(double x,double y,double z,float yaw,float pitch){}
 static int checks;static java.lang.reflect.Method method;
 static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 org.spongepowered.asm.mixin.injection.callback.CallbackInfo click(int slot,long at)throws Exception{
  var p=new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(0,slot,0);TestClock.ms=at;player.world.server.main=false;
  var network=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("net",true);method.invoke(this,p,network);ck(!network.isCancelled(),"network phase records only");
  TestClock.ms=at+100;player.world.server.main=true;var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("main",true);method.invoke(this,p,ci);return ci;
 }
 public static void main(String[] args)throws Exception{
  method=dev.meshac.mixin.ServerGamePacketListenerImplMixin.class.getDeclaredMethod("meshac$click",net.minecraft.network.protocol.game.ServerboundContainerClickPacket.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);
  Meshac.skip=false;
  for(boolean monitor:new boolean[]{true,false})for(boolean egregious:new boolean[]{false,true}){
   Config.cur=new Config();Config.cur.monitor=monitor;Config.cur.egregious=egregious;Cases.adds=0;Cases.last=null;Meshac.LOG.signals=0;
   var h=new ActualMonitorPipelineTest();h.player=new net.minecraft.server.level.ServerPlayer();
   boolean enforce=!monitor||egregious;
   for(int i=0;i<8;i++){
    long at=100000+i*1000;var first=h.click(4,at);ck(!first.isCancelled(),"ordinary slot click permitted");
    var fast=h.click(45,at+5);ck(fast.isCancelled()==enforce,"actual monitor/egregious gate drives fast-fill cancellation");
    ck(h.player.menu.syncs==(enforce?i+1:0),"resync tracks actual refusal");
    ck(Meshac.LOG.signals>=i+1,"real Verdict logs each earned fast pair");
    if(!enforce)ck(Cases.adds==0&&h.player.connection.calls==0,"monitor-only never case/disconnects after repeated earned signals");
   }
   if(enforce)ck(Cases.adds>0&&h.player.connection.calls==Cases.adds,"real Ladder removal propagates to one disconnect per case");
   Inventory.forget(h.player.getUUID());Totem.forget(h.player.getUUID());Verdict.forget(h.player.getUUID());
  }
  System.out.println("PASS actual-monitor-pipeline checks="+checks);
 }
}
