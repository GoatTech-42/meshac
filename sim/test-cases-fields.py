#!/usr/bin/env python3
"""Real Cases/Gson reload checks in a new temporary directory only. JDK25 and --gson JAR required."""
from pathlib import Path
import argparse,subprocess,tempfile,resource,hashlib
parser=argparse.ArgumentParser();parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Cases.java');parser.add_argument('--gson',type=Path,required=True);parser.add_argument('--field-mutant',choices=['id','player','uuid','action','reason','by','evidence','evidence-entry']);args=parser.parse_args()
text=args.source.read_text()
assert hashlib.sha256(text.encode()).hexdigest() in {'938546e10107b694db21cc15c366518b2c15037f5c0462651e66272e9c735bc6','e53b5d795a3405e7c9a60bb1971a24b12b95e429c2875590bf24d96b2e0a2dc6'},'Cases source drift: review and repin'
if args.field_mutant:
 old='c.evidence == null || c.evidence.stream().anyMatch(java.util.Objects::isNull)' if args.field_mutant=='evidence' else ('c.evidence.stream().anyMatch(java.util.Objects::isNull)' if args.field_mutant=='evidence-entry' else 'c.'+args.field_mutant+' == null || ')
 assert text.count(old)==1
 text=text.replace(old,'false' if args.field_mutant in ['evidence','evidence-entry'] else '')
print('SOURCE',args.source,hashlib.sha256(text.encode()).hexdigest(),flush=True)
sources={
'dev/meshac/Cases.java':text,
'dev/meshac/CasesFieldsTest.java':(Path(__file__).resolve().parent/'CasesFieldsTest.java').read_text(),
'dev/meshac/Config.java':'package dev.meshac;class Config {static Config get(){return new Config();}double offenceHalfLifeDays(){return 14;}}',
'dev/meshac/Ladder.java':(Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Ladder.java').read_text(),
'dev/meshac/Meshac.java':'package dev.meshac;class Meshac {static Log LOG=new Log();static class Log {void warn(String s,Object... args){}}}',
'net/fabricmc/loader/api/FabricLoader.java':'package net.fabricmc.loader.api;public class FabricLoader {public static final java.nio.file.Path dir=java.nio.file.Path.of(System.getProperty("test.config"));public static FabricLoader getInstance(){return new FabricLoader();}public java.nio.file.Path getConfigDir(){return dir;}}'
}
with tempfile.TemporaryDirectory(prefix='cases-load-') as tmp:
 d=Path(tmp);files=[];(d/'config').mkdir()
 for rel,text in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
 for cmd in [['javac','-J-Xmx256m','-cp',str(args.gson),'-d',str(d/'out')]+files,['java','-Xmx256m','-Dtest.config='+str(d/'config'),'-cp',str(d/'out')+':'+str(args.gson),'dev.meshac.CasesFieldsTest']]:
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=30,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304;log.seek(0);print(log.read().decode(),end='');assert q.returncode==0,q.returncode
