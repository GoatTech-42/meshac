package dev.meshac;
public class ActualCommandHookTest extends dev.meshac.mixin.ServerGamePacketListenerImplMixin {
 public void teleport(double x,double y,double z,float yaw,float pitch){}
 static int checks;static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception {
  for(boolean signed:new boolean[]{false,true})for(boolean skip:new boolean[]{false,true})for(String response:new String[]{null,"scripted-cadence"}){
   Meshac.skip=skip;Chatter.calls=0;Chatter.response=response;Verdict.calls=0;
   var hook=new ActualCommandHookTest();hook.player=new net.minecraft.server.level.ServerPlayer();
   var server=hook.player.level().getServer();
   Class<?> packet=Class.forName("net.minecraft.network.protocol.game.ServerboundChatCommand"+(signed?"Signed":"")+"Packet");
   Object p=packet.getConstructor(String.class).newInstance("tpa fixture");
   var method=dev.meshac.mixin.ServerGamePacketListenerImplMixin.class.getDeclaredMethod(signed?"meshac$signedChatCommand":"meshac$chatCommand",packet,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);
   var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("command-test",false);method.invoke(hook,p,ci);
   ck(!ci.isCancelled(),"command observer never cancels");ck(Chatter.calls==(skip?0:1),"one observation unless skipped");
   ck(skip||"tpa fixture".equals(Chatter.line),"exact packet command forwarded");
   ck(Verdict.calls==0,"signal not issued on observation thread");
   ck(server.tasks.size()==(!skip&&response!=null?1:0),"exactly one deferred signal when cadence exists");
   for(Runnable task:server.tasks)task.run();
   ck(Verdict.calls==(!skip&&response!=null?1:0),"queued task delivers one signal");
  }
  System.out.println("PASS actual-command-hooks checks="+checks);
 }
}
