#!/usr/bin/env python3
"""Summarize MESHAC_TRACE A15 samples. Data only: never a cheat verdict."""
import argparse, collections, json, math, re, statistics
from pathlib import Path

def percentile(xs, q):
    if not xs: return None
    xs=sorted(xs); return xs[round((len(xs)-1)*q)]

def describe(xs):
    return {'n':len(xs), 'min':min(xs) if xs else None, 'p50':percentile(xs,.5),
            'p95':percentile(xs,.95), 'max':max(xs) if xs else None,
            'stdev':statistics.pstdev(xs) if xs else None}

def parse(text):
    rows=[]
    for line in text.splitlines():
        if '[a15] ' not in line: continue
        raw=dict(re.findall(r'(\w+)=([^\s]+)',line.split('[a15] ',1)[1]))
        required=('player','target','tickGap','gapMs','aimErr','cd','yawStep','pitchStep','eyeStep','centreStep','pingMs')
        if not all(k in raw for k in required): continue
        try:
            row={k:float(v) if k not in ('player','verdict','ground') else v for k,v in raw.items()}
            if not all(math.isfinite(row[k]) for k in required if k!='player'): continue
        except ValueError: continue
        rows.append(row)
    return rows

def summarize(rows):
    result={}
    for player in sorted({r['player'] for r in rows}):
        rs=[r for r in rows if r['player']==player]; gaps=[r['gapMs'] for r in rs if r['gapMs']>=0]
        result[player]={'hits':len(rs),'targets':len({r['target'] for r in rs}),
            'gapMs':describe(gaps),'aimErr':describe([r['aimErr'] for r in rs]),
            'cooldown':describe([r['cd'] for r in rs]),
            'tickGaps':dict(collections.Counter(str(int(r['tickGap'])) for r in rs)),
            'stillViewHits':sum(abs(r['yawStep'])<.0001 and abs(r['pitchStep'])<.0001 for r in rs),
            'sameTickHits':sum(r['tickGap']==0 for r in rs),
            'movingEyeHits':sum(r['eyeStep']>.05 for r in rs),
            'movingTargetHits':sum(r['centreStep']>.05 for r in rs),
            'warning':'Server handling cadence is not client click cadence. Regular timing and still aim are legal. No enforcement verdict.'}
    return result

if __name__=='__main__':
    ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('logs',nargs='+',type=Path)
    args=ap.parse_args()
    print(json.dumps({str(p):summarize(parse(p.read_text(errors='replace'))) for p in args.logs},indent=2))
