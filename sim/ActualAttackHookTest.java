package dev.meshac;
public class ActualAttackHookTest extends dev.meshac.mixin.AttackMixin {
 static int checks;static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Class<?> packet=Class.forName("net.minecraft.network.protocol.game."+args[0]);
  var method=dev.meshac.mixin.AttackMixin.class.getDeclaredMethod("meshac$attack",packet,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);
  for(boolean skip:new boolean[]{false,true})for(boolean main:new boolean[]{false,true})for(boolean target:new boolean[]{false,true})for(String reason:new String[]{null,"scripted-reach"})for(Verdict.Step step:Verdict.Step.values()){
   Meshac.skip=skip;Combat.reason=reason;Combat.calls=0;Verdict.calls=0;Verdict.step=step;
   var hook=new ActualAttackHookTest();hook.player=new net.minecraft.server.level.ServerPlayer();hook.player.world.server.main=main;hook.player.world.target=target;
   var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("attack",true);method.invoke(hook,packet.getConstructor().newInstance(),ci);
   boolean reaches=!skip&&main&&target,signal=reaches&&reason!=null;
   ck(Combat.calls==(reaches?1:0),"one combat check only eligible attack");ck(Verdict.calls==(signal?1:0),"signal retained for detected attack");
   ck(ci.isCancelled()==(signal&&step!=Verdict.Step.LOG),"LOG permits attack/nonLOG denies");
  }
  if(args[0].equals("ServerboundInteractPacket")){
   Meshac.skip=false;Combat.calls=0;Verdict.calls=0;var hook=new ActualAttackHookTest();hook.player=new net.minecraft.server.level.ServerPlayer();
   Object p=packet.getConstructor().newInstance();packet.getField("attack").setBoolean(p,false);var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("interaction",true);method.invoke(hook,p,ci);
   ck(Combat.calls==0&&Verdict.calls==0&&!ci.isCancelled(),"legacy nonattack interaction untouched");
  }
  System.out.println("PASS actual-attack-hook "+args[0]+" checks="+checks);
 }
}
