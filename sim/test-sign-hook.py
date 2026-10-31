#!/usr/bin/env python3
"""Compile the full real mixin against a supplied cached 26.1.2 compile classpath,
then invoke its sign hook with focused runtime doubles and real CallbackInfo.
No Mixin application, native server, annotation processing or packaging proof.
JDK25 required. --classpath JSON is an ordered list of local classpath paths.
"""
from pathlib import Path
import subprocess,tempfile,resource,argparse,json,hashlib
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--classpath',type=Path,required=True)
parser.add_argument('--mixin-jar',type=Path,required=True)
parser.add_argument('--source',type=Path)
parser.add_argument('--mutant',choices=['invert','suppress-signal'])
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
source=args.source or root/'src/main/java/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java'
text=source.read_text()
actual=hashlib.sha256(source.read_bytes()).hexdigest()
assert actual in {'e05a2c56784b37ea27f2994f28577f269fda3bf38c6505e3101ded3149160a05','4567cfafbcc8ece4feebcf155875522fe343bf5f4841190bb2cbed0249394aa8'},('Mixin source drift: review and repin',actual)
print('SOURCE',source,actual,flush=True)
if args.mutant:
 old='if (dev.meshac.Config.get().enforce("interact")) ci.cancel();' if args.mutant=='invert' else 'pl.level().getServer().execute(() -> dev.meshac.Verdict.signal(pl, "interact", "sign text filled in " + gap + " ms after the editor opened", 1));'
 assert text.count(old)==1
 text=text.replace(old,old.replace('if (','if (!',1) if args.mutant=='invert' else '')
classpath=json.loads(args.classpath.read_text())
assert isinstance(classpath,list) and all(Path(p).exists() for p in classpath)
def run(command):
 with tempfile.TemporaryFile() as log:
  q=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=25,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
  assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='',flush=True);assert q.returncode==0,q.returncode
sources={
'dev/meshac/ActualSignHookTest.java':(Path(__file__).resolve().parent/'ActualSignHookTest.java').read_text(),
'dev/meshac/Meshac.java':'package dev.meshac;public class Meshac {public static boolean skip(net.minecraft.server.level.ServerPlayer p){return false;}}',
'dev/meshac/Config.java':'package dev.meshac;public class Config {static boolean enforce;public static Config get(){return new Config();}public boolean enforce(String check){if(!check.equals("interact"))throw new AssertionError("policy identity");return enforce;}}',
'dev/meshac/Packets.java':'package dev.meshac;public class Packets {public static String[] lines(net.minecraft.network.protocol.game.ServerboundSignUpdatePacket p){return new String[]{"scripted"};}}',
'dev/meshac/Interact.java':'package dev.meshac;public class Interact {static long gap;public static long signGap(net.minecraft.server.level.ServerPlayer p,String[] lines){return gap;}}',
'dev/meshac/Trace.java':'package dev.meshac;public class Trace {public static void t(net.minecraft.server.level.ServerPlayer p,String s){}}',
'dev/meshac/Verdict.java':'package dev.meshac;public class Verdict {public enum Step {LOG} static int calls;public static Step signal(net.minecraft.server.level.ServerPlayer p,String check,String detail,int weight){calls++;return Step.LOG;}}',
'net/minecraft/network/protocol/game/ServerboundSignUpdatePacket.java':'package net.minecraft.network.protocol.game;public class ServerboundSignUpdatePacket {}',
'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {public ServerLevel level(){return new ServerLevel();}}',
'net/minecraft/server/level/ServerLevel.java':'package net.minecraft.server.level;public class ServerLevel {public net.minecraft.server.MinecraftServer getServer(){return new net.minecraft.server.MinecraftServer();}}',
'net/minecraft/server/MinecraftServer.java':'package net.minecraft.server;public class MinecraftServer {public void execute(Runnable task){task.run();}}'
}
for packet in ['ServerboundMovePlayerPacket','ServerboundMoveVehiclePacket','ServerboundSetCarriedItemPacket','ServerboundUseItemOnPacket','ServerboundUseItemPacket','ServerboundChatCommandPacket','ServerboundChatCommandSignedPacket','ServerboundClientTickEndPacket','ServerboundPlayerCommandPacket','ServerboundPlayerActionPacket','ServerboundContainerClickPacket']:
 sources['net/minecraft/network/protocol/game/'+packet+'.java']='package net.minecraft.network.protocol.game;public class '+packet+' {}'
sources['net/minecraft/network/protocol/Packet.java']='package net.minecraft.network.protocol;public interface Packet {}'
sources['net/minecraft/world/level/BlockGetter.java']='package net.minecraft.world.level;public interface BlockGetter {}'
sources['net/minecraft/world/phys/Vec3.java']='package net.minecraft.world.phys;public class Vec3 {}'
with tempfile.TemporaryDirectory(prefix='actual-sign-hook-') as tmp:
 d=Path(tmp);p=d/'real/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java';p.parent.mkdir(parents=True);p.write_text(text)
 run(['javac','-J-Xmx256m','-proc:none','-cp',':'.join(classpath),'-d',str(d/'real-out'),str(p)])
 files=[]
 for rel,body in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(str(p))
 cp=str(d/'real-out')+':'+str(args.mixin_jar)
 run(['javac','-J-Xmx256m','-proc:none','-cp',cp,'-d',str(d/'out')]+files)
 run(['java','-Xmx256m','-cp',str(d/'out')+':'+cp,'dev.meshac.ActualSignHookTest'])
