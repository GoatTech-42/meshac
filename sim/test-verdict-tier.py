#!/usr/bin/env python3
"""Compile real Verdict/Ladder with controlled external-effect doubles. JDK25 required.
Tests removal evidence custody, not live bans, persistence, Minecraft or networking.
"""
import os, subprocess, tempfile, resource, hashlib, argparse
from pathlib import Path
root=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--baseline',type=Path)
parser.add_argument('--mutant',choices=['lost-tier','all-tier'])
parser.add_argument('--case',choices=['all','permanent'],default='all')
args=parser.parse_args()
sources={
'dev/meshac/Verdict.java':(args.baseline or root/'src/main/java/dev/meshac/Verdict.java').read_text(),
'dev/meshac/Ladder.java':(root/'src/main/java/dev/meshac/Ladder.java').read_text(),
'dev/meshac/VerdictTierTest.java':(root/'sim/VerdictTierTest.java').read_text(),
'dev/meshac/TestClock.java':'package dev.meshac; class TestClock { static long ms=100000; }',
'dev/meshac/Config.java':'''package dev.meshac; class Config {
 static Config cur=new Config();static Config get(){return cur;} boolean skip=true,perma=false,monitor=false;
 Ladder.Params ladderParams(){return Ladder.Params.defaults();} boolean monitor(){return monitor;}
 boolean isEgregious(String c){return false;} int holdAtI(){return 3;} int kicks(){return 1;}
 boolean explicitLadder(){return false;}int[] tempMinutes(){return new int[]{10,60};}
 double tempBase(){return 10;}double tempGrowth(){return 6;}double tempMax(){return 10080;}
 boolean perma(){return perma;}boolean permanentNeedsTierA(){return true;}boolean skipRungOnTierA(){return skip;}
}''',
'dev/meshac/Cases.java':'''package dev.meshac; class Cases {
 static double effective;static Case last;static int adds;static double offences(java.util.UUID id){return effective;}
 static class Case {String id="test",action,player;long until;}
 static Case add(String name,java.util.UUID id,String action,String why,String by,long until,java.util.List<String> evidence){
 adds++;Case c=new Case();c.player=name;c.action=action;c.until=until;last=c;return c;}
}''',
'dev/meshac/Discord.java':'package dev.meshac; class Discord { static void hold(String a,String b,String c){} static void post(Cases.Case c){} }',
'dev/meshac/Screens.java':'package dev.meshac; class Screens { static Object kick(String why,Cases.Case c){return c;} static Object ban(Cases.Case c){return c;} }',
'dev/meshac/Meshac.java':'package dev.meshac; class Meshac { static final Log LOG=new Log(); static class Log { void warn(String s,Object... args){} } }',
'net/minecraft/network/chat/Component.java':'package net.minecraft.network.chat; public class Component { public static Component literal(String s){return new Component();} }',
'net/minecraft/server/level/ServerPlayer.java':'''package net.minecraft.server.level;
public class ServerPlayer {
 private final java.util.UUID id=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return id;}
 public record Profile(String name){} public Profile getGameProfile(){return new Profile("test");}
 public final Connection connection=new Connection();public static class Connection {public int calls;public void disconnect(Object o){calls++;}}
 public void sendSystemMessage(Object o){} public Level level(){return new Level();}
 public static class Level {public Server getServer(){return new Server();}}
 public static class Server {public Players getPlayerList(){return new Players();}}
 public static class Players {public ServerPlayer getPlayer(java.util.UUID id){return null;}}
}'''}
expected_verdict='b3e390f8622ff8aef8531d9f59ac6a5cf1291b1b931807b491c59f238d47592b' if args.baseline else 'fd0ab11edf55dceb20a1b1ce9290f1e90acfa4248e135b472ecca0bf2f8d1e26'
assert hashlib.sha256(sources['dev/meshac/Verdict.java'].encode()).hexdigest()==expected_verdict,'Verdict source drift: review and repin'
assert hashlib.sha256(sources['dev/meshac/Ladder.java'].encode()).hexdigest()=='33f504b9135316feb6d50407c203516215f847e3f2be421e80d237625ffbd493','Ladder source drift: review and repin'
if args.mutant:
 old='tierA = h.lh.confirmedTierA();'
 assert sources['dev/meshac/Verdict.java'].count(old)==1
 sources['dev/meshac/Verdict.java']=sources['dev/meshac/Verdict.java'].replace(old,'tierA = '+('false' if args.mutant=='lost-tier' else 'true')+';')
for rel in ['dev/meshac/Verdict.java','dev/meshac/Ladder.java']:
 text=sources[rel];print('SOURCE',rel,hashlib.sha256(text.encode()).hexdigest(),flush=True)
 copy=text.replace('System.currentTimeMillis()','TestClock.ms')
 assert copy.replace('TestClock.ms','System.currentTimeMillis()')==text
 sources[rel]=copy
with tempfile.TemporaryDirectory(prefix='verdict-tier-') as tmp:
 directory=Path(tmp);files=[]
 for rel,text in sources.items():
  p=directory/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
 env={k:v for k,v in os.environ.items() if not k.startswith('MESHAC_')}
 def run(command):
  with tempfile.TemporaryFile() as log:
   result=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT,env=env,timeout=30,
    preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='',flush=True)
   assert result.returncode==0,(command[0],result.returncode)
 run(['javac','-J-Xmx256m','-d',str(directory/'out')]+files)
 run(['java','-Xmx256m','-cp',str(directory/'out'),'dev.meshac.VerdictTierTest',args.case])
