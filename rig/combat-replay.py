#!/usr/bin/env python3
"""Strict synthetic combat corpus loader and pure geometry oracle, not Minecraft."""
import json, math
from pathlib import Path

def vec(v,n,label):
    if not isinstance(v,list) or len(v)!=n or any(isinstance(x,bool) or not isinstance(x,(int,float)) or not math.isfinite(x) for x in v): raise ValueError(label)
    return v

def load(path):
    d=json.loads(Path(path).read_text())
    if d.get('schema')!='meshac.combat-replay.v1' or d.get('provenance')!='synthetic' or not isinstance(d.get('cases'),list):raise ValueError('schema/provenance/cases')
    ids=set()
    for c in d['cases']:
        if not isinstance(c.get('id'),str) or c['id'] in ids:raise ValueError('duplicate/missing case id')
        ids.add(c['id'])
        if not isinstance(c.get('samples'),list) or not c['samples']:raise ValueError('samples')
        previous=-1
        for s in c['samples']:
            if isinstance(s.get('tick'),bool) or not isinstance(s.get('tick'),int) or s['tick']<=previous:raise ValueError('ticks')
            previous=s['tick'];vec(s.get('eye'),3,'eye');b=vec(s.get('box'),6,'box')
            if any(b[i]>b[i+3] for i in range(3)):raise ValueError('inverted box')
            if s.get('visibility') not in ('visible','blocked','unknown'):raise ValueError('visibility')
        a=c['attack'];vec(a.get('look'),3,'look')
        if abs(sum(x*x for x in a['look'])-1)>.00001:raise ValueError('normalized look required')
        for k in ('sentMs','handledMs'):
            x=a.get(k)
            if isinstance(x,bool) or not isinstance(x,(int,float)) or not math.isfinite(x) or x<0:raise ValueError(k)
        if a['handledMs']<a['sentMs']:raise ValueError('handling before sending')
        if c.get('expected') not in ('allow_geometry','deny_reach','deny_los','deny_aim'):raise ValueError('expectation')
    return d

def distance(eye,b):return math.sqrt(sum(max(b[i]-eye[i],0,eye[i]-b[i+3])**2 for i in range(3)))

def ray(eye,look,b,inflate=.6):
    lo,hi=0.,6.
    for i in range(3):
        mn,mx=b[i]-inflate,b[i+3]+inflate
        if abs(look[i])<1e-12:
            if eye[i]<mn or eye[i]>mx:return False
        else:
            a,z=(mn-eye[i])/look[i],(mx-eye[i])/look[i]
            lo=max(lo,min(a,z));hi=min(hi,max(a,z))
            if hi<lo:return False
    return True

def replay(c):
    # All supplied pairs are coherent observations. The oracle has no block world,
    # packets or vanilla physics: visibility is fixture input, not raycast output.
    samples=c['samples'];look=c['attack']['look']
    best=min(distance(s['eye'],s['box']) for s in samples)
    if best>3.2:return 'deny_reach'
    if all(s['visibility']=='blocked' for s in samples):return 'deny_los'
    if not any(ray(s['eye'],look,s['box']) for s in samples):return 'deny_aim'
    return 'allow_geometry'

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('corpus',type=Path);a=p.parse_args()
    d=load(a.corpus)
    for c in d['cases']:
        actual=replay(c);print(c['id'],actual)
        if actual!=c['expected']:raise SystemExit('mismatch '+c['id'])
