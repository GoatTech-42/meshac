#!/usr/bin/env python3
"""Real Movement arrival pairing with scripted clocks, policy and world doubles.
JDK25 required. Not native packets, threads or physics qualification.
"""
import hashlib,json,os,resource,subprocess,tempfile
from pathlib import Path
r=Path(__file__).resolve().parents[1]
import argparse
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--source',type=Path)
parser.add_argument('--mutant',choices=['poll-status','keep-exempt'])
args=parser.parse_args()
source=args.source or r/'src/main/java/dev/meshac/Movement.java'
text=source.read_text()
actual=hashlib.sha256(source.read_bytes()).hexdigest()
assert actual in {'6b76bf4eae6101688bd0f238a9f4433420de62dcbf0131885129a2809966d3fa','bc57cf5531bdc79ab7cce403bc23801349a96224fd3b764f9dec2ef148d7bf0b'},('Movement source drift: review and repin',actual)
if args.mutant:
 old='p.hasPosition() ? s.arrivals.poll() : null'
 assert text.count(old)==1
 new='s.arrivals.poll()' if args.mutant=='poll-status' else '(p.hasPosition() && !Config.get().exempt(pl)) ? s.arrivals.poll() : null'
 text=text.replace(old,new)
ms,ns=text.count('System.currentTimeMillis()'),text.count('System.nanoTime()')
assert (ms,ns)==(18,2),(ms,ns)
copy=text.replace('System.currentTimeMillis()','TestClock.ms').replace('System.nanoTime()','TestClock.ns')
assert copy.replace('TestClock.ms','System.currentTimeMillis()').replace('TestClock.ns','System.nanoTime()')==text
print('SOURCE',source,hashlib.sha256(source.read_bytes()).hexdigest(),'clocksites',ms,ns,flush=True)
with tempfile.TemporaryDirectory(prefix='cheat-log-') as d:
 d=Path(d);files=[]
 for rel,s in json.loads((r/'sim/doubles.json').read_text()).items():
  if rel=='dev/meshac/Config.java': s=s.replace('public static boolean enforce=true;', 'public static boolean enforce=true,exempt=false;').replace('return false;', 'return exempt;')
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s);files.append(str(p))
 p=d/'dev/meshac/Movement.java';p.write_text(copy);files.append(str(p))
 p=d/'dev/meshac/MovementArrivalTest.java';p.write_bytes((Path(__file__).resolve().parent/'MovementArrivalTest.java').read_bytes());files.append(str(p))
 env={k:v for k,v in os.environ.items() if not k.startswith('MESHAC_')}
 def run(cmd):
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,env=env,timeout=60,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   n=log.tell();assert n<4194304,'output cap';log.seek(0);print(log.read().decode(),end='',flush=True)
   assert q.returncode==0,(cmd[0],q.returncode)
 run(['javac','-version']);run(['javac','-J-Xmx256m','-d',str(d/'out')]+files)
 run(['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.MovementArrivalTest'])
