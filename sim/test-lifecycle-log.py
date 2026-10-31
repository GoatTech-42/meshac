#!/usr/bin/env python3
"""Run real Movement lifecycle regressions against existing scripted doubles.
Queued Blink cleanup and UUID isolation, not native reconnect/physics proof.
JDK25 on PATH; --mutate-forget must fail the authored cleanup assertion.
"""
import hashlib,json,os,resource,subprocess,tempfile,sys
from pathlib import Path
r=Path(__file__).resolve().parents[1]
source=r/'src/main/java/dev/meshac/Movement.java'
expected="6b76bf4eae6101688bd0f238a9f4433420de62dcbf0131885129a2809966d3fa"
actual=hashlib.sha256(source.read_bytes()).hexdigest()
assert actual==expected,("Movement source drift: review and repin before running",actual,expected)
text=source.read_text()
ms,ns=text.count('System.currentTimeMillis()'),text.count('System.nanoTime()')
assert (ms,ns)==(18,2),(ms,ns)
copy=text.replace('System.currentTimeMillis()','TestClock.ms').replace('System.nanoTime()','TestClock.ns')
assert copy.replace('TestClock.ms','System.currentTimeMillis()').replace('TestClock.ns','System.nanoTime()')==text
if '--mutate-forget' in sys.argv:
 old='public static void forget(UUID id) { STATE.remove(id); }'
 assert old in copy
 copy=copy.replace(old,'public static void forget(UUID id) { }')
if '--mutate-clear-all' in sys.argv:
 old='public static void forget(UUID id) { STATE.remove(id); }'
 assert old in copy
 copy=copy.replace(old,'public static void forget(UUID id) { STATE.clear(); }')
print('SOURCE',source,hashlib.sha256(source.read_bytes()).hexdigest(),'clocksites',ms,ns,flush=True)
with tempfile.TemporaryDirectory(prefix='lifecycle-log-') as d:
 d=Path(d);files=[]
 for rel,s in json.loads((r/'sim/doubles.json').read_text()).items():
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s);files.append(str(p))
 p=d/'dev/meshac/Movement.java';p.write_text(copy);files.append(str(p))
 p=d/'dev/meshac/LifecycleLogTest.java';p.write_bytes((r/'sim/LifecycleLogTest.java').read_bytes());files.append(str(p))
 env={k:v for k,v in os.environ.items() if not k.startswith('MESHAC_')}
 def run(cmd):
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,env=env,timeout=60,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   n=log.tell();assert n<4194304,'output cap';log.seek(0);print(log.read().decode(),end='',flush=True)
   assert q.returncode==0,(cmd[0],q.returncode)
 run(['javac','-version']);run(['javac','-J-Xmx256m','-d',str(d/'out')]+files)
 run(['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.LifecycleLogTest'])
