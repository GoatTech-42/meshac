package dev.meshac;
public class ActualAttackHookTest extends dev.meshac.mixin.AttackMixin {
 static int checks;static void ck(boolean v,String m){checks++;if(!v)throw new AssertionError(m);}
 public static void main(String[] args)throws Exception{
  Class<?> packet=Class.forName("net.minecraft.network.protocol.game."+args[0]);
  var method=dev.meshac.mixin.AttackMixin.class.getDeclaredMethod("meshac$attack",packet,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);method.setAccessible(true);
  for(boolean skip:new boolean[]{false,true})for(boolean main:new boolean[]{false,true})for(boolean target:new boolean[]{false,true})for(String reason:new String[]{null,"scripted-reach"})for(Verdict.Step step:Verdict.Step.values()){
   Meshac.skip=skip;Combat.reason=reason;Combat.calls=0;Verdict.calls=0;Interact.calls=0;Interact.lastId=-1;Verdict.step=step;
   var hook=new ActualAttackHookTest();hook.player=new net.minecraft.server.level.ServerPlayer();hook.player.world.server.main=main;hook.player.world.target=target;
   var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("attack",true);method.invoke(hook,packet.getConstructor().newInstance(),ci);
   boolean reaches=!skip&&main&&target,signal=reaches&&reason!=null;
   ck(Combat.calls==(reaches?1:0),"one combat check only eligible attack");ck(Verdict.calls==(signal?1:0),"signal retained for detected attack");
   ck(ci.isCancelled()==(signal&&step!=Verdict.Step.LOG),"LOG permits attack/nonLOG denies");
   ck(Interact.calls==0,"attack path never routes entity interaction");
  }
  if(args[0].equals("ServerboundInteractPacket")){
   // 1.21.x: plain entity interactions arrive in the same packet as attacks and route to the entity-aura check
   for(boolean skip:new boolean[]{false,true})for(boolean main:new boolean[]{false,true})for(boolean target:new boolean[]{false,true})for(boolean refuse:new boolean[]{false,true}){
    Meshac.skip=skip;Combat.calls=0;Verdict.calls=0;Interact.calls=0;Interact.lastId=-1;Interact.refuse=refuse;
    var hook=new ActualAttackHookTest();hook.player=new net.minecraft.server.level.ServerPlayer();hook.player.world.server.main=main;hook.player.world.target=target;
    Object p=packet.getConstructor().newInstance();packet.getField("attack").setBoolean(p,false);var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("interaction",true);method.invoke(hook,p,ci);
    boolean routes=!skip&&main&&target;
    ck(Interact.calls==(routes?1:0),"nonattack interaction routes to entity check exactly once when eligible");
    ck(!routes||Interact.lastId==42,"entity check receives the resolved target id");
    ck(Combat.calls==0&&Verdict.calls==0,"nonattack interaction never touches combat path");
    ck(ci.isCancelled()==(routes&&refuse),"entity refusal cancels, acceptance passes");
   }
  }
  else {
   // 26.x: the separate interact hook routes every entity interaction (reference routing the 1.21.x fix mirrors)
   var interact=dev.meshac.mixin.AttackMixin.class.getDeclaredMethod("meshac$interact",net.minecraft.network.protocol.game.ServerboundInteractPacket.class,org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class);interact.setAccessible(true);
   for(boolean skip:new boolean[]{false,true})for(boolean main:new boolean[]{false,true})for(boolean refuse:new boolean[]{false,true}){
    Meshac.skip=skip;Combat.calls=0;Verdict.calls=0;Interact.calls=0;Interact.lastId=-1;Interact.refuse=refuse;
    var hook=new ActualAttackHookTest();hook.player=new net.minecraft.server.level.ServerPlayer();hook.player.world.server.main=main;
    var ci=new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("interaction",true);interact.invoke(hook,new net.minecraft.network.protocol.game.ServerboundInteractPacket(),ci);
    boolean routes=!skip&&main;
    ck(Interact.calls==(routes?1:0),"26 interact hook routes exactly once when eligible");
    ck(!routes||Interact.lastId==42,"26 interact hook passes the packet entity id");
    ck(Combat.calls==0&&Verdict.calls==0,"26 interact hook never touches combat path");
    ck(ci.isCancelled()==(routes&&refuse),"26 entity refusal cancels, acceptance passes");
   }
  }
  System.out.println("PASS actual-attack-hook "+args[0]+" checks="+checks);
 }
}
