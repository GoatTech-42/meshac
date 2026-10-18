#!/usr/bin/env python3
"""Prepared-client A15 negative controls. Draft runner for the meshac rig rep."""
import argparse, datetime, hashlib, json, os, random, subprocess, sys, time
from pathlib import Path

def intervals(kind):
    if kind=='one-hz': return [1.0]*30
    if kind=='cooldown': return [.625]*48
    if kind=='cooldown-jitter':
        rng=random.Random(1516);return [.625+rng.uniform(-.075,.15) for _ in range(48)]
    if kind=='target-switch': return [.75]*40
    if kind in ('low-tps','packet-bunched'): return [1.25]*24
    raise ValueError(kind)

def main():
    ap=argparse.ArgumentParser(description=__doc__)
    ap.add_argument('scenario',choices=['one-hz','cooldown','cooldown-jitter','target-switch','low-tps','packet-bunched'])
    ap.add_argument('--harness',type=Path,default=Path('/home/luke/meshac-work/meadow-meshac-round1-20261015'))
    ap.add_argument('--output',type=Path,required=True)
    ap.add_argument('--player',default='McR1CombatCtl16')
    ap.add_argument('--jar-sha256',required=True)
    ap.add_argument('--execute',action='store_true')
    ap.add_argument('--prepared-vanilla-client-confirmed',action='store_true')
    ap.add_argument('--allow-tickrate-change',action='store_true')
    ap.add_argument('--network-evidence',type=Path,help='Rep receipt of a real TCP stall/release fixture. JVM pause is not equivalent.')
    a=ap.parse_args();gaps=intervals(a.scenario)
    if not a.execute:
        print(json.dumps({'scenario':a.scenario,'gaps':gaps,'verdictWait':30,'execution':'requires prepared vanilla client, existing A15 targets, reviewed jar, module-OFF pixel check'},indent=2));return
    if not a.prepared_vanilla_client_confirmed: raise SystemExit('Confirm reviewed module-OFF pixels and an already running vanilla control client.')
    if a.scenario=='low-tps' and not a.allow_tickrate_change: raise SystemExit('Rig rep must approve global tick-rate change explicitly.')
    if a.scenario=='packet-bunched' and (not a.network_evidence or not a.network_evidence.is_file()):
        raise SystemExit('No actual network-batch fixture receipt. Do not label ordinary clicks or a paused JVM as packet batching.')
    if a.output.exists() and any(a.output.iterdir()): raise SystemExit('Output folder not empty; do not overwrite evidence.')
    sys.path.insert(0,str(a.harness));from helpers import rc,x
    jar=Path('/home/luke/meshac-work/rig/red-26.2/mods/meshac.jar')
    if hashlib.sha256(jar.read_bytes()).hexdigest()!=a.jar_sha256: raise SystemExit('Rig jar differs from reviewed candidate.')
    if int(Path('/proc/meminfo').read_text().split('MemAvailable:')[1].split()[0])<2097152: raise SystemExit('RAM guard')
    if os.statvfs('/').f_bavail*os.statvfs('/').f_frsize<3072*1048576: raise SystemExit('Disk guard')
    if a.player not in rc('list'): raise SystemExit('Prepared player not online')
    pid=int((a.harness/'client-control.pid').read_text());cmd=Path(f'/proc/{pid}/cmdline').read_bytes()
    if b'client-control' not in cmd: raise SystemExit('Control PID ownership mismatch')
    # No client start/stop, server/world copies, entity kills or jar/config writes.
    a.output.mkdir(parents=True,exist_ok=True)
    def shot(name): subprocess.run(['import','-display',':90','-window','root',str(a.output/name)],check=True)
    def health(): return [rc(f'data get entity @e[tag=r1combat16_{i},limit=1] Health') for i in range(4)]
    initial=health()
    if any('500' not in h for h in initial): raise SystemExit('Rep must reset all four existing 500HP targets before each case.')
    w=subprocess.check_output(['xdotool','search','--onlyvisible','--pid',str(pid)],env={**os.environ,'DISPLAY':':90'},text=True).splitlines()[-1]
    x('windowfocus',w);shot('before.png')
    since=datetime.datetime.now(datetime.timezone.utc).isoformat();events=[];rate_changed=False
    try:
        if a.scenario=='low-tps':
            query=rc('tick query')
            if '20.0' not in query and '20.00' not in query: raise RuntimeError('Expected original20TPS. Review tick query instead of assuming a restore rate: '+query)
            rc('tick rate 10');rate_changed=True
        rc('tp '+a.player+' 155.5 70 155.5 0 0');time.sleep(2)
        start=time.monotonic();deadline=start
        for i,gap in enumerate(gaps):
            # RCON view change tests the packet classifier on legal angles, not human aiming skill.
            if a.scenario=='target-switch':
                rc('tp '+a.player+' 155.5 70 155.5 '+str([0,-90,180,90][i%4])+' 0');time.sleep(.12)
            x('click','1');events.append({'hit':i,'t':time.monotonic()-start})
            deadline+=gap;time.sleep(max(0,deadline-time.monotonic()))
        if rate_changed:rc('tick rate 20');rate_changed=False
        time.sleep(30);shot('end.png')
    finally:
        if rate_changed:rc('tick rate 20')
        log=subprocess.check_output(['docker','logs','meshac-red','--since',since],stderr=subprocess.STDOUT,text=True)
        (a.output/'server.log').write_text(log)
        result={'scenario':a.scenario,'player':a.player,'jarSHA':a.jar_sha256,'initialHP':initial,'finalHP':health(),'events':events,
            'signals':[l for l in log.splitlines() if 'SIGNAL '+a.player in l],
            'traceRows':sum('[a15] player='+a.player+' ' in l for l in log.splitlines()),
            'networkFixtureEvidence':a.network_evidence.read_text() if a.network_evidence else None,
            'reviewRequired':'Inspect before/end pixels, damage and trace rows. Zero signals without actual attacks is INVALID. RCON target switching is not a human motor baseline.'}
        (a.output/'result.json').write_text(json.dumps(result,indent=2))
        print(json.dumps(result,indent=2))
if __name__=='__main__':main()
