#!/usr/bin/env python3
"""Real Discord/Gson callbacks; invalid URLs and loopback only. JDK25 and --gson JAR required."""
import subprocess,tempfile,resource,os,argparse,hashlib
from pathlib import Path
parser=argparse.ArgumentParser(description='Real Discord/Gson failure isolation and loopback submission checks. No external requests.')
parser.add_argument('--source',type=Path,default=Path(__file__).resolve().parents[1]/'src/main/java/dev/meshac/Discord.java')
parser.add_argument('--gson',type=Path,required=True)
args=parser.parse_args()
gson=str(args.gson)
actual=hashlib.sha256(args.source.read_bytes()).hexdigest()
assert actual in {'c1e0161f1f35882e12f301cf9ed52ed9f9ad4690f9afc7305cc4c83be7fb38d0','f216e9e0239eb88d5f6a180d6bb761aaee5d9c51291f67395f6a694c63f282ac'},('Discord source drift: review and repin',actual)
print('SOURCE',args.source,actual,flush=True)
sources={
'dev/meshac/Discord.java':args.source.read_text(),
'dev/meshac/DiscordInvalidTest.java':(Path(__file__).resolve().parent/'DiscordInvalidTest.java').read_text(),
'dev/meshac/Config.java':'package dev.meshac;class Config {static Config cur=new Config(); static Config get(){return cur;} String discordWebhook;boolean discordOnHold=true;}',
'dev/meshac/Meshac.java':'package dev.meshac;class Meshac {static Log LOG=new Log();static class Log {final java.util.List<String> lines=new java.util.ArrayList<>();void warn(String s,Object... args){lines.add(s+java.util.Arrays.toString(args));}}}',
'dev/meshac/Cases.java':'package dev.meshac;class Cases {static class Case {long until;String action="kick",player="test",id="test",reason="scripted";java.util.List<String> evidence=java.util.List.of();}}'
}
with tempfile.TemporaryDirectory(prefix='discord-invalid-') as tmp:
 d=Path(tmp);files=[]
 for rel,text in sources.items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(str(p))
 for cmd in [['javac','-J-Xmx256m','-cp',gson,'-d',str(d/'out')]+files,['java','-Xmx256m','-cp',str(d/'out')+':'+gson,'dev.meshac.DiscordInvalidTest']]:
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=30,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   assert log.tell()<4194304,'output cap';log.seek(0);print(log.read().decode(),end='');assert q.returncode==0,q.returncode
