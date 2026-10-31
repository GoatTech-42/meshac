#!/usr/bin/env python3
"""Real whole Combat with deterministic clock, focused geometry/world doubles.
No native physics, packet hook, actual Fabric disconnect callback or policy proof.
"""
import argparse,hashlib,resource,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--mutant',choices=['omit-combat','wrong-uuid']);args=p.parse_args()
source=root/'src/main/java/dev/meshac/Combat.java';text=source.read_text();actual=hashlib.sha256(source.read_bytes()).hexdigest();assert actual=='bbc626801736a1ce349f3ce1b6de8bb20a5b8ba73650d24ea2e49c92be1e84cd'
print('SOURCE',actual,'mutant',args.mutant,flush=True)
copy=text.replace('System.currentTimeMillis()','TestClock.ms');assert copy.replace('TestClock.ms','System.currentTimeMillis()')==text
sources={
'dev/meshac/Combat.java':copy,
'dev/meshac/DisconnectRoutingTest.java':(root/'sim/DisconnectRoutingTest.java').read_text(),
'dev/meshac/TestClock.java':'package dev.meshac;class TestClock {static long ms=100000;}',
'dev/meshac/Config.java':'package dev.meshac;class Config {static Config get(){return new Config();}boolean exempt(net.minecraft.server.level.ServerPlayer p){return false;}}',
'dev/meshac/Trace.java':'package dev.meshac;class Trace {static void t(Object p,String s){}}',
'dev/meshac/Verdict.java':'package dev.meshac;class Verdict {static void signal(Object p,String c,String d,int w){throw new AssertionError("unexpected auxiliary signal: "+d);}}',
'dev/meshac/Movement.java':'package dev.meshac;class Movement {static void maceSmash(Object p){throw new AssertionError("not mace");}}',
'dev/meshac/Meshac.java':'package dev.meshac;class Meshac {static final Log LOG=new Log();static class Log {void info(String s,Object... a){}}}',
'dev/meshac/veil/Veil.java':'package dev.meshac.veil;public class Veil {public static void attacked(Object p,Object e,int tick){}}',
'net/minecraft/util/Mth.java':'package net.minecraft.util;public class Mth {public static float wrapDegrees(float x){return x;}}',
'net/minecraft/world/phys/Vec3.java':'''package net.minecraft.world.phys;public class Vec3 {public final double x,y,z;public Vec3(double x,double y,double z){this.x=x;this.y=y;this.z=z;}public Vec3 add(Vec3 v){return new Vec3(x+v.x,y+v.y,z+v.z);}public Vec3 subtract(Vec3 v){return new Vec3(x-v.x,y-v.y,z-v.z);}public Vec3 scale(double n){return new Vec3(x*n,y*n,z*n);}public double dot(Vec3 v){return x*v.x+y*v.y+z*v.z;}public double length(){return Math.sqrt(dot(this));}}''',
'net/minecraft/world/phys/AABB.java':'''package net.minecraft.world.phys;public class AABB {public double minX,minY,minZ,maxX,maxY,maxZ;public AABB(double a,double b,double c,double d,double e,double f){minX=a;minY=b;minZ=c;maxX=d;maxY=e;maxZ=f;}public AABB inflate(double d){return this;}public Vec3 getCenter(){return new Vec3(0,0,2);}public double distanceToSqr(Vec3 v){return 1;}public java.util.Optional<Vec3> clip(Vec3 a,Vec3 b){return java.util.Optional.of(getCenter());}public boolean contains(Vec3 v){return false;}}''',
'net/minecraft/world/phys/HitResult.java':'package net.minecraft.world.phys;public class HitResult {public enum Type {MISS,BLOCK}public Type getType(){return Type.MISS;}}',
'net/minecraft/world/level/ClipContext.java':'package net.minecraft.world.level;public class ClipContext {public enum Block {COLLIDER}public enum Fluid {NONE}public ClipContext(Object a,Object b,Block c,Fluid d,Object e){}}',
'net/minecraft/world/entity/Entity.java':'''package net.minecraft.world.entity;public class Entity {final int id;public Entity(int id){this.id=id;}public int getId(){return id;}public net.minecraft.world.phys.AABB getBoundingBox(){return new net.minecraft.world.phys.AABB(-.2,-.2,1.8,.2,.2,2.2);}public net.minecraft.world.phys.Vec3 getEyePosition(){return new net.minecraft.world.phys.Vec3(0,0,0);}public net.minecraft.world.phys.Vec3 getDeltaMovement(){return new net.minecraft.world.phys.Vec3(0,0,0);}}''',
'net/minecraft/world/entity/LivingEntity.java':'package net.minecraft.world.entity;public class LivingEntity extends Entity {public LivingEntity(){super(1);}}',
'net/minecraft/server/level/ServerPlayer.java':'''package net.minecraft.server.level;public class ServerPlayer extends net.minecraft.world.entity.Entity {
public ServerPlayer(){this(java.util.UUID.randomUUID());}public ServerPlayer(java.util.UUID id){super(0);this.id=id;}final java.util.UUID id;public java.util.UUID getUUID(){return id;}public boolean isCreative(){return false;}public boolean isSpectator(){return false;}public boolean onGround(){return true;}public boolean onClimbable(){return false;}public boolean isInWater(){return false;}public boolean isPassenger(){return false;}public double fallDistance;public float getAttackStrengthScale(float x){return 1;}public float getYRot(){return 0;}
public net.minecraft.world.phys.Vec3 getLookAngle(){return new net.minecraft.world.phys.Vec3(0,0,1);}public Abilities getAbilities(){return new Abilities();}public static class Abilities {public boolean mayfly;}
public ItemStack getMainHandItem(){return new ItemStack();}public Inventory getInventory(){return new Inventory();}public static class ItemStack {public Object getItem(){return "sword";}}public static class Inventory {public ItemStack getItem(int s){return new ItemStack();}public int getSelectedSlot(){return 0;}}
public record Profile(String name){}public Profile getGameProfile(){return new Profile("fixture");}public World level(){return new World();}public static class World {public java.util.List<net.minecraft.world.entity.Entity> getEntities(Object p,net.minecraft.world.phys.AABB b,java.util.function.Predicate<net.minecraft.world.entity.Entity> test){return java.util.List.of(new net.minecraft.world.entity.LivingEntity());}public net.minecraft.world.phys.HitResult clip(net.minecraft.world.level.ClipContext c){return new net.minecraft.world.phys.HitResult();}public World getServer(){return this;}public int getTickCount(){return 1;}}
}''',
}
meshac=root/'src/main/java/dev/meshac/Meshac.java';body=meshac.read_text();assert hashlib.sha256(meshac.read_bytes()).hexdigest()=='dcdf075ea70f776b0a6cf097fd480c154096b25f068231378fab0550a47a66c3'
print('MESHAC',hashlib.sha256(meshac.read_bytes()).hexdigest(),'mutant',args.mutant,flush=True)
if args.mutant:
 old='Combat.forget(handler.player.getUUID());';assert body.count(old)==1
 body=body.replace(old,'' if args.mutant=='omit-combat' else 'Combat.forget(new java.util.UUID(0,0));')
sources['dev/meshac/Meshac.java']=body
sources['dev/meshac/DisconnectRoutingTest.java']=(root/'sim/DisconnectRoutingTest.java').read_text()
sources['net/fabricmc/api/ModInitializer.java']='package net.fabricmc.api;public interface ModInitializer {void onInitialize();}'
sources['org/slf4j/Logger.java']='package org.slf4j;public interface Logger {void info(String s,Object... a);}'
sources['org/slf4j/LoggerFactory.java']='package org.slf4j;public class LoggerFactory {public static Logger getLogger(String s){return (v,a)->{};}}'
sources['dev/meshac/Config.java']='package dev.meshac;class Config {static void load(){}static void applyVeil(){}static Config get(){return new Config();}boolean exempt(net.minecraft.server.level.ServerPlayer p){return false;}}'
sources['dev/meshac/Cases.java']='package dev.meshac;class Cases {static class Case {}static void load(){}static Case activeBan(java.util.UUID id){return null;}}'
sources['dev/meshac/Screens.java']='package dev.meshac;class Screens {static Object ban(Cases.Case c){return c;}}'
sources['dev/meshac/MeshCommands.java']='package dev.meshac;class MeshCommands {static void register(Object d){}}'
sources['dev/meshac/CombatLog.java']='package dev.meshac;class CombatLog {static void init(){}}'
sources['dev/meshac/Fingerprints.java']='package dev.meshac;class Fingerprints {static void check(Object p){}static void forget(java.util.UUID id){}}'
sources['dev/meshac/Movement.java']='package dev.meshac;class Movement {static void tick(Object p){}static void forget(java.util.UUID id){}static void maceSmash(Object p){throw new AssertionError("not mace");}}'
sources['dev/meshac/Verdict.java']='package dev.meshac;class Verdict {static void forget(java.util.UUID id){}static void signal(Object p,String c,String d,int w){throw new AssertionError("unexpected signal");}}'
for name in ['Vehicle','Mining','Interact','Inventory','Totem','Glide','Ticks','Chatter']:
 sources['dev/meshac/'+name+'.java']='package dev.meshac;class '+name+' {static void forget(java.util.UUID id){}}'
sources['dev/meshac/Fish.java']='package dev.meshac;class Fish {static void tick(net.minecraft.server.MinecraftServer s){}static void forget(java.util.UUID id){}}'
sources['dev/meshac/veil/Veil.java']='package dev.meshac.veil;public class Veil {public static void attacked(Object p,Object e,int t){}public static void grace(Object p,int t){}public static void tick(Object s){}public static void forgetPlayer(Object p){}}'
sources['net/minecraft/world/effect/MobEffects.java']='package net.minecraft.world.effect;public class MobEffects {public static final Object LEVITATION=new Object(),SLOW_FALLING=new Object();}'
sources['net/minecraft/server/MinecraftServer.java']='package net.minecraft.server;public class MinecraftServer {public int getTickCount(){return 1;}public MinecraftServer getPlayerList(){return this;}public java.util.List<net.minecraft.server.level.ServerPlayer> getPlayers(){return java.util.List.of();}}'
sources['net/fabricmc/fabric/api/command/v2/CommandRegistrationCallback.java']='package net.fabricmc.fabric.api.command.v2;public class CommandRegistrationCallback {public interface Callback {void call(Object d,Object r,Object e);}public static final Event EVENT=new Event();public static class Event {public void register(Callback c){}}}'
sources['net/fabricmc/fabric/api/event/lifecycle/v1/ServerTickEvents.java']='package net.fabricmc.fabric.api.event.lifecycle.v1;public class ServerTickEvents {public interface Callback {void call(net.minecraft.server.MinecraftServer s);}public static final Event END_SERVER_TICK=new Event();public static class Event {public void register(Callback c){}}}'
sources['net/fabricmc/fabric/api/networking/v1/ServerPlayConnectionEvents.java']='package net.fabricmc.fabric.api.networking.v1;public class ServerPlayConnectionEvents {public static class Handler {public final net.minecraft.server.level.ServerPlayer player;public Handler(net.minecraft.server.level.ServerPlayer p){player=p;}public void disconnect(Object o){}}public interface Join {void call(Handler h,Object sender,net.minecraft.server.MinecraftServer s);}public interface Disconnect {void call(Handler h,net.minecraft.server.MinecraftServer s);}public static final JoinEvent JOIN=new JoinEvent();public static final DisconnectEvent DISCONNECT=new DisconnectEvent();public static class JoinEvent {public void register(Join c){}}public static class DisconnectEvent {public final java.util.List<Disconnect> callbacks=new java.util.ArrayList<>();public void register(Disconnect c){callbacks.add(c);}}}'
player=sources['net/minecraft/server/level/ServerPlayer.java']
player=player.replace('public boolean isCreative()', 'public boolean isFallFlying(){return false;}public boolean isAutoSpinAttack(){return false;}public boolean hasEffect(Object e){return false;}public boolean isCreative()')
sources['net/minecraft/server/level/ServerPlayer.java']=player

with tempfile.TemporaryDirectory(prefix='disconnect-routing-') as tmp:
 d=Path(tmp);files=[]
 for rel,body in sources.items():
  f=d/rel;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(body);files.append(str(f))
 for cmd in [['javac','-J-Xmx256m','-d',str(d/'out')]+files,['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.DisconnectRoutingTest']]:
  with tempfile.TemporaryFile() as log:
   r=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=25,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='',flush=True);assert r.returncode==0,r.returncode
