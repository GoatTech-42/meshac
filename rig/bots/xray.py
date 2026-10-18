import json,subprocess,sys,time,os,random
mode=sys.argv[1]; flag=(mode=='on'); v='26.1'
d=os.path.expanduser('~/meshac-work/rig/smoke-26.1'); bots=os.path.expanduser('~/meshac-work/meshac/rig/bots')
def sh(s,t=100): return subprocess.run(s,shell=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=t).stdout.strip()
def ct(cmd): return sh(f'docker run --rm -v {d}:/srv eclipse-temurin:25-jre sh -c "{cmd}"')
sh(f"docker run --rm -v {d}:/srv -v /tmp/setveil.py:/s.py python:3-alpine python3 /s.py {str(flag).lower()}")
ct("rm -rf /srv/flat")
open("/tmp/vt-server.log","w").write(sh("docker logs --tail 80 meshac-vt")); sh("docker rm -f meshac-vt")
sh(f'docker run -d --name meshac-vt --memory 2g --cpus 2 -v {d}:/srv -w /srv eclipse-temurin:25-jre java -Xmx1400M -jar fabric-server.jar nogui')
for i in range(50):
    time.sleep(4)
    if 'Done (' in sh('docker logs meshac-vt 2>&1'): break
ip=sh("docker inspect -f '{{.NetworkSettings.IPAddress}}' meshac-vt")
def R(c): return sh(f"docker exec crafty-controller python3 /tmp/rcon.py {ip} 25575 pwtest \"{c}\"")
R('forceload add -48 -48 48 48')
for y0 in range(-60,-29,7): R(f'fill -32 {y0} -32 32 {min(y0+6,-30)} 32 stone')
R('fill 0 -48 0 6 -44 6 air')
exposed=[(-1,-46,3),(7,-46,3),(3,-43,3)]
for p in exposed: R('setblock %d %d %d diamond_ore'%p)
random.seed(7); truth=set()
while len(truth)<80:
    p=(random.randint(-28,28),random.randint(-58,-34),random.randint(-28,28))
    if -4<=p[0]<=10 and -52<=p[1]<=-40 and -4<=p[2]<=10: continue
    truth.add(p)
for p in truth: R('setblock %d %d %d diamond_ore'%p)
R('say ready')
time.sleep(2)
name='vt%s%d'%(mode,int(time.time())%100000)
cmd=f'docker run --rm -v {bots}:/w -w /w -e HOST={ip} -e NAME={name} node:22-slim node /tmp/xray.mjs'
# tp the bot into the pocket once joined
import threading
P=sorted(t for t in truth if not any((t[0]+a,t[1]+b_,t[2]+c) in truth for a,b_,c in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]))[0]
def rv():
    time.sleep(26); R("setblock %d %d %d air"%(P[0]+1,P[1],P[2]))
threading.Thread(target=rv).start()
def tp():
    time.sleep(6); R(f'tp {name} 3 -48 3')
threading.Thread(target=tp).start()
out=sh(f'cp /tmp/xray.mjs {bots}/xray.mjs; docker run --rm -v {bots}:/w -w /w -e HOST={ip} -e NAME={name} -e PX={P[0]} -e PY={P[1]} -e PZ={P[2]} -e WAIT=16000 node:22-slim node xray.mjs',110)
open("/tmp/vt-server.log","w").write(sh("docker logs --tail 80 meshac-vt")); sh("docker rm -f meshac-vt")
print([l for l in out.split(chr(10)) if l.startswith("REVEAL")],"target",P)
line=[l for l in out.split('\n') if l.startswith('ORES ')]
if not line: print(mode,'NO ORES LINE:',out[:300]); sys.exit(1)
seen={tuple(x[:3]) for x in json.loads(line[0][5:])}
real=truth|set(exposed)
print('RESULT',mode,'truth_buried',len(truth),'exposed',len(exposed),'seen_total',len(seen),
 'real_buried_seen',len(seen&truth),'exposed_seen',len(seen&set(exposed)),'fakes_seen',len(seen-real))
