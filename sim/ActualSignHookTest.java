package dev.meshac;public class ActualSignHookTest extends dev.meshac.mixin.ServerGamePacketListenerImplMixin {
 public void teleport(double x,double y,double z,float yaw,float pitch){}
 public static void main(String[] args)throws Exception {
 var method=dev.meshac.mixin.ServerGamePacketListenerImplMixin.class.getDeclaredMethod("meshac$sign",net.minecraft.network.protocol.game.ServerboundSignUpdatePacket.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);int checks=0;
 for(boolean enforce:new boolean[]{false,true})for(long gap:new long[]{-1,0,250}){
 Config.enforce=enforce;Interact.gap=gap;Verdict.calls=0;var hook=new ActualSignHookTest();hook.player=new net.minecraft.server.level.ServerPlayer();
 var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("sign-test",true);method.invoke(hook,new net.minecraft.network.protocol.game.ServerboundSignUpdatePacket(),ci);
 if(ci.isCancelled()!=(enforce && gap>=0))throw new AssertionError("actual hook cancellation policy enforce="+enforce+" gap="+gap);checks++;
 if(Verdict.calls!=(gap>=0?1:0))throw new AssertionError("actual hook signal delivery must remain");checks++;
 }System.out.println("PASS actual-sign-hook checks="+checks);
 }}
