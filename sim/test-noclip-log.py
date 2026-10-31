#!/usr/bin/env python3
"""Run real Movement noclip LOG/non-LOG policy checks against scripted doubles.
JDK25 on PATH. Optional positional mode: body, path, leap.
Not native collision, config, Verdict or packet-hook qualification.
"""
import argparse,hashlib,json,os,resource,subprocess,tempfile
from pathlib import Path
r=Path(__file__).resolve().parents[1]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('mode',choices=['body','path','leap'])
parser.add_argument('--baseline',type=Path)
parser.add_argument('--mutant',choices=['body-log','move-log','all-log','pending'])
args=parser.parse_args()
source=args.baseline or r/'src/main/java/dev/meshac/Movement.java'
actual=hashlib.sha256(source.read_bytes()).hexdigest()
expected='9b228604bad3802ef940664701eb2ef0caa653e740b8caf39ae8be60c3fcea56' if args.baseline else 'bc57cf5531bdc79ab7cce403bc23801349a96224fd3b764f9dec2ef148d7bf0b'
assert actual==expected,('Movement source drift: review and repin before running',actual,expected)
text=source.read_text()
ms,ns=text.count('System.currentTimeMillis()'),text.count('System.nanoTime()')
assert (ms,ns)==(18,2),(ms,ns)
copy=text.replace('System.currentTimeMillis()','TestClock.ms').replace('System.nanoTime()','TestClock.ns')
assert copy.replace('TestClock.ms','System.currentTimeMillis()').replace('TestClock.ns','System.nanoTime()')==text
if args.mutant:
 if args.mutant=='body-log':
  old='if (Verdict.signal(pl, "noclip", "moving inside solid blocks", 1) == Verdict.Step.LOG) return null;'
  assert copy.count(old)==1
  copy=copy.replace(old,'Verdict.signal(pl, "noclip", "moving inside solid blocks", 1);')
 elif args.mutant=='move-log':
  old='if (Verdict.signal(pl, "noclip", d2 > 100 ?'
  assert copy.count(old)==1
  copy=copy.replace('Math.sqrt(d2)), 2) == Verdict.Step.LOG) return null;', 'Math.sqrt(d2)), 2) == null) return null;')
 elif args.mutant=='all-log':
  assert copy.count('== Verdict.Step.LOG) return null;')==3
  copy=copy.replace('== Verdict.Step.LOG) return null;','!= null) return null;')
 elif args.mutant=='pending':
  assert copy.count('if (!s.pending &&')==1
  copy=copy.replace('if (!s.pending &&','if (')
print('SOURCE',source,hashlib.sha256(source.read_bytes()).hexdigest(),'clocksites',ms,ns,flush=True)
with tempfile.TemporaryDirectory(prefix='noclip-log-') as d:
 d=Path(d);files=[]
 for rel,s in json.loads((r/'sim/doubles.json').read_text()).items():
  if rel=='dev/meshac/Verdict.java':
   assert 'enum Step { LOG, HOLD }' in s
   s=s.replace('enum Step { LOG, HOLD }','enum Step { SETBACK, HOLD, REMOVE, LOG }')
  p=d/rel;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s);files.append(str(p))
 p=d/'dev/meshac/Movement.java';p.write_text(copy);files.append(str(p))
 p=d/'dev/meshac/NoClipLogTest.java';p.write_bytes((r/'sim/NoClipLogTest.java').read_bytes());files.append(str(p))
 env={k:v for k,v in os.environ.items() if not k.startswith('MESHAC_')}
 def run(cmd):
  with tempfile.TemporaryFile() as log:
   q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,env=env,timeout=60,preexec_fn=lambda:resource.setrlimit(resource.RLIMIT_FSIZE,(4194304,4194304)))
   n=log.tell();assert n<4194304,'output cap';log.seek(0);print(log.read().decode(),end='',flush=True)
   assert q.returncode==0,(cmd[0],q.returncode)
 run(['javac','-version']);run(['javac','-J-Xmx256m','-d',str(d/'out')]+files)
 run(['java','-Xmx256m','-cp',str(d/'out'),'dev.meshac.NoClipLogTest',args.mode])
