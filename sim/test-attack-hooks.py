#!/usr/bin/env python3
"""Compile/invoke both whole attack hooks with bounded API doubles and real CallbackInfo.
Covers the combat attack path and entity-interaction routing on both variants: 26.x has a
separate interact hook, 1.21.x routes non-attack interacts from the shared handleInteract hook.
Optional exact26 cached classpath compile. Not Mixin/AP/native/version runtime proof.
"""
import argparse,hashlib,json,resource,subprocess,tempfile
from pathlib import Path
root=Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--mixin-jar',type=Path,required=True);p.add_argument('--classpath',type=Path);p.add_argument('--mutant',choices=['always-refuse','never-refuse','invert','entity-drop','entity-always-refuse']);p.add_argument('--baseline',action='store_true');p.add_argument('--variant',choices=['attack26','attack1211']);args=p.parse_args()
pins={'attack26': '5feed200bbbb3023def5f1c09d2be2313e3665dbf32f8b7fcba6eb08d83760d3', 'attack1211': '58ad973a4be2304d2421dcd79c29a0c571a251f5d58a5c86dcc30f927f452c9d'}
sources={
 'dev/meshac/ActualAttackHookTest.java':(root/'sim/ActualAttackHookTest.java').read_text(),
 'dev/meshac/Meshac.java':'package dev.meshac;public class Meshac {static boolean skip;public static boolean skip(net.minecraft.server.level.ServerPlayer p){return skip;}}',
 'dev/meshac/Combat.java':'package dev.meshac;public class Combat {static String reason;static int calls;public static String check(net.minecraft.server.level.ServerPlayer p,net.minecraft.world.entity.Entity e){calls++;return reason;}}',
 'dev/meshac/Interact.java':'package dev.meshac;public class Interact {static int calls;static int lastId=-1;static boolean refuse;public static boolean entity(net.minecraft.server.level.ServerPlayer p,int id){calls++;lastId=id;return refuse;}}',
 'dev/meshac/Verdict.java':'package dev.meshac;public class Verdict {public enum Step {LOG,SETBACK,HOLD,REMOVE}static Step step;static int calls;public static Step signal(net.minecraft.server.level.ServerPlayer p,String c,String text,int w){if(!c.equals("combat")||!text.equals("scripted-reach")||w!=2)throw new AssertionError("signal identity");calls++;return step;}}',
 'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {public final ServerLevel world=new ServerLevel();public ServerLevel level(){return world;}}',
 'net/minecraft/server/level/ServerLevel.java':'package net.minecraft.server.level;public class ServerLevel {public boolean target=true;public final net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();public net.minecraft.server.MinecraftServer getServer(){return server;}public net.minecraft.world.entity.Entity getEntity(int id){if(id!=42)throw new AssertionError("entity id");return target?new net.minecraft.world.entity.Entity():null;}}',
 'net/minecraft/server/MinecraftServer.java':'package net.minecraft.server;public class MinecraftServer {public boolean main=true;public boolean isSameThread(){return main;}}',
 'net/minecraft/server/network/ServerGamePacketListenerImpl.java':'package net.minecraft.server.network;public class ServerGamePacketListenerImpl {}',
 'net/minecraft/world/entity/Entity.java':'package net.minecraft.world.entity;public class Entity {public int getId(){return 42;}}',
 'net/minecraft/world/InteractionHand.java':'package net.minecraft.world;public enum InteractionHand {MAIN_HAND}',
 'net/minecraft/world/phys/Vec3.java':'package net.minecraft.world.phys;public class Vec3 {}',
 'net/minecraft/network/protocol/game/ServerboundAttackPacket.java':'package net.minecraft.network.protocol.game;public class ServerboundAttackPacket {public int entityId(){return 42;}}',
 'net/minecraft/network/protocol/game/ServerboundInteractPacket.java':'''package net.minecraft.network.protocol.game;public class ServerboundInteractPacket {public boolean attack=true;public int entityId(){return 42;}public net.minecraft.world.entity.Entity getTarget(net.minecraft.server.level.ServerLevel l){return l.getEntity(42);}public interface Handler {void onInteraction(net.minecraft.world.InteractionHand h);void onInteraction(net.minecraft.world.InteractionHand h,net.minecraft.world.phys.Vec3 v);void onAttack();}public void dispatch(Handler h){if(attack)h.onAttack();else h.onInteraction(net.minecraft.world.InteractionHand.MAIN_HAND);}}''',
}
def run(cmd):
 with tempfile.TemporaryFile() as log:
  r=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=25,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
  assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='',flush=True);assert r.returncode==0,r.returncode
for variant,packet in [('attack26','ServerboundAttackPacket'),('attack1211','ServerboundInteractPacket')]:
 if args.variant and variant!=args.variant:continue
 source=root/'src'/variant/'java/dev/meshac/mixin/AttackMixin.java';text=source.read_text();actual=hashlib.sha256(source.read_bytes()).hexdigest();assert actual==pins[variant],('source drift',variant,actual)
 if args.baseline:text=subprocess.check_output(['git','show','HEAD:src/'+variant+'/java/dev/meshac/mixin/AttackMixin.java'],cwd=root,text=True)
 if args.mutant:
  combat='if (Verdict.signal(player, "combat", hit, 2) != Verdict.Step.LOG) ci.cancel();'
  route26='\t\tif (player.level().getServer().isSameThread() && dev.meshac.Interact.entity(player, p.entityId())) ci.cancel();'
  route1211='\t\t\tEntity entity = p.getTarget(player.level());\n\t\t\tif (entity != null && dev.meshac.Interact.entity(player, entity.getId())) ci.cancel();\n\t\t\treturn;'
  mutants={
   'always-refuse':{v:(combat,'Verdict.signal(player, "combat", hit, 2); ci.cancel();') for v in ('attack26','attack1211')},
   'never-refuse':{v:(combat,'Verdict.signal(player, "combat", hit, 2);') for v in ('attack26','attack1211')},
   'invert':{v:(combat,combat.replace('!=','==')) for v in ('attack26','attack1211')},
   'entity-drop':{'attack26':(route26,''),'attack1211':(route1211,'\t\t\treturn;')},
   'entity-always-refuse':{'attack26':(route26,'\t\tif (player.level().getServer().isSameThread()) { dev.meshac.Interact.entity(player, p.entityId()); ci.cancel(); }'),
                           'attack1211':(route1211,'\t\t\tEntity entity = p.getTarget(player.level());\n\t\t\tif (entity != null) { dev.meshac.Interact.entity(player, entity.getId()); ci.cancel(); }\n\t\t\treturn;')},
  }
  old,new=mutants[args.mutant][variant]
  assert text.count(old)==1,(args.mutant,variant);text=text.replace(old,new)
 print('SOURCE',variant,actual,'baseline',args.baseline,'mutant',args.mutant,flush=True)
 with tempfile.TemporaryDirectory(prefix='attack-hook-') as tmp:
  d=Path(tmp);files=[]
  for rel,body in dict(sources,**{'dev/meshac/mixin/AttackMixin.java':text}).items():
   f=d/rel;f.parent.mkdir(parents=True,exist_ok=True);f.write_text(body);files.append(str(f))
  if variant=='attack26' and args.classpath:
   cp=json.loads(args.classpath.read_text());assert all(Path(x).exists() for x in cp)
   run(['javac','-J-Xmx256m','-proc:none','-cp',':'.join(cp),'-d',str(d/'exact26'),str(d/'dev/meshac/mixin/AttackMixin.java')])
  run(['javac','-J-Xmx256m','-proc:none','-cp',str(args.mixin_jar),'-d',str(d/'out')]+files)
  run(['java','-Xmx256m','-cp',str(d/'out')+':'+str(args.mixin_jar),'dev.meshac.ActualAttackHookTest',packet])
