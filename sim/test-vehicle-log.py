#!/usr/bin/env python3
"""Real Vehicle policy with scripted boat/world/packet-adapter doubles. JDK25 required.
No native physics, packet accessor, config, Verdict or hook qualification.
"""
import argparse,hashlib,subprocess,tempfile,resource
from pathlib import Path
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path);parser.add_argument('--mutant',choices=['invert','always-refuse','never-refuse']);args=parser.parse_args()
source=args.source or root/'src/main/java/dev/meshac/Vehicle.java'
actual=hashlib.sha256(source.read_bytes()).hexdigest()
assert actual in {'b12f11aa1393acf677113fd25e90fd4f13aef253d9eaf36cab3289c98a0d0cb0','9f1e44dd480eadca12132613fe6cb7c1dad1a751b5074992fcff18403f704b0b'},('Vehicle source drift: review and repin',actual)
text=source.read_text()
if args.mutant:
 old='return Verdict.signal(pl, "boatfly", "boat climbing in open air", 1) != Verdict.Step.LOG;'
 assert text.count(old)==1
 new={'invert':old.replace('!=','=='),'always-refuse':'Verdict.signal(pl, "boatfly", "boat climbing in open air", 1); return true;','never-refuse':'Verdict.signal(pl, "boatfly", "boat climbing in open air", 1); return false;'}[args.mutant]
 text=text.replace(old,new)
print('SOURCE',source,actual,'mutant',args.mutant,flush=True)
sources={
'dev/meshac/Vehicle.java':text,
'dev/meshac/VehicleLogTest.java':(root/'sim/VehicleLogTest.java').read_text(),
'dev/meshac/Verdict.java':'''package dev.meshac;class Verdict {enum Step {LOG,SETBACK,HOLD,REMOVE} static Step next;static int calls;
 static Step signal(net.minecraft.server.level.ServerPlayer p,String check,String detail,int weight){if(!check.equals("boatfly") || weight!=1)throw new AssertionError("signal identity");calls++;return next;}}''',
'dev/meshac/Packets.java':'package dev.meshac;class Packets {static net.minecraft.world.phys.Vec3 pos(net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket p){return new net.minecraft.world.phys.Vec3(p.y);}}',
'net/minecraft/network/protocol/game/ServerboundMoveVehiclePacket.java':'package net.minecraft.network.protocol.game;public class ServerboundMoveVehiclePacket {public double y;public ServerboundMoveVehiclePacket(double y){this.y=y;}}',
'net/minecraft/world/phys/Vec3.java':'package net.minecraft.world.phys;public class Vec3 {public double y;public Vec3(double y){this.y=y;}}',
'net/minecraft/tags/FluidTags.java':'package net.minecraft.tags;public class FluidTags {public static final Object WATER=new Object();}',
'net/minecraft/world/level/block/Blocks.java':'package net.minecraft.world.level.block;public class Blocks {public static final Object BUBBLE_COLUMN=new Object();}',
'net/minecraft/world/entity/Entity.java':'''package net.minecraft.world.entity; public class Entity {
 public boolean water,ground;public final World world=new World();public double getY(){return 0;}public boolean isInWater(){return water;}public boolean onGround(){return ground;}
 public Pos blockPosition(){return new Pos(false);}public World level(){return world;}
 public record Pos(boolean lower){public Pos below(){return new Pos(true);}}
 public static class World {public boolean bubble,belowBubble,belowWater;
 public State getBlockState(Pos pos){return new State(pos.lower()?belowBubble:bubble);}
 public State getFluidState(Pos pos){return new State(belowWater);}}
 public record State(boolean present){public boolean is(Object tag){return present;}}
}''',
'net/minecraft/world/entity/vehicle/boat/AbstractBoat.java':'package net.minecraft.world.entity.vehicle.boat;public class AbstractBoat extends net.minecraft.world.entity.Entity {}',
'net/minecraft/server/level/ServerPlayer.java':'''package net.minecraft.server.level;public class ServerPlayer {
 final java.util.UUID id=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return id;}
 public boolean creative,spectator,other;public final net.minecraft.world.entity.vehicle.boat.AbstractBoat boat=new net.minecraft.world.entity.vehicle.boat.AbstractBoat();
 public boolean isCreative(){return creative;}public boolean isSpectator(){return spectator;}
 public net.minecraft.world.entity.Entity getRootVehicle(){return other?new net.minecraft.world.entity.Entity():boat;}}
'''}
with tempfile.TemporaryDirectory(prefix='vehicle-policy-') as tmp:
 d=Path(tmp);files=[]
 for rel,text in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
 for cmd in [['javac','-J-Xmx256m','-d',str(d/'out')]+files,['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.VehicleLogTest']]:
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=30,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='');assert q.returncode==0,q.returncode
