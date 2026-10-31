package dev.meshac;
/** Actual container hook -> real Inventory -> real Totem, focused world/policy doubles. */
public class ActualTotemPipelineTest extends dev.meshac.mixin.ServerGamePacketListenerImplMixin {
 public void teleport(double x,double y,double z,float yaw,float pitch){}
 static int checks;static java.lang.reflect.Method method;
 static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 org.spongepowered.asm.mixin.injection.callback.CallbackInfo click(int slot,int button,long at)throws Exception{
  var p=new net.minecraft.network.protocol.game.ServerboundContainerClickPacket(0,slot,button);
  TestClock.ms=at;player.world.server.main=false;var n=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("network",true);method.invoke(this,p,n);ck(!n.isCancelled(),"network phase never cancels");
  TestClock.ms=at+100;player.world.server.main=true;var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("main",true);method.invoke(this,p,ci);return ci;
 }
 public static void main(String[] args)throws Exception{
  method=dev.meshac.mixin.ServerGamePacketListenerImplMixin.class.getDeclaredMethod("meshac$click",net.minecraft.network.protocol.game.ServerboundContainerClickPacket.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);
  Meshac.skip=false;Config.exempt=false;
  for(Verdict.Step step:Verdict.Step.values()){
   var h=new ActualTotemPipelineTest();h.player=new net.minecraft.server.level.ServerPlayer();Verdict.step=step;Verdict.calls=0;
   long base=100000;var first=h.click(4,0,base);ck(!first.isCancelled()&&Verdict.calls==0,"ordinary initial slot silent");
   var fast=h.click(45,0,base+5);
   ck(Verdict.calls==1&&Verdict.detail.contains("5 ms after another slot"),"real queued5ms pair retained despite100ms main delay");
   ck(Verdict.weight==2,"first impossible pair weight2");
   ck(fast.isCancelled()==(step!=Verdict.Step.LOG),"LOG permits real fast pair/nonLOG denies");
   ck(h.player.menu.syncs==(step==Verdict.Step.LOG?0:1),"resync only for refusal");
   var normal=h.click(45,0,base+1000);ck(!normal.isCancelled()&&Verdict.calls==1,"normal-speed fill resets without signal");
   TestClock.ms=base+2000;Totem.popped(h.player);var reaction=h.click(7,40,base+2005);
   ck(Verdict.calls==2&&Verdict.detail.contains("5 ms after a totem popped"),"real pop path through button40 signals");
   ck(reaction.isCancelled()==(step!=Verdict.Step.LOG),"LOG pop signal no cancellation");
   Inventory.forget(h.player.getUUID());Totem.forget(h.player.getUUID());
  }
  // A25ms boundary is normal; two20ms fills need their own earned streak.
  var h=new ActualTotemPipelineTest();h.player=new net.minecraft.server.level.ServerPlayer();Verdict.calls=0;Verdict.step=Verdict.Step.LOG;
  h.click(4,0,400000);var edge=h.click(45,0,400025);ck(Verdict.calls==0&&!edge.isCancelled(),"25ms pair not fast");
  h.click(4,0,401000);h.click(45,0,401020);ck(Verdict.calls==0,"first20ms pair only strike");
  h.click(4,0,402000);var repeated=h.click(45,0,402020);ck(Verdict.calls==1&&Verdict.detail.contains("2 in a row")&&!repeated.isCancelled(),"second20ms pair signals LOG without refusal");
  ck(h.player.menu.syncs==0,"all LOG sequences leave resync off");
  System.out.println("PASS actual-totem-pipeline checks="+checks);
 }
}
