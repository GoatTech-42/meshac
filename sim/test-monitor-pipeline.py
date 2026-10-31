#!/usr/bin/env python3
"""Compile the full real mixin against a supplied cached 26.1.2 compile classpath,
then invoke its container hook with real Inventory/Totem/Verdict/Ladder with focused runtime doubles and real CallbackInfo.
No Mixin application, native server, annotation processing or packaging proof.
JDK25 required. --classpath JSON is an ordered list of local classpath paths.
"""
from pathlib import Path
import subprocess,tempfile,resource,argparse,json,hashlib
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--classpath',type=Path,required=True)
parser.add_argument('--mixin-jar',type=Path,required=True)
parser.add_argument('--source',type=Path)
parser.add_argument('--mutant',choices=['monitor-bypass','egregious-ignore','totem-always-refuse','totem-never-refuse'])
args=parser.parse_args()
root=Path(__file__).resolve().parents[1]
source=args.source or root/'src/main/java/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java'
text=source.read_text()
actual=hashlib.sha256(source.read_bytes()).hexdigest()
assert actual == '4567cfafbcc8ece4feebcf155875522fe343bf5f4841190bb2cbed0249394aa8', ('Mixin drift',actual)
print('SOURCE',source,actual,flush=True)
classpath=json.loads(args.classpath.read_text())
assert isinstance(classpath,list) and all(Path(p).exists() for p in classpath)
def run(command):
 with tempfile.TemporaryFile() as log:
  q=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,timeout=25,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
  assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='',flush=True);assert q.returncode==0,q.returncode
sources={
'dev/meshac/ActualMonitorPipelineTest.java':(Path(__file__).resolve().parent/'ActualMonitorPipelineTest.java').read_text(),
'dev/meshac/Meshac.java':'package dev.meshac;public class Meshac {static boolean skip;public static boolean skip(net.minecraft.server.level.ServerPlayer p){return skip;}}',
'dev/meshac/Config.java':'package dev.meshac;public class Config {static boolean enforce;public static Config get(){return new Config();}public boolean enforce(String check){if(!check.equals("interact"))throw new AssertionError("policy identity");return enforce;}}',
'dev/meshac/Packets.java':'package dev.meshac;public class Packets {public static String[] lines(net.minecraft.network.protocol.game.ServerboundSignUpdatePacket p){return new String[]{"scripted"};}}',
'dev/meshac/Chatter.java':'package dev.meshac;public class Chatter {static int calls;static String line,response;public static String command(net.minecraft.server.level.ServerPlayer p,String s){calls++;line=s;return response;}}',
'dev/meshac/Interact.java':'package dev.meshac;public class Interact {static long gap;public static long signGap(net.minecraft.server.level.ServerPlayer p,String[] lines){return gap;}}',
'dev/meshac/Trace.java':'package dev.meshac;public class Trace {public static void t(net.minecraft.server.level.ServerPlayer p,String s){}}',
'dev/meshac/Verdict.java':'package dev.meshac;public class Verdict {public enum Step {LOG} static int calls;public static Step signal(net.minecraft.server.level.ServerPlayer p,String check,String detail,int weight){calls++;return Step.LOG;}}',
'net/minecraft/network/protocol/game/ServerboundSignUpdatePacket.java':'package net.minecraft.network.protocol.game;public class ServerboundSignUpdatePacket {}',
'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {final ServerLevel world=new ServerLevel();public ServerLevel level(){return world;}}',
'net/minecraft/server/level/ServerLevel.java':'package net.minecraft.server.level;public class ServerLevel {final net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();public net.minecraft.server.MinecraftServer getServer(){return server;}}',
'net/minecraft/server/MinecraftServer.java':'package net.minecraft.server;public class MinecraftServer {public final java.util.List<Runnable> tasks=new java.util.ArrayList<>();public void execute(Runnable task){tasks.add(task);}}'
}
inventory=root/'src/main/java/dev/meshac/Inventory.java'
raw=inventory.read_text();assert hashlib.sha256(inventory.read_bytes()).hexdigest()=='3ba69bd6065c78be33929e4362d8f9c7c1c2c94e9a19756ff2205c78e377ac3a'
print('INVENTORY',hashlib.sha256(inventory.read_bytes()).hexdigest(),'mutant',args.mutant,flush=True)
clocked=raw.replace('System.currentTimeMillis()','TestClock.ms');assert clocked.replace('TestClock.ms','System.currentTimeMillis()')==raw
sources['dev/meshac/Inventory.java']=clocked
sources['dev/meshac/TestClock.java']='package dev.meshac;class TestClock {static long ms;}'
sources['dev/meshac/Config.java']='package dev.meshac;public class Config {static boolean exempt;public static Config get(){return new Config();}public boolean exempt(net.minecraft.server.level.ServerPlayer p){return exempt;}}'
totem=root/'src/main/java/dev/meshac/Totem.java';totemRaw=totem.read_text()
assert hashlib.sha256(totem.read_bytes()).hexdigest()=='8a1b30383d5915bed8db3df572ae185ab82ec948037f41fd6922604cc08e78d3'
print('TOTEM',hashlib.sha256(totem.read_bytes()).hexdigest(),flush=True)
if args.mutant in ['totem-always-refuse','totem-never-refuse']:
 old='return Verdict.signal(pl, "inventory",';assert totemRaw.count(old)==1
 if args.mutant=='totem-always-refuse':totemRaw=totemRaw.replace(' != Verdict.Step.LOG;', ' != null;')
 else:totemRaw=totemRaw.replace(' != Verdict.Step.LOG;', ' == null;')
totemClocked=totemRaw.replace('System.currentTimeMillis()','TestClock.ms');assert totemClocked.replace('TestClock.ms','System.currentTimeMillis()')==totemRaw
sources['dev/meshac/Totem.java']=totemClocked
sources['dev/meshac/Verdict.java']='package dev.meshac;public class Verdict {public enum Step {LOG,SETBACK,HOLD,REMOVE}static Step step;static int calls,weight;static String detail;public static Step signal(net.minecraft.server.level.ServerPlayer p,String c,String d,int w){if(!c.equals("inventory"))throw new AssertionError("signal identity");calls++;detail=d;weight=w;return step;}}'
sources['net/minecraft/server/MinecraftServer.java']='package net.minecraft.server;public class MinecraftServer {public boolean main;public boolean isSameThread(){return main;}public MinecraftServer getPlayerList(){return this;}public net.minecraft.server.level.ServerPlayer getPlayer(java.util.UUID id){return null;}}'
sources['net/minecraft/server/level/ServerLevel.java']='package net.minecraft.server.level;public class ServerLevel {public final net.minecraft.server.MinecraftServer server=new net.minecraft.server.MinecraftServer();public net.minecraft.server.MinecraftServer getServer(){return server;}}'
sources['net/minecraft/server/level/ServerPlayer.java']='package net.minecraft.server.level;public class ServerPlayer {final java.util.UUID id=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return id;}public boolean creative,spectator;public boolean isCreative(){return creative;}public boolean isSpectator(){return spectator;}public final ServerLevel world=new ServerLevel();public ServerLevel level(){return world;}public record Profile(String name){}public Profile getGameProfile(){return new Profile("fixture");}public final Connection connection=new Connection();public static class Connection {public int calls;public void disconnect(Object o){calls++;}}public void sendSystemMessage(Object o){}public final net.minecraft.world.inventory.AbstractContainerMenu menu=new net.minecraft.world.inventory.AbstractContainerMenu();public final net.minecraft.world.inventory.AbstractContainerMenu containerMenu=menu;}'
sources['net/minecraft/world/inventory/AbstractContainerMenu.java']='package net.minecraft.world.inventory;public class AbstractContainerMenu {public int syncs;public void sendAllDataToRemote(){syncs++;}}'
for packet in ['ServerboundMovePlayerPacket','ServerboundMoveVehiclePacket','ServerboundSetCarriedItemPacket','ServerboundUseItemOnPacket','ServerboundUseItemPacket','ServerboundChatCommandPacket','ServerboundChatCommandSignedPacket','ServerboundClientTickEndPacket','ServerboundPlayerCommandPacket','ServerboundPlayerActionPacket','ServerboundContainerClickPacket']:
 sources['net/minecraft/network/protocol/game/'+packet+'.java']='package net.minecraft.network.protocol.game;public class '+packet+' {}'
for name in ['ServerboundChatCommandPacket','ServerboundChatCommandSignedPacket']:
 sources['net/minecraft/network/protocol/game/'+name+'.java']='package net.minecraft.network.protocol.game;public record '+name+'(String command) {}'
sources['net/minecraft/network/protocol/game/ServerboundContainerClickPacket.java']='package net.minecraft.network.protocol.game;public class ServerboundContainerClickPacket {final int c;final short s;final byte b;public ServerboundContainerClickPacket(int c,int s,int b){this.c=c;this.s=(short)s;this.b=(byte)b;}public int containerId(){return c;}public short slotNum(){return s;}public byte buttonNum(){return b;}}'
sources['net/minecraft/network/protocol/Packet.java']='package net.minecraft.network.protocol;public interface Packet {}'
sources['net/minecraft/world/level/BlockGetter.java']='package net.minecraft.world.level;public interface BlockGetter {}'
sources['net/minecraft/world/phys/Vec3.java']='package net.minecraft.world.phys;public class Vec3 {}'
sources['dev/meshac/Config.java']='package dev.meshac; class Config {\n static Config cur=new Config();static Config get(){return cur;} boolean skip=true,perma=false,monitor=false,egregious=false;\n Ladder.Params ladderParams(){return Ladder.Params.defaults();} boolean monitor(){return monitor;}\n boolean isEgregious(String c){return egregious;} boolean exempt(net.minecraft.server.level.ServerPlayer p){return false;} int holdAtI(){return 3;} int kicks(){return 1;}\n boolean explicitLadder(){return false;}int[] tempMinutes(){return new int[]{10,60};}\n double tempBase(){return 10;}double tempGrowth(){return 6;}double tempMax(){return 10080;}\n boolean perma(){return perma;}boolean permanentNeedsTierA(){return true;}boolean skipRungOnTierA(){return skip;}\n}'
sources['dev/meshac/Cases.java']='package dev.meshac; class Cases {\n static double effective;static Case last;static int adds;static double offences(java.util.UUID id){return effective;}\n static class Case {String id="test",action,player;long until;}\n static Case add(String name,java.util.UUID id,String action,String why,String by,long until,java.util.List<String> evidence){\n adds++;Case c=new Case();c.player=name;c.action=action;c.until=until;last=c;return c;}\n}'
sources['dev/meshac/Discord.java']='package dev.meshac; class Discord { static void hold(String a,String b,String c){} static void post(Cases.Case c){} }'
sources['dev/meshac/Screens.java']='package dev.meshac; class Screens { static Object kick(String why,Cases.Case c){return c;} static Object ban(Cases.Case c){return c;} }'
sources['dev/meshac/Meshac.java']='package dev.meshac;public class Meshac {static boolean skip;public static boolean skip(net.minecraft.server.level.ServerPlayer p){return skip;}static final Log LOG=new Log();static class Log {int signals;void warn(String s,Object... a){if(s.contains("SIGNAL"))signals++;}}}'
sources['net/minecraft/network/chat/Component.java']='package net.minecraft.network.chat; public class Component { public static Component literal(String s){return new Component();} }'
for name,pin in {'Verdict.java': 'fd0ab11edf55dceb20a1b1ce9290f1e90acfa4248e135b472ecca0bf2f8d1e26', 'Ladder.java': '33f504b9135316feb6d50407c203516215f847e3f2be421e80d237625ffbd493'}.items():
 p=root/'src/main/java/dev/meshac'/name
 raw=p.read_text();assert hashlib.sha256(p.read_bytes()).hexdigest()==pin,('source drift',name)
 print('REAL',name,pin,'mutant',args.mutant,flush=True)
 if name=='Verdict.java' and args.mutant=='monitor-bypass':raw=raw.replace('Config.get().monitor() && !Config.get().isEgregious(check)', 'false')
 if name=='Verdict.java' and args.mutant=='egregious-ignore':raw=raw.replace('Config.get().monitor() && !Config.get().isEgregious(check)', 'Config.get().monitor()')
 clocked=raw.replace('System.currentTimeMillis()','TestClock.ms');assert clocked.replace('TestClock.ms','System.currentTimeMillis()')==raw
 sources['dev/meshac/'+name]=clocked
with tempfile.TemporaryDirectory(prefix='actual-monitor-pipeline-') as tmp:
 d=Path(tmp);p=d/'real/dev/meshac/mixin/ServerGamePacketListenerImplMixin.java';p.parent.mkdir(parents=True);p.write_text(text)
 run(['javac','-J-Xmx256m','-proc:none','-cp',':'.join(classpath),'-d',str(d/'real-out'),str(p)])
 files=[]
 for rel,body in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(str(p))
 cp=str(d/'real-out')+':'+str(args.mixin_jar)
 run(['javac','-J-Xmx256m','-proc:none','-cp',cp,'-d',str(d/'out')]+files)
 run(['java','-Xmx256m','-cp',str(d/'out')+':'+cp,'dev.meshac.ActualMonitorPipelineTest'])
