#!/usr/bin/env python3
"""Real Config/Gson preset reload checks in a new temporary directory only. JDK25 and --gson JAR required."""
from pathlib import Path
import argparse,subprocess,tempfile,resource,hashlib
parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Config.java');parser.add_argument('--gson',type=Path,required=True);parser.add_argument('--mutant',action='store_true');args=parser.parse_args()
text=args.source.read_text()
assert hashlib.sha256(text.encode()).hexdigest() in {'e678a105d1693d87efe5c4628da2647c728806f9fef238785b73801fd386329a','279c50ed16b5da9f46759b09475bd28838aac8158f73fdcb4c6d3cd3d030bb4a'},'Config source drift: review and repin'
if args.mutant:
 old='if (loaded.preset == null) throw new IllegalArgumentException("preset must not be null");'
 assert text.count(old)==1;text=text.replace(old,'')
print('SOURCE',args.source,hashlib.sha256(args.source.read_bytes()).hexdigest(),flush=True)
sources={
'dev/meshac/Config.java':text,
'dev/meshac/ConfigPresetTest.java':(Path(__file__).resolve().parent/'ConfigPresetTest.java').read_text(),
'dev/meshac/Ladder.java':(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Ladder.java').read_text(),
'dev/meshac/Meshac.java':'package dev.meshac;class Meshac {static Log LOG=new Log();static class Log {void warn(String s,Object... args){}}}',
'dev/meshac/veil/Veil.java':'package dev.meshac.veil;public class Veil {public static boolean xrayOn,espOn;public static void clear(){}}',
'net/minecraft/server/level/ServerPlayer.java':'package net.minecraft.server.level;public class ServerPlayer {public record Profile(String name){}public Profile getGameProfile(){return new Profile("fixture");}}',
'net/fabricmc/loader/api/FabricLoader.java':'package net.fabricmc.loader.api;public class FabricLoader {public static final java.nio.file.Path dir=java.nio.file.Path.of(System.getProperty("test.config"));public static FabricLoader getInstance(){return new FabricLoader();}public java.nio.file.Path getConfigDir(){return dir;}}'
}
with tempfile.TemporaryDirectory(prefix='cases-load-') as tmp:
 d=Path(tmp);files=[];(d/'config').mkdir()
 for rel,text in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
 for cmd in [['javac','-J-Xmx256m','-cp',str(args.gson),'-d',str(d/'out')]+files,['java','-Xmx256m','-Dtest.config='+str(d/'config'),'-cp',str(d/'out')+':'+str(args.gson),'dev.meshac.ConfigPresetTest']]:
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=30,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='');assert q.returncode==0,q.returncode
