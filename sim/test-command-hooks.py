#!/usr/bin/env python3
"""Compile the full real mixin against a supplied cached 26.1.2 compile classpath,
then invoke its unsigned/signed command hooks with focused runtime doubles and real CallbackInfo.
No Mixin application, native server, annotation processing or packaging proof.
JDK25 required. --classpath JSON is an ordered list of local classpath paths.
"""
from pathlib import Path
import subprocess,tempfile,resource,argparse,json,hashlib
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--classpath',type=Path,required=True)
parser.add_argument('--mixin-jar',type=Path,required=True)
parser.add_argument('--source',type=Path)
parser.add_argument('--mutant',choices=['suppress-signed','wrong-signed-command','inline-signal'])
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
source=args.source or root/'src/main/java/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java'
text=source.read_text()
actual=hashlib.sha256(source.read_bytes()).hexdigest()
assert actual == '4567cfafbcc8ece4feebcf155875522fe343bf5f4841190bb2cbed0249394aa8', ('Mixin drift',actual)
print('SOURCE',source,actual,flush=True)
if args.mutant:
 start=text.index('private void meshac$signedChatCommand')
 prefix,block=text[:start],text[start:]
 old,new={
  'suppress-signed':('String msg = dev.meshac.Chatter.command(player, p.command());','String msg = null;'),
  'wrong-signed-command':('Chatter.command(player, p.command())','Chatter.command(player, "wrong")'),
  'inline-signal':('pl.level().getServer().execute(() -> dev.meshac.Verdict.signal(pl, "chat", msg, 1));','dev.meshac.Verdict.signal(pl, "chat", msg, 1);'),
 }[args.mutant]
 assert block.count(old)==1; text=prefix+block.replace(old,new)
classpath=json.loads(args.classpath.read_text())
assert isinstance(classpath,list) and all(Path(p).exists() for p in classpath)
def run(command):
 with tempfile.TemporaryFile() as log:
  q=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=25,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
  assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='',flush=True);assert q.returncode==0,q.returncode
sources={
'dev/meshac/ActualCommandHookTest.java':(Path(__file__).resolve().parent/'ActualCommandHookTest.java').read_text(),
'dev/meshac/Meshac.java':'package dev.meshac;public class Meshac {static boolean skip;public static boolean skip(net.minecraft.server.level.ServerPlayer p){return skip;}}',
'dev/meshac/Config.java':'package dev.meshac;public class Config {static boolean enforce;public static Config get(){return new Config();}public boolean enforce(String check){if(!check.equals("interact"))throw new AssertionError("policy identity");return enforce;}}',
'dev/meshac/Packets.java':'package dev.meshac;public class Packets {public static String[] lines(net.minecraft.network.protocol.game.ServerboundSignUpdatePacket p){return new String[]{"scripted"};}}',
'dev/meshac/Chatter.java':'package dev.meshac;public class Chatter {static int calls;static String line,response;public static String command(net.minecraft.server.level.ServerPlayer p,String s){calls++;line=s;return response;}}',
'dev/meshac/Interact.java':'package dev.meshac;public class Interact {static long gap;public static long signGap(net.minecraft.server.level.ServerPlayer p,String[] lines){return gap;}}',
'dev/meshac/Trace.java':'package dev.meshac;public class Trace {public static void t(net.minecraft.server.level.ServerPlayer p,String s){}}',
'dev/meshac/Verdict.java':'package dev.meshac;public class Verdict {public enum Step {LOG} static int calls;public static Step signal(net.minecraft.server.level.ServerPlayer p,String check,String detail,int weight){if(!check.equals("chat")||!detail.equals("scripted-cadence")||weight!=1)throw new AssertionError("signal identity");calls++;return Step.LOG;}}',
'net/minecraft/network/protocol/game/ServerboundSignUpdatePacket.java':'package net.minecraft.network.protocol.game;public class ServerboundSignUpdatePacket {}',
'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {final ServerLevel world=new ServerLevel();public ServerLevel level(){return world;}}',
'net/minecraft/server/level/ServerLevel.java':'package net.minecraft.server.level;public class ServerLevel {final net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();public net.minecraft.server.MinecraftServer getServer(){return server;}}',
'net/minecraft/server/MinecraftServer.java':'package net.minecraft.server;public class MinecraftServer {public final java.util.List<Runnable> tasks=new java.util.ArrayList<>();public void execute(Runnable task){tasks.add(task);}}'
}
for packet in ['ServerboundMovePlayerPacket','ServerboundMoveVehiclePacket','ServerboundSetCarriedItemPacket','ServerboundUseItemOnPacket','ServerboundUseItemPacket','ServerboundChatCommandPacket','ServerboundChatCommandSignedPacket','ServerboundClientTickEndPacket','ServerboundPlayerCommandPacket','ServerboundPlayerActionPacket','ServerboundContainerClickPacket']:
 sources['net/minecraft/network/protocol/game/'+packet+'.java']='package net.minecraft.network.protocol.game;public class '+packet+' {}'
for name in ['ServerboundChatCommandPacket','ServerboundChatCommandSignedPacket']:
 sources['net/minecraft/network/protocol/game/'+name+'.java']='package net.minecraft.network.protocol.game;public record '+name+'(String command) {}'
sources['net/minecraft/network/protocol/Packet.java']='package net.minecraft.network.protocol;public interface Packet {}'
sources['net/minecraft/world/level/BlockGetter.java']='package net.minecraft.world.level;public interface BlockGetter {}'
sources['net/minecraft/world/phys/Vec3.java']='package net.minecraft.world.phys;public class Vec3 {}'
with tempfile.TemporaryDirectory(prefix='actual-command-hook-') as tmp:
 d=Path(tmp);p=d/'real/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java';p.parent.mkdir(parents=True);p.write_text(text)
 run(['javac','-J-Xmx256m','-proc:none','-cp',':'.join(classpath),'-d',str(d/'real-out'),str(p)])
 files=[]
 for rel,body in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(str(p))
 cp=str(d/'real-out')+':'+str(args.mixin_jar)
 run(['javac','-J-Xmx256m','-proc:none','-cp',cp,'-d',str(d/'out')]+files)
 run(['java','-Xmx256m','-cp',str(d/'out')+':'+cp,'dev.meshac.ActualCommandHookTest'])
