#!/usr/bin/env python3
"""Real Cases/Gson reload checks in a new temporary directory only. JDK25 and --gson JAR required."""
from pathlib import Path
import argparse,subprocess,tempfile,resource,hashlib
parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Cases.java');parser.add_argument('--gson',type=Path,required=True);parser.add_argument('--mutant',action='store_true');args=parser.parse_args()
text=args.source.read_text()
assert hashlib.sha256(text.encode()).hexdigest() in {'d2bcfaf1844c1b06caa127bdea3b115996ac76e1dbf0f10fd86088ff1e7da254','e457f17f29b458030c8422a69ef516dfd9c6e74abde57ec12cdaa6c807b11a96'},'Cases source drift: review and repin'
print('SOURCE',args.source,hashlib.sha256(text.encode()).hexdigest(),flush=True)
if args.mutant:
 old='if (loaded == null) throw new IllegalArgumentException("case store must be an array, not null");'
 assert text.count(old)==1
 text=text.replace(old,'if (loaded == null) loaded = new java.util.ArrayList<>();')
sources={
'dev/meshac/Cases.java':text,
'dev/meshac/CasesLoadTest.java':(Path(__file__).resolve().parent/'CasesLoadTest.java').read_text(),
'dev/meshac/Config.java':'package dev.meshac;class Config {static Config get(){return new Config();}double offenceHalfLifeDays(){return 14;}}',
'dev/meshac/Ladder.java':(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Ladder.java').read_text(),
'dev/meshac/Meshac.java':'package dev.meshac;class Meshac {static Log LOG=new Log();static class Log {void warn(String s,Object... args){}}}',
'net/fabricmc/loader/api/FabricLoader.java':'package net.fabricmc.loader.api;public class FabricLoader {public static final java.nio.file.Path dir=java.nio.file.Path.of(System.getProperty("test.config"));public static FabricLoader getInstance(){return new FabricLoader();}public java.nio.file.Path getConfigDir(){return dir;}}'
}
with tempfile.TemporaryDirectory(prefix='cases-load-') as tmp:
 d=Path(tmp);files=[];(d/'config').mkdir()
 for rel,text in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
 for cmd in [['javac','-J-Xmx256m','-cp',str(args.gson),'-d',str(d/'out')]+files,['java','-Xmx256m','-Dtest.config='+str(d/'config'),'-cp',str(d/'out')+':'+str(args.gson),'dev.meshac.CasesLoadTest']]:
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=30,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='');assert q.returncode==0,q.returncode
