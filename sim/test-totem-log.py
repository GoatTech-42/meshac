#!/usr/bin/env python3
"""Real Totem with scripted clock/verdict/player doubles. JDK25 required; no native hook proof."""
import argparse,hashlib,subprocess,tempfile,resource
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Totem.java');parser.add_argument('--mutant',choices=['always-refuse','never-refuse']);args=parser.parse_args()
text=args.source.read_text()
actual=hashlib.sha256(text.encode()).hexdigest()
assert actual in {'d7c7e4bec6a6f13aec4236d968fdb4cda813182a4e079d3c07ee30bc48135604','8a1b30383d5915bed8db3df572ae185ab82ec948037f41fd6922604cc08e78d3'},('Totem source drift: review and repin',actual)
if args.mutant:
 assert text.count('!= Verdict.Step.LOG;')==1
 text=text.replace('!= Verdict.Step.LOG;','!= null;' if args.mutant=='always-refuse' else '== null;')
copy=text.replace('System.currentTimeMillis()','TestClock.ms');assert copy.replace('TestClock.ms','System.currentTimeMillis()')==text
print('SOURCE',args.source,hashlib.sha256(text.encode()).hexdigest(),flush=True)
sources={
'dev/meshac/Totem.java':copy,
'dev/meshac/TotemLogTest.java':(Path(__file__).resolve().parent/'TotemLogTest.java').read_text(),
'dev/meshac/Trace.java':'package dev.meshac;class Trace {static void t(net.minecraft.server.level.ServerPlayer p,String s){}}',
'dev/meshac/TestClock.java':'package dev.meshac;class TestClock {static long ms=100000;}',
'dev/meshac/Verdict.java':'package dev.meshac;class Verdict {enum Step {LOG,SETBACK,HOLD,REMOVE} static Step next;static int calls;static Step signal(net.minecraft.server.level.ServerPlayer p,String c,String d,int w){if(!c.equals("inventory"))throw new AssertionError("signal identity");calls++;return next;}}',
'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {final java.util.UUID id=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return id;}}'
}
with tempfile.TemporaryDirectory(prefix='totem-policy-') as tmp:
 d=Path(tmp);files=[]
 for rel,body in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(str(p))
 for cmd in [['javac','-J-Xmx256m','-d',str(d/'out')]+files,['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.TotemLogTest']]:
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=30,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='');assert q.returncode==0,q.returncode
