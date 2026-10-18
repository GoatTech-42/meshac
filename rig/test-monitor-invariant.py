#!/usr/bin/env python3
"""Monitor safety: actual Cases/Verdict/Fingerprints/AttackMixins against instrumented service doubles.
No Minecraft runtime or native-play clearance. Exact Config methods and selected Interact/Glide/callback bodies are compiled with service doubles. Only wall-clock calls are rebound to a controlled test clock; policy bodies are unchanged.
"""
from pathlib import Path
import subprocess,tempfile,unittest,re
import os
R=Path(os.environ.get('MESHAC_TEST_TREE',Path(__file__).resolve().parents[1]))
S=R/'src/main/java/dev/meshac'
def method(text,needle):
 a=text.index(needle);b=text.index('{',a);depth=1;i=b+1
 while depth:
  depth+=(text[i]=='{')-(text[i]=='}');i+=1
 return text[a:i]
class MonitorInvariant(unittest.TestCase):
 def test_combatlog_disconnect(self):
  subprocess.run(["python3",str(R/"rig/test-combatlog-monitor.py")],check=True)
 def test_source_guards(self):
  c=(S/'Config.java').read_text();self.assertIn('return !monitor();',method(c,'public boolean enforce('))
  v=(S/'Verdict.java').read_text();self.assertIn('if (cfg.monitor())',v);self.assertNotIn('cfg.monitor() &&',v)
  rem=method(v,'private static void remove(');self.assertLess(rem.index('if (!Config.get().enforce('),rem.index('Cases.add('))
  f=(S/'Fingerprints.java').read_text();self.assertLess(f.index('if(cf.monitor())'),f.index('if(!REMOVED.add'))
  self.assertIn('LOG_AT.remove(id)',f)
  for src in ['attack1211','attack26']:
   a=(R/f'src/{src}/java/dev/meshac/mixin/AttackMixin.java').read_text()
   self.assertIn('Verdict.signalLogOnly(player, "combat", hit, 2);',a)
   attack=a.split('private void meshac$attack',1)[1].split('\n\t}',1)[0]
   self.assertNotIn('ci.cancel()',attack)
   self.assertNotIn('Verdict.signal(player, "combat"',attack)
 def test_movement_source_and_pure_pending_gates(self):
  if os.environ.get('MESHAC_TEST_BASELINE'):
   self.assertIn('monitorClear(s);',(S/'Movement.java').read_text())
   return
  with tempfile.TemporaryDirectory(prefix='monitor-source-') as d:
   subprocess.run(['javac','-J-Xmx128m','-d',d,str(S/'MonitorGate.java'),str(S/'SafeSwap.java'),str(R/'sim/MonitorGateTest.java'),str(R/'sim/SafeSwapTest.java'),str(R/'sim/MonitorSourceWitnessTest.java')],check=True)
   for name in ['MonitorGateTest','SafeSwapTest','MonitorSourceWitnessTest']:
    subprocess.run(['java','-Xmx128m','-cp',d,name],cwd=R,check=True)
 def test_actual_monitor_paths_and_positive_controls(self):
  with tempfile.TemporaryDirectory(prefix='monitor-invariant-') as d:
   root=Path(d)
   def put(name,text):
    p=root/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text.replace("System.currentTimeMillis()", "dev.meshac.TestClock.ms"))
   config=(S/'Config.java').read_text()
   put('dev/meshac/Config.java','''package dev.meshac; import java.util.*; import net.minecraft.server.level.ServerPlayer;
public class Config {
 static Config cur=new Config(); public static Config get(){return cur;}
 public Boolean monitorOnly; public boolean clientFingerprintEnabled=true;
 public List<String> egregiousChecks=new ArrayList<>(),clientFingerprintBrands=new ArrayList<>(),clientFingerprintChannels=new ArrayList<>();
 public boolean skips(ServerPlayer p,String c){return false;} public boolean disabled(String c){return false;}
 public boolean isEgregious(String c){return egregiousChecks!=null&&egregiousChecks.contains(c);}
 public int kickLimit=3;public boolean permanent=false;public int memoryDays(){return 30;} public int kicks(){return kickLimit;}
 public double offenceHalfLifeDays(){return 30;} public boolean explicitLadder(){return false;}
 public int[] tempMinutes(){return new int[]{5};} public double tempBase(){return 5;} public double tempGrowth(){return 6;} public double tempMax(){return 10080;}
 public int holdAtI(){return 1000;} public boolean perma(){return permanent;} public boolean permanentNeedsTierA(){return false;} public boolean skipRungOnTierA(){return false;}
 public Ladder.Params ladderParams(){return new Ladder.Params(8,1000,2000,3,2,3,150);}
'''+method(config,'public boolean monitor()')+'\n'+method(config,'public boolean enforce(')+'\n}')
   put('dev/meshac/Meshac.java','''package dev.meshac; import net.minecraft.server.level.ServerPlayer;
public class Meshac {public static boolean skip(ServerPlayer p){return false;} public static Logger LOG=new Logger();
public static class Logger {public int fpLogs, signals;public void info(String s,Object...o){if(s.contains("FINGERPRINT_LOG"))fpLogs++;}public void warn(String s,Object...o){if(s.contains("SIGNAL"))signals++;} }}''')
   put('dev/meshac/Discord.java','package dev.meshac; public class Discord { public static void hold(String a,String b,String c){} public static void post(Cases.Case c){} }')
   put('dev/meshac/Screens.java','package dev.meshac; public class Screens {public static String kick(String r,Cases.Case c){return r;}public static String ban(Cases.Case c){return "ban";} }')
   put('dev/meshac/Combat.java','package dev.meshac; import net.minecraft.server.level.ServerPlayer; import net.minecraft.world.entity.Entity;public class Combat {public static String check(ServerPlayer p,Entity e){return "reach 9";} }')
   put('dev/meshac/Interact.java','package dev.meshac; import net.minecraft.server.level.ServerPlayer;public class Interact {public static boolean entity(ServerPlayer p,int i){return false;} }')
   put('net/minecraft/world/entity/Entity.java','package net.minecraft.world.entity;public class Entity { public int getId(){return 7;} public double getY(){return 0;}public boolean onGround(){return false;}public boolean isInWater(){return false;} public net.minecraft.core.BlockPos blockPosition(){return new net.minecraft.core.BlockPos();} public net.minecraft.server.level.ServerPlayer.Level level(){return new net.minecraft.server.level.ServerPlayer.Level();}public Object getDeltaMovement(){return null;} }')
   put('net/minecraft/server/level/ServerPlayer.java','''package net.minecraft.server.level; import java.util.*; import net.minecraft.world.entity.Entity;
public class ServerPlayer { public UUID id=UUID.randomUUID(); public UUID getUUID(){return id;} public record Profile(String name){} public Profile getGameProfile(){return new Profile("test");}
public boolean isCreative(){return false;}public float getYRot(){return 0;}public float getXRot(){return 0;}public net.minecraft.world.item.ItemStack getUseItem(){return new net.minecraft.world.item.ItemStack();}public Menu containerMenu=new Menu();public static class Menu{public java.util.List<Slot> slots=java.util.List.of();public int resyncs;public void sendAllDataToRemote(){resyncs++;}}public static class Slot{public net.minecraft.world.item.ItemStack getItem(){return new net.minecraft.world.item.ItemStack();}}
public int tickCount;public net.minecraft.world.entity.Entity vehicle=new net.minecraft.world.entity.vehicle.boat.AbstractBoat();public net.minecraft.world.entity.Entity getRootVehicle(){return vehicle;} public boolean onGround(){return false;} public net.minecraft.world.item.ItemStack getMainHandItem(){return new net.minecraft.world.item.ItemStack();}
public Conn connection=new Conn();public static class Conn{public int disconnects,rollbacks;public void ackBlockChangesUpTo(int i){rollbacks++;}public void send(Object o){rollbacks++;}public int latency(){return 0;} public void disconnect(Object o){disconnects++;}}
public Level level(){return new Level();}public static class Level {public Block getBlockState(net.minecraft.core.BlockPos p){return new Block();}public Block getFluidState(net.minecraft.core.BlockPos p){return new Block();}public static class Block{public boolean is(Object o){return false;}}public Entity getEntity(int id){return new Entity();} public Server getServer(){return new Server();}}
public static class Server {public static java.util.List<Runnable> queue=new java.util.ArrayList<>();public void execute(Runnable r){queue.add(r);}public static int tick;public int getTickCount(){return tick;}public boolean isSameThread(){return true;}public Players getPlayerList(){return new Players();}} public static class Players{public ServerPlayer getPlayer(UUID u){return null;}}
public void sendSystemMessage(Object o){} }
''')
   put('net/minecraft/server/network/ServerGamePacketListenerImpl.java','package net.minecraft.server.network;public class ServerGamePacketListenerImpl {}')
   put('net/minecraft/network/chat/Component.java','package net.minecraft.network.chat;public class Component {public static Object literal(String s){return s;} }')
   put('net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking.java','package net.fabricmc.fabric.api.networking.v1; import java.util.*;import net.minecraft.server.level.ServerPlayer;public class ServerPlayNetworking {public static Set<String> getSendable(ServerPlayer p){return Set.of();} }')
   put('net/fabricmc/loader/api/FabricLoader.java','package net.fabricmc.loader.api;import java.nio.file.*;public class FabricLoader{public static FabricLoader getInstance(){return new FabricLoader();}public Path getConfigDir(){return Path.of(System.getProperty("java.io.tmpdir"));}}')
   put('com/google/gson/Gson.java','package com.google.gson;public class Gson{public <T>T fromJson(String s,java.lang.reflect.Type t){return null;}public String toJson(Object o){return "[]";}}')
   put('com/google/gson/GsonBuilder.java','package com.google.gson;public class GsonBuilder{public GsonBuilder setPrettyPrinting(){return this;}public Gson create(){return new Gson();}}')
   put('com/google/gson/reflect/TypeToken.java','package com.google.gson.reflect;public class TypeToken<T>{public java.lang.reflect.Type getType(){return Object.class;}}')
   for n in ['Mixin','Shadow']:
    put(f'org/spongepowered/asm/mixin/{n}.java',f'package org.spongepowered.asm.mixin;public @interface {n} {{Class<?>[] value() default {{}};}}')
   put('org/spongepowered/asm/mixin/injection/At.java','package org.spongepowered.asm.mixin.injection;public @interface At {String value();}')
   put('org/spongepowered/asm/mixin/injection/Inject.java','package org.spongepowered.asm.mixin.injection;public @interface Inject {String method();At at();boolean cancellable() default false;}')
   put('org/spongepowered/asm/mixin/injection/callback/CallbackInfo.java','package org.spongepowered.asm.mixin.injection.callback;public class CallbackInfo {public boolean canceled;public void cancel(){canceled=true;}}')
   put('net/minecraft/world/InteractionHand.java','package net.minecraft.world;public enum InteractionHand {MAIN_HAND,OFF_HAND}')
   put('net/minecraft/world/phys/Vec3.java','package net.minecraft.world.phys;public class Vec3 {}')
   put('net/minecraft/network/protocol/game/ServerboundAttackPacket.java','package net.minecraft.network.protocol.game;public class ServerboundAttackPacket {public int entityId(){return 1;}}')
   put('net/minecraft/network/protocol/game/ServerboundInteractPacket.java','''package net.minecraft.network.protocol.game;import net.minecraft.world.InteractionHand;import net.minecraft.world.phys.Vec3;import net.minecraft.world.entity.Entity;import net.minecraft.server.level.ServerPlayer;
public class ServerboundInteractPacket {public interface Handler {void onInteraction(InteractionHand h);void onInteraction(InteractionHand h,Vec3 v);void onAttack();}public void dispatch(Handler h){h.onAttack();}public int entityId(){return 1;}public Entity getTarget(ServerPlayer.Level l){return new Entity();}}''')
   put('net/minecraft/core/BlockPos.java','package net.minecraft.core;public class BlockPos{public BlockPos below(){return this;}}')
   put('net/minecraft/world/entity/vehicle/boat/AbstractBoat.java','package net.minecraft.world.entity.vehicle.boat;public class AbstractBoat extends net.minecraft.world.entity.Entity {}')
   put('net/minecraft/tags/FluidTags.java','package net.minecraft.tags;public class FluidTags {public static Object WATER=new Object();}')
   put('net/minecraft/world/level/block/Blocks.java','package net.minecraft.world.level.block;public class Blocks {public static Object BUBBLE_COLUMN=new Object();}')
   put('net/minecraft/network/protocol/game/ServerboundMoveVehiclePacket.java','package net.minecraft.network.protocol.game;public class ServerboundMoveVehiclePacket {}')
   put('net/minecraft/world/item/ItemStack.java','package net.minecraft.world.item;public class ItemStack {public boolean is(Object o){return false;}public boolean isEmpty(){return false;}public Object getItem(){return "test:potion";}}')
   put('dev/meshac/Packets.java','package dev.meshac;public class Packets {public static String[] lines(Object p){return new String[]{"typed"};}public static int seq(Object p){return 1;}public static class Pos{public double y=1;}public static Pos pos(Object p){return new Pos();}}')
   put('dev/meshac/ActionTrace.java','package dev.meshac;import java.util.function.Supplier;import net.minecraft.server.level.ServerPlayer;public class ActionTrace{public static String clickKind(Object p){return "PICKUP";}public static void event(ServerPlayer p,String s,Supplier<String>x){}}')
   put('dev/meshac/Trace.java','package dev.meshac;import net.minecraft.server.level.ServerPlayer;public class Trace{public static final boolean ON=false;public static void t(ServerPlayer p,String s){}}')
   put('dev/meshac/Ticks.java','package dev.meshac;import net.minecraft.server.level.ServerPlayer;public class Ticks{public static void inc(ServerPlayer p){}public static boolean bunched(ServerPlayer p){return false;}}')
   # Actual method bodies selected from Interact: no copied policy formulas. Geometry-heavy place is not covered by this fixture.
   interact=(S/'Interact.java').read_text()
   state=method(interact,'private static final class S')
   methods='\n'.join(method(interact,n) for n in ['public static void release(','public static void swap(','public static boolean use(','public static boolean entity(','public static boolean sign(','public static long signGap(','private static void rate(','public static void forget('])
   put('dev/meshac/Interact.java','package dev.meshac;import java.util.*;import java.util.concurrent.*;import net.minecraft.server.level.ServerPlayer;public class Interact {'+state+'private static void bowTrace(ServerPlayer p){}private static final Map<UUID,S> STATE=new ConcurrentHashMap<>();private static final int MAX_PER_SECOND=16;'+methods+'}')
   # Actual Glide start-spam decision body and real state, with rocket stub only for Interact.use integration.
   glide=(S/'Glide.java').read_text()
   put('dev/meshac/Glide.java','package dev.meshac;import java.util.*;import java.util.concurrent.*;import net.minecraft.server.level.ServerPlayer;public class Glide {'+method(glide,'private static final class S')+'private static final Map<UUID,S> STATE=new ConcurrentHashMap<>();public static void rocket(ServerPlayer p){}'+method(glide,'public static void startFly(')+'}')
   put('dev/meshac/TestClock.java','package dev.meshac;public class TestClock{public static long ms=1000000;}')
   put('dev/meshac/Fish.java','package dev.meshac;import net.minecraft.server.level.ServerPlayer;public class Fish{public static void use(ServerPlayer p){}}')
   put('dev/meshac/Mining.java','package dev.meshac;import net.minecraft.server.level.ServerPlayer;public class Mining{public static void digStart(ServerPlayer p,Object o){}public static void digStop(ServerPlayer p,Object o){}public static void digAbort(ServerPlayer p){}}')
   put('net/minecraft/tags/ItemTags.java','package net.minecraft.tags;public class ItemTags{public static Object BOW_ENCHANTABLE=new Object();}')
   put('net/minecraft/network/protocol/game/ServerboundSignUpdatePacket.java','package net.minecraft.network.protocol.game;public class ServerboundSignUpdatePacket{}')
   put('net/minecraft/network/protocol/game/ServerboundUseItemPacket.java','package net.minecraft.network.protocol.game;public class ServerboundUseItemPacket{}')
   put('net/minecraft/network/protocol/game/ServerboundContainerClickPacket.java','package net.minecraft.network.protocol.game;public class ServerboundContainerClickPacket{public int slotNum(){return 45;}public int buttonNum(){return 0;}public int containerId(){return 0;}}')
   put('net/minecraft/network/protocol/game/ServerboundPlayerActionPacket.java','package net.minecraft.network.protocol.game;public class ServerboundPlayerActionPacket{public enum Action{START_DESTROY_BLOCK,STOP_DESTROY_BLOCK,ABORT_DESTROY_BLOCK,SWAP_ITEM_WITH_OFFHAND,RELEASE_USE_ITEM}public Action getAction(){return Action.SWAP_ITEM_WITH_OFFHAND;}public Object getPos(){return null;}}')
   packet_source=(S/'mixin/ServerGamePacketListenerImplMixin.java').read_text()
   callbacks='\n'.join(method(packet_source,n) for n in ['private void meshac$vehicle(','private void meshac$sign(','private void meshac$use(','private void meshac$click(','private void meshac$release('])
   put('dev/meshac/PacketCallbacks.java','package dev.meshac;import net.minecraft.network.protocol.game.*;import net.minecraft.server.level.ServerPlayer;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;public class PacketCallbacks{public ServerPlayer player;'+callbacks+'}')
   put('dev/meshac/MonitorHarness.java','''package dev.meshac;
import java.util.*;import java.lang.reflect.*;import net.minecraft.server.level.ServerPlayer;import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
public class MonitorHarness {
 static void callback(String name,ServerPlayer p,Object packet,boolean monitor,boolean expectSignal)throws Exception {
  PacketCallbacks hook=new PacketCallbacks();hook.player=p;Method m=PacketCallbacks.class.getDeclaredMethod(name,packet.getClass(),CallbackInfo.class);m.setAccessible(true);CallbackInfo ci=new CallbackInfo();int before=Meshac.LOG.signals;
  m.invoke(hook,packet,ci);for(Runnable r:new ArrayList<>(ServerPlayer.Server.queue))r.run();ServerPlayer.Server.queue.clear();
  if(expectSignal)ck(Meshac.LOG.signals>before,name+" callback trigger missing");ck(ci.canceled!=monitor,name+" callback cancellation wrong");
  if(monitor)ck(p.connection.rollbacks==0&&p.containerMenu.resyncs==0,name+" punitive rollback in monitor");
 }
 static void clearVerdict()throws Exception {Field heat=Verdict.class.getDeclaredField("HEAT");heat.setAccessible(true);((Map<?,?>)heat.get(null)).clear();}
 static int witnessed;
 static void route(boolean monitor)throws Exception {
  Config cf=Config.get();cf.monitorOnly=monitor;ServerPlayer p=new ServerPlayer();
  for(String check:List.of("boatfly","inventory","interact","glide"))cf.egregiousChecks.add(check.toUpperCase());
  int before=Meshac.LOG.signals;boolean refused=false;
  for(int i=0;i<5;i++){ServerPlayer.Server.tick++;refused|=Vehicle.check(p,new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket());}
  ck(Meshac.LOG.signals>before,"VEHICLE no trigger witness");ck(refused!=monitor,"VEHICLE wrong action");witnessed++;
  for(int slot:new int[]{45,40}){
   p=new ServerPlayer();before=Meshac.LOG.signals;Totem.refuse(p,1000,4,false);refused=Totem.refuse(p,1005,slot,true);
   ck(Meshac.LOG.signals>before,"TOTEM no witness "+slot);ck(refused!=monitor,"TOTEM wrong action "+slot);witnessed++;
  }
  p=new ServerPlayer();before=Meshac.LOG.signals;for(int i=0;i<6;i++){Interact.swap(p);refused=Interact.use(p);ck(refused!=monitor,"USE action");}
  ck(Meshac.LOG.signals>before,"USE no witness");witnessed++;
  p=new ServerPlayer();before=Meshac.LOG.signals;Interact.entity(p,1);for(int i=2;i<5;i++){refused=Interact.entity(p,i);ck(refused!=monitor,"ENTITY action");}
  ck(Meshac.LOG.signals>before,"ENTITY no witness");witnessed++;
  p=new ServerPlayer();Field map=Interact.class.getDeclaredField("STATE");map.setAccessible(true);Interact.swap(p);Object state=((Map<?,?>)map.get(null)).get(p.id);Field click=state.getClass().getDeclaredField("clickAt");click.setAccessible(true);click.setLong(state,System.currentTimeMillis());
  before=Meshac.LOG.signals;refused=Interact.sign(p,new String[]{"typed"});ck(Meshac.LOG.signals>before,"SIGN no witness");ck(refused!=monitor,"SIGN action");witnessed++;
  p=new ServerPlayer();before=Meshac.LOG.signals;for(int i=0;i<40;i++)Inventory.hotbar(p);ck(Meshac.LOG.signals>before,"INVENTORY no witness");witnessed++;
  p=new ServerPlayer();before=Meshac.LOG.signals;for(int i=0;i<6;i++)Glide.startFly(p);ck(Meshac.LOG.signals>before,"GLIDE no witness");witnessed++;
  // Real callback seams: witness qualifying detector evidence and spy cancellation/rollback.
  p=new ServerPlayer();for(int i=0;i<4;i++){ServerPlayer.Server.tick++;Vehicle.check(p,new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket());}ServerPlayer.Server.tick++;
  callback("meshac$vehicle",p,new net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket(),monitor,true);
  p=new ServerPlayer();Interact.swap(p);for(int i=0;i<3;i++)Interact.use(p);callback("meshac$use",p,new net.minecraft.network.protocol.game.ServerboundUseItemPacket(),monitor,true);
  for(String name:List.of("meshac$click","meshac$release")){
   p=new ServerPlayer();Totem.refuse(p,TestClock.ms-5,4,false);
   callback(name,p,name.equals("meshac$click")?new net.minecraft.network.protocol.game.ServerboundContainerClickPacket():new net.minecraft.network.protocol.game.ServerboundPlayerActionPacket(),monitor,true);
  }
  p=new ServerPlayer();Interact.swap(p);state=((Map<?,?>)map.get(null)).get(p.id);click.setLong(state,TestClock.ms);
  callback("meshac$sign",p,new net.minecraft.network.protocol.game.ServerboundSignUpdatePacket(),monitor,true);
  // Queue through actual sign callback while enforcing, switch before executing its actual lambda.
  p=new ServerPlayer();Interact.swap(p);state=((Map<?,?>)map.get(null)).get(p.id);click.setLong(state,TestClock.ms);cf.monitorOnly=false;
  PacketCallbacks queueHook=new PacketCallbacks();queueHook.player=p;Method sign=PacketCallbacks.class.getDeclaredMethod("meshac$sign",net.minecraft.network.protocol.game.ServerboundSignUpdatePacket.class,CallbackInfo.class);sign.setAccessible(true);sign.invoke(queueHook,new net.minecraft.network.protocol.game.ServerboundSignUpdatePacket(),new CallbackInfo());ck(ServerPlayer.Server.queue.size()==1,"no async sign witness");cf.monitorOnly=true;before=Meshac.LOG.signals;ServerPlayer.Server.queue.remove(0).run();ck(Meshac.LOG.signals>before&&p.connection.disconnects==0,"actual queued sign recheck failed");
  cf.monitorOnly=false;ServerPlayer queued=p;Runnable task=()->ck(Verdict.signal(queued,"interact","queued sign",999)==Verdict.Step.LOG,"ASYNC recheck failed");cf.monitorOnly=true;before=Meshac.LOG.signals;task.run();ck(Meshac.LOG.signals>before,"ASYNC no signal witness");ck(p.connection.disconnects==0,"route monitor removed");witnessed++;
 }
 static void ck(boolean b,String m){if(!b)throw new AssertionError(m);}
 static void scenario(String row,String packetName)throws Exception {
  ServerPlayer p=new ServerPlayer();Config cf=Config.get();cf.monitorOnly=true;cf.egregiousChecks.add("combat");cf.egregiousChecks.add("fly");int before;
  switch(row){
   case "CONFIG_EGREGIOUS": for(String check:CheckNames.ALL)ck(!cf.enforce(check),"monitor enforces "+check);break;
   case "VERDICT_ORDINARY": cf.egregiousChecks.clear();for(int i=0;i<20;i++)ck(Verdict.signal(p,"combat","reach 9",999)==Verdict.Step.LOG,"ordinary not LOG");ck(p.connection.disconnects==0&&Cases.of("test").isEmpty(),"ordinary action");break;
   case "VERDICT_EGREGIOUS": for(int i=0;i<20;i++)ck(Verdict.signal(p,"combat","reach 9",999)==Verdict.Step.LOG,"egregious not LOG");ck(p.connection.disconnects==0&&Cases.of("test").isEmpty(),"egregious action");break;
   case "REMOVE_KICK": case "REMOVE_TEMPBAN": case "REMOVE_PERMANENT": cf.kickLimit=row.equals("REMOVE_KICK")?3:0;cf.permanent=row.equals("REMOVE_PERMANENT");Method remove=Verdict.class.getDeclaredMethod("remove",ServerPlayer.class,String.class,List.class);remove.setAccessible(true);remove.invoke(null,p,"combat reach 9",List.of("qualified"));ck(p.connection.disconnects==0&&Cases.of("test").isEmpty(),"remove action");if(row.equals("REMOVE_PERMANENT"))for(int i=0;i<4;i++)Cases.add("test",p.id,"kick","prior","meshac",0,List.of());cf.monitorOnly=false;remove.invoke(null,p,"combat reach 9",List.of("qualified"));ck(p.connection.disconnects==1&&!Cases.of("test").isEmpty(),"remove positive missing");String want=row.equals("REMOVE_KICK")?"kick":row.equals("REMOVE_TEMPBAN")?"tempban":"ban";ck(Cases.of("test").stream().anyMatch(c->c.action.equals(want)),"wrong removal positive rung");break;
   case "CASES_AUTO_BAN": Cases.add("test",p.id,"ban","auto","meshac",0,List.of());ck(Cases.activeBan(p.id)==null,"auto ban applies");cf.monitorOnly=false;ck(Cases.activeBan(p.id)!=null,"auto positive missing");break;
   case "CASES_STAFF_SCAN": Cases.Case staff=Cases.add("test",p.id,"ban","staff","operator",0,List.of());Cases.add("test",p.id,"ban","auto","meshac-fingerprint",0,List.of());ck(Cases.activeBan(p.id)==staff,"staff scan hidden by auto");break;
   case "FINGERPRINT_BRAND": case "FINGERPRINT_CHANNEL": cf.clientFingerprintBrands.add("test:bad");cf.clientFingerprintChannels.add("test:bad");if(row.endsWith("BRAND"))Fingerprints.brand(p.id,"test:bad");else Fingerprints.channels(p.id,Set.of("test:bad"));for(int i=0;i<20;i++)Fingerprints.check(p);ck(Meshac.LOG.fpLogs==1&&p.connection.disconnects==0&&Cases.of("test").isEmpty(),"fingerprint action/log");cf.monitorOnly=false;Fingerprints.check(p);ck(p.connection.disconnects==1,"fingerprint positive missing");break;
   case "FINGERPRINT_FORGET": cf.clientFingerprintBrands.add("test:bad");Fingerprints.brand(p.id,"test:bad");Fingerprints.check(p);Fingerprints.forget(p.id);Fingerprints.brand(p.id,"test:bad");Fingerprints.check(p);ck(Meshac.LOG.fpLogs==2&&p.connection.disconnects==0,"forget throttle/removal");break;
   case "ATTACK": Class<?> mix=Class.forName("dev.meshac.mixin.AttackMixin");Object hook=Class.forName("dev.meshac.MonitorHarness$Attack").getConstructor().newInstance();mix.getField("player").set(hook,p);Class<?> packet=Class.forName(packetName);Method attack=mix.getDeclaredMethod("meshac$attack",packet,CallbackInfo.class);attack.setAccessible(true);CallbackInfo ci=new CallbackInfo();attack.invoke(hook,packet.getConstructor().newInstance(),ci);ck(!ci.canceled&&Meshac.LOG.signals>0,"attack cancel/no signal");int cb=Cases.of("test").size();cf.monitorOnly=false;ci=new CallbackInfo();attack.invoke(hook,packet.getConstructor().newInstance(),ci);ck(!ci.canceled,"combat denied with monitor off (0055 by-construction)");ck(p.connection.disconnects==0&&Cases.of("test").size()==cb,"by-construction removal");break;
   default:throw new AssertionError("missing row adapter "+row);
  }
  System.out.println("ROW "+row+" PASS");
 }
 public static void main(String[] args)throws Exception {
  if(args.length>1){scenario(args[1],args[0]);return;}
  ServerPlayer p=new ServerPlayer();Config cf=Config.get();cf.monitorOnly=true;cf.egregiousChecks.add("combat");cf.egregiousChecks.add("fly");
  for(List<String> names:Arrays.asList(null,List.<String>of(),List.of("combat","COMBAT","fly","FLY"))){cf.egregiousChecks=names;for(String c:CheckNames.ALL)ck(!cf.enforce(c),"monitor enforces "+c);ck(!cf.enforce("unknown-sentinel"),"monitor enforces unknown");}cf.egregiousChecks=new ArrayList<>(List.of("combat","fly"));
  for(int i=0;i<20;i++)ck(Verdict.signal(p,"combat","reach 9",999)==Verdict.Step.LOG,"monitor verdict not LOG");
  ck(p.connection.disconnects==0 && Cases.of("test").isEmpty(),"monitor signal caused removal/case");
  Method remove=Verdict.class.getDeclaredMethod("remove",ServerPlayer.class,String.class,List.class);remove.setAccessible(true);remove.invoke(null,p,"combat reach 9",List.of());
  ck(p.connection.disconnects==0 && Cases.of("test").isEmpty(),"final remove guard failed");
  cf.clientFingerprintBrands.add("test:bad");Fingerprints.brand(p.id,"test:bad");for(int i=0;i<100;i++)Fingerprints.check(p);
  ck(p.connection.disconnects==0 && Cases.of("test").isEmpty(),"fingerprint monitor took action");ck(Meshac.LOG.fpLogs==1,"fingerprint log unbounded");TestClock.ms+=4999;Fingerprints.check(p);ck(Meshac.LOG.fpLogs==1,"fingerprint logged before5s");TestClock.ms++;Fingerprints.check(p);ck(Meshac.LOG.fpLogs==2,"fingerprint did not log at5s");
  Field removed=Fingerprints.class.getDeclaredField("REMOVED");removed.setAccessible(true);ck(((Set<?>)removed.get(null)).isEmpty(),"monitor marks removed");
  ServerPlayer channel=new ServerPlayer();cf.clientFingerprintChannels.add("test:channel");Fingerprints.channels(channel.id,Set.of("test:channel"));int channelLog=Meshac.LOG.fpLogs;Fingerprints.check(channel);ck(Meshac.LOG.fpLogs==channelLog+1&&channel.connection.disconnects==0,"channel monitor took action");
  Fingerprints.forget(channel.id);
  Fingerprints.forget(p.id);Field logs=Fingerprints.class.getDeclaredField("LOG_AT");logs.setAccessible(true);ck(((Map<?,?>)logs.get(null)).isEmpty(),"log cleanup failed");
  Cases.Case staff=Cases.add("test",p.id,"ban","staff","operator",0,List.of());Cases.add("test",p.id,"ban","auto","meshac",0,List.of());Cases.add("test",p.id,"ban","fp","meshac-fingerprint",0,List.of());
  ck(Cases.activeBan(p.id)==staff,"auto suppression hid older staff ban");
  UUID auto=UUID.randomUUID();Cases.add("auto",auto,"ban","auto","meshac",0,List.of());ck(Cases.activeBan(auto)==null,"auto ban active in monitor");
  Class<?> mix=Class.forName("dev.meshac.mixin.AttackMixin");Object hook=Class.forName("dev.meshac.MonitorHarness$Attack").getConstructor().newInstance();mix.getField("player").set(hook,p);
  Class<?> packet=Class.forName(args[0]);Method attack=mix.getDeclaredMethod("meshac$attack",packet,CallbackInfo.class);attack.setAccessible(true);CallbackInfo ci=new CallbackInfo();attack.invoke(hook,packet.getConstructor().newInstance(),ci);ck(!ci.canceled,"monitor cancels attack");
  cf.monitorOnly=false;ck(cf.enforce("combat"),"enforced mode disabled");int casesBefore=Cases.of("test").size();ci=new CallbackInfo();attack.invoke(hook,packet.getConstructor().newInstance(),ci);ck(!ci.canceled,"by-construction combat denied the hit with monitor off (0055)");ck(p.connection.disconnects==0&&Cases.of("test").size()==casesBefore,"by-construction removal in enforced config");
  Field heatMap=Verdict.class.getDeclaredField("HEAT");heatMap.setAccessible(true);Object hh=((Map<?,?>)heatMap.get(null)).get(p.id);Field monF=hh.getClass().getDeclaredField("mon");monF.setAccessible(true);Field lhF=hh.getClass().getDeclaredField("lh");lhF.setAccessible(true);Field heatVal=Ladder.Heat.class.getDeclaredField("heat");heatVal.setAccessible(true);ck(heatVal.getDouble(monF.get(hh))>0,"log-only mon heat missing");ck(heatVal.getDouble(lhF.get(hh))==0.0,"combat leaked into the removal ladder (0055)");
  int b4=Meshac.LOG.signals;for(int i=0;i<40;i++)Verdict.signalLogOnly(p,"combat","budget witness",1);ck(Meshac.LOG.signals-b4==8,"log-only budget cap wrong (30 per 60s, proposal value)");TestClock.ms+=60001;b4=Meshac.LOG.signals;Verdict.signalLogOnly(p,"combat","budget witness",1);ck(Meshac.LOG.signals-b4==1,"log-only budget window did not roll");
  ck(Cases.activeBan(auto)!=null,"enforced auto ban lost");
  Fingerprints.brand(p.id,"test:bad");Fingerprints.check(p);ck(p.connection.disconnects==1,"enforced fingerprint positive lost");
  route(true);clearVerdict();route(false);clearVerdict();ck(witnessed==18,"missing route witnesses");System.out.println("monitor invariant runtime doubles PASS "+args[0]);
 }
 public static class Attack extends dev.meshac.mixin.AttackMixin {}
}''')
   production=[S/f'{n}.java' for n in ['Cases','Verdict','Fingerprints','Ladder','Vehicle','Totem','Inventory','ActionEvidence','InventoryBurst','InventoryBurstEvidence','BeatCounter','InteractionPoseRing','InteractionPose','CombatEvidence','CheckNames']]
   for source in production: put('production/'+source.name,source.read_text())
   production=[]
   stub=list(root.rglob('*.java'))
   for version,packet in [('attack1211','ServerboundInteractPacket'),('attack26','ServerboundAttackPacket')]:
    out=root/version;out.mkdir()
    subprocess.run(['javac','-J-Xmx128m','-proc:none','-d',str(out),*[str(p) for p in stub+production],str(R/f'src/{version}/java/dev/meshac/mixin/AttackMixin.java')],check=True)
    rows=['CONFIG_EGREGIOUS','VERDICT_ORDINARY','VERDICT_EGREGIOUS','REMOVE_KICK','REMOVE_TEMPBAN','REMOVE_PERMANENT','CASES_AUTO_BAN','CASES_STAFF_SCAN','FINGERPRINT_BRAND','FINGERPRINT_CHANNEL','FINGERPRINT_FORGET','ATTACK']
    failures=[]
    for row in rows:
     result=subprocess.run(['java','-Xmx128m',f'-Djava.io.tmpdir={root}','-cp',str(out),'dev.meshac.MonitorHarness',f'net.minecraft.network.protocol.game.{packet}',row],text=True,capture_output=True)
     print(f'ROW_RESULT {version} {row} '+('PASS' if result.returncode==0 else 'FAIL'),flush=True)
     if result.returncode: failures.append((row,result.stderr.splitlines()[0] if result.stderr else 'no output'))
    if not os.environ.get('MESHAC_TEST_BASELINE'):
     self.assertFalse(failures,failures)
     subprocess.run(['java','-Xmx128m',f'-Djava.io.tmpdir={root}','-cp',str(out),'dev.meshac.MonitorHarness',f'net.minecraft.network.protocol.game.{packet}'],check=True)
if __name__=='__main__':unittest.main(verbosity=2)
