import json,subprocess,sys,time,os,threading
d=os.path.expanduser("~/meshac-work/rig/smoke-26.1"); bots=os.path.expanduser("~/meshac-work/meshac/rig/bots")
def sh(s,t=100): return subprocess.run(s,shell=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=t).stdout.strip()
esp=sys.argv[1]=="on"
cfg=json.load(open(d+"/config/meshac.json")); cfg["veilEsp"]=esp; cfg["veilXray"]=False
json.dump(cfg,open("/tmp/espcfg.json","w")); sh(f"docker run --rm -v {d}:/srv -v /tmp/espcfg.json:/c.json eclipse-temurin:25-jre sh -c \"cp /c.json /srv/config/meshac.json\"")
sh(f"docker run --rm -v {d}:/srv eclipse-temurin:25-jre sh -c \"rm -rf /srv/flat\"")
sh("docker rm -f meshac-vt")
sh(f"docker run -d --name meshac-vt --memory 2g --cpus 2 --cpuset-cpus 2,3 -v {d}:/srv -w /srv eclipse-temurin:25-jre java -Xmx1400M -jar fabric-server.jar nogui")
for i in range(50):
    time.sleep(4)
    if "Done (" in sh("docker logs meshac-vt 2>&1"): break
ip=sh("docker inspect -f \"{{.NetworkSettings.IPAddress}}\" meshac-vt")
def R(c): return sh(f"docker exec crafty-controller python3 /tmp/rcon.py {ip} 25575 meshacrig \"{c}\"")
R("gamerule spawn_mobs false"); R("forceload add -48 -16 48 48")
V="espv%d"%(int(time.time())%100000); T="espt%d"%(int(time.time())%100000)
def bot(role,name,other,inp=False):
    inpflag="-i" if inp else ""
    cmd=f"docker run --rm {inpflag} -v {bots}:/w -w /w -e HOST={ip} -e ROLE={role} -e NAME={name} -e OTHER={other} -e TTL=200000 node:22-slim node esp.mjs"
    return subprocess.Popen(cmd,shell=True,stdin=subprocess.PIPE if inp else None,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
pv=bot("viewer",V,T); pt=bot("victim",T,V,True)
vl=[];tl=[]
threading.Thread(target=lambda:[vl.append(l) for l in pv.stdout],daemon=True).start()
threading.Thread(target=lambda:[tl.append(l) for l in pt.stdout],daemon=True).start()
time.sleep(14)
marks=[]
def mark(n): marks.append((n,time.time()*1000)); print("MARK",n,flush=True)
def clear(): R("fill -12 -60 2 12 -56 12 air")
def pos(who,x,z): R(f"tp {who} {x} -60 {z} 0 0")
clear(); R("fill -12 -61 -2 60 -61 60 stone"); 
pos(V,0.5,0.5)
# E1 wall + victim behind at 20
R("fill -10 -60 10 10 -57 10 stone"); pos(T,0.5,20.5); time.sleep(1); mark("E1_start"); time.sleep(5); mark("E1_end")
# E2 open: remove wall, victim in open
clear(); time.sleep(0.5); mark("E2_start"); time.sleep(4); mark("E2_end")
# E4 near: wall at z=3, victim z=6
R("fill -10 -60 3 10 -57 3 stone"); pos(T,0.5,6.5); time.sleep(0.5); mark("E4_start"); time.sleep(4); mark("E4_end")
# E5 glass at z=10
clear(); R("fill -10 -60 10 10 -57 10 glass"); pos(T,0.5,20.5); time.sleep(0.5); mark("E5_start"); time.sleep(4); mark("E5_end")
# E9 leaves & fence
clear(); R("fill -10 -60 10 10 -57 10 oak_leaves"); pos(T,0.5,20.5); time.sleep(0.5); mark("E5b_leaves_start"); time.sleep(4); mark("E5b_leaves_end")
# E10 teleport into view after hidden
clear(); R("fill -10 -60 10 10 -57 10 stone"); pos(T,0.5,20.5); time.sleep(5); mark("E10_hidden_ok")
R("fill -10 -60 10 10 -57 10 air"); mark("E10_open"); time.sleep(3); mark("E10_end")
# E3 corner peek: wall z=10 spans x -10..-1, victim walks +x along z=20 from x=-30
clear(); R("fill -10 -60 10 -1 -57 10 stone"); pos(T,-30.5,20.5); time.sleep(3); mark("E3_start")
pt.stdin.write("WALK\n"); pt.stdin.flush(); time.sleep(9); pt.stdin.write("STOP\n"); pt.stdin.flush(); mark("E3_end")
time.sleep(1); pv.terminate(); pt.terminate(); sh("docker rm -f meshac-vt")
S=[l.split() for l in vl if l.startswith("S ")]
S=[(float(a[1]),int(a[2]),a[3],a[4]) for a in S]
def window(n):
    s=[m[1] for m in marks if m[0]==n+"_start"][0]; e=[m[1] for m in marks if m[0]==n+"_end"][0]
    return [x for x in S if s<=x[0]<=e]
def summ(n):
    w=window(n); p=sum(x[1] for x in w); return f"{n}: samples={len(w)} present={p} ({100*p//max(1,len(w))}%) first_present_after_ms={next((int(x[0]-w[0][0]) for x in w if x[1]),None)}"
print("mode",sys.argv[1])
for n in ("E1","E2","E4","E5","E5b_leaves"): print(summ(n))
w=[x for x in S if [m[1] for m in marks if m[0]=="E10_open"][0]<=x[0]]
print("E10 first present ms after opening:",next((int(x[0]-[m[1] for m in marks if m[0]=="E10_open"][0]) for x in w if x[1]),None))
w=window("E3"); f=next((x for x in w if x[1]),None); print("E3 first present x=",f[2] if f else None,"at ms",int(f[0]-w[0][0]) if f else None,"; LOS opens near victim x=-0.55 (centre), walk start after ~3s")
print("victim samples:",[l.strip() for l in tl if l.startswith(("V ","WALKSTART"))][-3:])
open(os.path.expanduser("~/meshac-work/results/final-sweep/esp-%s.raw"%sys.argv[1]),"w").write("".join(vl[-4000:])+"".join(tl[-2000:])+str(marks))
