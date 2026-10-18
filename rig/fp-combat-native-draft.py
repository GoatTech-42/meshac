#!/usr/bin/env python3
"""Prepared-client FP-1105/1107 combat draft. meshac-red only. No setup or runs by default."""
import argparse,datetime,hashlib,json,os,socket,stat,subprocess,sys,time
from pathlib import Path

def plan(row,cps):
    if row=='FP-1105':
        return {'row':row,'durationSeconds':60,'cps':cps,'strafeSwitchSeconds':2,'radius':[2,2.6,3],
                'needs':'existing pillar/fence/door/ledge arena and moving vanilla target, actual orbit pilot/camera control, baseline damage proof; this driver alone does not prove an orbit'}
    return {'row':row,'durationSeconds':120,'cps':cps,'holdMs':[500,1000,2000],
            'needs':'reviewed existing loopback TCP proxy and receipt dir; separate150/250/400ms jitter/loss fixture remains rep-owned, not implemented by HOLD'}

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('row',choices=['FP-1105','FP-1107']);p.add_argument('--cps',type=int,choices=[6,8,10],default=8)
    p.add_argument('--harness',type=Path,default=Path('/home/luke/meshac-work/meadow-meshac-round1-20261015'))
    p.add_argument('--output',type=Path,required=True);p.add_argument('--manifest',type=Path);p.add_argument('--execute',action='store_true')
    a=p.parse_args();scenario=plan(a.row,a.cps)
    if not a.execute:print(json.dumps(scenario,indent=2));return
    if not a.manifest:raise SystemExit('Rep manifest required. No automatic arena/client/proxy setup.')
    m=json.loads(a.manifest.read_text())
    for key in ['rig','jarSHA256','player','clientPid','moduleOffPixelsReviewed','arenaReady','baselineDamage','targetHealthCommand','controlSocket','receiptDir','proxyRigBindingReviewed']:
        if key not in m:raise SystemExit('Missing manifest '+key)
    if m['rig']!='meshac-red' or m['moduleOffPixelsReviewed'] is not True or m['arenaReady'] is not True:raise SystemExit('rig/review gate')
    if not isinstance(m['baselineDamage'],(int,float)) or m['baselineDamage']<=0:raise SystemExit('disabled-baseline damage proof required')
    # This draft is not an arbitrary RCON executor.
    if not m['player'].replace('_','').isalnum():raise SystemExit('player name')
    if not m['targetHealthCommand'].startswith('data get entity @e[tag=r1combat'):raise SystemExit('Expected existing rig combat target selector')
    if '\n' in m['targetHealthCommand']:raise SystemExit('one read command only')
    if a.output.exists() and any(a.output.iterdir()):raise SystemExit('No overwrite')
    jar=Path('/home/luke/meshac-work/rig/red-26.2/mods/meshac.jar')
    if hashlib.sha256(jar.read_bytes()).hexdigest()!=m['jarSHA256']:raise SystemExit('reviewed rig jar changed')
    pid=int(m['clientPid']);cmd=Path(f'/proc/{pid}/cmdline').read_bytes()
    if b'client-control' not in cmd:raise SystemExit('prepared control PID ownership')
    if int(Path('/proc/meminfo').read_text().split('MemAvailable:')[1].split()[0])<2097152:raise SystemExit('RAM guard')
    if os.statvfs('/').f_bavail*os.statvfs('/').f_frsize<3072*1048576:raise SystemExit('disk guard')
    if a.row=='FP-1107':
        if m['proxyRigBindingReviewed'] is not True:raise SystemExit('proxy route signoff required')
        ctl=Path(m['controlSocket'])
        if not stat.S_ISSOCK(ctl.stat().st_mode) or ctl.stat().st_uid!=os.getuid():raise SystemExit('owned Unix control socket required')
    sys.path.insert(0,str(a.harness));from helpers import rc,x
    if m['player'] not in rc('list'):raise SystemExit('control not online')
    a.output.mkdir(parents=True,exist_ok=True)
    def shot(name):subprocess.run(['import','-display',':90','-window','root',str(a.output/name)],check=True)
    initial=rc(m['targetHealthCommand']);since=datetime.datetime.now(datetime.timezone.utc).isoformat();events=[]
    try:
        # Rep must put the prepared client in-game and verify pixels before execution.
        shot('before.png');start=time.monotonic();next_click=start;next_hold=start+20;holds=iter([500,1000,2000]);hold=next(holds,None);direction='a'
        if a.row=='FP-1105':x('keydown',direction)
        while time.monotonic()-start<scenario['durationSeconds']:
            now=time.monotonic()
            if now>=next_click:x('click','1');events.append({'clickAt':now-start});next_click+=1/a.cps
            if a.row=='FP-1105' and int((now-start)/2)%2 != (0 if direction=='a' else 1):
                x('keyup',direction);direction='d' if direction=='a' else 'a';x('keydown',direction)
            if a.row=='FP-1107' and hold is not None and now>=next_hold:
                with socket.socket(socket.AF_UNIX,socket.SOCK_STREAM) as s:
                    s.settimeout(5);s.connect(m['controlSocket']);s.sendall(f'HOLD {hold}\n'.encode());reply=s.recv(2048).decode()
                if not reply.startswith('OK '):raise RuntimeError('proxy rejected HOLD: '+reply)
                events.append({'holdMs':hold,'reply':reply,'at':now-start});hold=next(holds,None);next_hold+=30
            time.sleep(.002)
        time.sleep(30);shot('end.png')
    finally:
        x('keyup','a');x('keyup','d')
        log=subprocess.check_output(['docker','logs','meshac-red','--since',since],stderr=subprocess.STDOUT,text=True)
        (a.output/'server.log').write_text(log)
        (a.output/'result.json').write_text(json.dumps({'scenario':scenario,'manifest':m,'initialHealth':initial,'finalHealth':rc(m['targetHealthCommand']),
            'events':events,'signals':[l for l in log.splitlines() if 'SIGNAL '+m['player']+' ' in l],
            'proofStatus':'UNVERIFIED: rep must inspect pixels, moving target/orbit, damage within10% disabled baseline and actual proxy receipt byte queues/flush. No zero-signal PASS by default.'},indent=2))
if __name__=='__main__':main()
