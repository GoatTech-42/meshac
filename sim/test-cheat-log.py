#!/usr/bin/env python3
"""Compile real Movement against scripted MC/Config/Verdict doubles and run the
cheat positive-control suite round 2: airborne speed above cap, fast packet
cadence (timer), ground step accumulation and microhop must produce signals,
honest walking and slow cadence must stay silent, log-only must never correct,
HOLD mode must set back. Only Movement clocks are rebound and reversal-verified.
No real Minecraft, Verdict, config, threads or physics validation. JDK25 on PATH."""
import hashlib,json,os,resource,subprocess,tempfile
from pathlib import Path
r=Path(__file__).resolve().parents[1]
source=r/'src/main/java/dev/meshac/Movement.java'
text=source.read_text()
ms,ns=text.count('System.currentTimeMillis()'),text.count('System.nanoTime()')
assert (ms,ns)==(18,2),(ms,ns)
copy=text.replace('System.currentTimeMillis()','TestClock.ms').replace('System.nanoTime()','TestClock.ns')
assert copy.replace('TestClock.ms','System.currentTimeMillis()').replace('TestClock.ns','System.nanoTime()')==text
print('SOURCE',source,hashlib.sha256(source.read_bytes()).hexdigest(),'clocksites',ms,ns,flush=True)
with tempfile.TemporaryDirectory(prefix='cheat-log-') as d:
 d=Path(d);files=[]
 for rel,s in json.loads((r/'sim/doubles.json').read_text()).items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s);files.append(str(p))
 p=d/'dev/meshac/Movement.java';p.write_text(copy);files.append(str(p))
 p=d/'dev/meshac/CheatLogTest.java';p.write_bytes((r/'sim/CheatLogTest.java').read_bytes());files.append(str(p))
 env={k:v for k,v in os.environ.items() if not k.startswith('MESHAC_')}
 def run(cmd):
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,env=env,timeout=60,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   n=log.tell();assert n<4194304,'output cap';log.seek(0);print(log.read().decode(),end='',flush=True)
   assert q.returncode==0,(cmd[0],q.returncode)
 run(['javac','-version']);run(['javac','-J-Xmx256m','-d',str(d/'out')]+files)
 run(['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.CheatLogTest'])
