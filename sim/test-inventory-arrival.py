#!/usr/bin/env python3
"""Real Inventory arrival pairing with policy/player doubles. JDK25; no packet hook proof."""
import argparse,hashlib,subprocess,tempfile,resource
from pathlib import Path
parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Inventory.java');parser.add_argument('--resume-only',action='store_true');parser.add_argument('--mutant',choices=['peek','ignore-arrival']);args=parser.parse_args()
text=args.source.read_text()
actual=hashlib.sha256(text.encode()).hexdigest()
assert actual in {'9dd157bb1cdf2655df48f87b475ffac56200ccc4c572f194988ab35f5284521c','3ba69bd6065c78be33929e4362d8f9c7c1c2c94e9a19756ff2205c78e377ac3a'},('Inventory source drift: review and repin',actual)
if args.mutant:
 old='s.arrivals.poll()' if args.mutant=='peek' else 'at != null ? at : System.currentTimeMillis()'
 new='s.arrivals.peek()' if args.mutant=='peek' else 'System.currentTimeMillis()'
 assert text.count(old)==1;text=text.replace(old,new)
copy=text.replace('System.currentTimeMillis()','TestClock.ms');assert copy.replace('TestClock.ms','System.currentTimeMillis()')==text
print('SOURCE',args.source,hashlib.sha256(text.encode()).hexdigest(),flush=True)
sources={
'dev/meshac/Inventory.java':copy,
'dev/meshac/InventoryArrivalTest.java':(Path(__file__).resolve().parent/'InventoryArrivalTest.java').read_text(),
'dev/meshac/TestClock.java':'package dev.meshac;class TestClock {static long ms;}',
'dev/meshac/Config.java':'package dev.meshac;class Config {static boolean exempt;static Config get(){return new Config();} boolean exempt(net.minecraft.server.level.ServerPlayer p){return exempt;}}',
'dev/meshac/Verdict.java':'package dev.meshac;class Verdict {static int calls;static void signal(net.minecraft.server.level.ServerPlayer p,String c,String d,int w){calls++;}}',
'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {final java.util.UUID id=java.util.UUID.randomUUID();public java.util.UUID getUUID(){return id;}public boolean creative,spectator;public boolean isCreative(){return creative;}public boolean isSpectator(){return spectator;}}'
}
with tempfile.TemporaryDirectory(prefix='inventory-arrival-') as tmp:
 d=Path(tmp);files=[]
 for rel,body in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(body);files.append(str(p))
 for cmd in [['javac','-J-Xmx256m','-d',str(d/'out')]+files,['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.InventoryArrivalTest']+(['resume'] if args.resume_only else [])]:
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=30,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='');assert q.returncode==0,q.returncode
