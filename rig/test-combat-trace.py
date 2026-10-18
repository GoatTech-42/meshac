#!/usr/bin/env python3
import importlib.util
from pathlib import Path
spec=importlib.util.spec_from_file_location('combat_trace', Path(__file__).with_name('analyze-combat-trace.py'))
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)

def line(gap=625,tick=12,yaw=0,pitch=0,err=.2):
    return f'[a15] player=Legit target=8 wallMs=1 tick=2 tickGap={tick} gapMs={gap} reach=2.5 rewindReach=2.5 aimErr={err} cd=1.0 yaw=0 pitch=0 yawStep={yaw} pitchStep={pitch} eyeStep=0 centreStep=0 targetVx=0 targetVy=0 targetVz=0 pingMs=80 slot=0 ground=true verdict=null'
assert m.parse('old-format attack target=1')==[]
assert m.parse('[a15] player=Truncated target=1')==[]
assert m.parse(line(err='nan'))==[]
assert m.parse(line(err='bad'))==[]
r=m.summarize(m.parse('\n'.join(line() for _ in range(40))))['Legit']
assert r['hits']==40 and r['stillViewHits']==40 and r['gapMs']['stdev']==0
assert 'verdict' not in r and 'No enforcement verdict' in r['warning']
r=m.summarize(m.parse(line(gap=-1,tick=-1)+'\n'+line(gap=0,tick=0)))['Legit']
assert r['gapMs']['n']==1 and r['sameTickHits']==1
print('PASS: malformed/old logs, non-finite input, perfectly regular legal clicks, first hit and same-tick batching')
