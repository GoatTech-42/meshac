#!/usr/bin/env python3
"""Emit/validate an offline dense-load profiling plan. No client/server commands."""
import argparse,json,math,statistics
from pathlib import Path

def make_plan():
 return {'schema':'meshac.dense-profile.v1','mode':'PLAN_ONLY','rig':'meshac-red','players':40,'nearbyEntitiesPerPlayer':100,
  'stages':[{'players':n,'warmupSeconds':60,'measureSeconds':120,'repetitions':3} for n in [1,4,10,20,40]],
  'pairs':['same arena meshac-disabled baseline','candidate checks on, TRACE_OFF, no capture','candidate checks on, four captures, TRACE_OFF'],
  'limits':{'memAvailableKiB':2097152,'rootFreeMiB':3072,'totalNativeClientsOnBox':2,'syntheticBotPermission':'requires rep approval of a bounded non-rendering protocol-bot design before40connections'},
  'stop':['MemAvailable below2GiB','tick p95 exceeds50ms','GC pause exceeds200ms','throttling/disconnect/challenge','unexpected production traffic or target route'],
  'readouts':['per-tick monotonic total/check durations','LOSclip/current/snapshot counts','nearby entity samples','active users and valid attacks','GCcount/time','heap used/committed','processCPUtime','RSS','queue delay and tick overrun'],
  'validity':['existing isolated arena/world only','no public routes/ilhsmc','screenshot/native fixture verifies actual movement/attacks','baseline proof metric within10percent','no40render clients; max2native clients total','raw data labels source/synthetic and exact jarSHA/config','TRACE_FULLabsent','never fabricate empty-stage PASS'],
  'pending':['40synthetic protocol bots/route not implemented here','in-process per-check timer hooks not wired','actual RAM/CPUbudget measured before approval','GCmetrics collector adapter','rep owns run scheduling/fixture cleanup']}

def summarize(rows):
 if not rows:raise ValueError('no measured rows')
 out={}
 for row in rows:
  if not isinstance(row,dict):raise ValueError('row')
  for k in ['stage','tick','tickNs','checkNs','gcPauseNs','validPlayers','attacks']:
   if k not in row:raise ValueError('missing '+k)
  if not isinstance(row['checkNs'],dict):raise ValueError('checkNs')
  for x in [row['tickNs'],row['gcPauseNs'],*row['checkNs'].values()]:
   if isinstance(x,bool) or not isinstance(x,(float,int)) or not math.isfinite(x) or x<0:raise ValueError('invalid duration')
  if sum(row['checkNs'].values())>row['tickNs']:raise ValueError('nested/overlapping timers require separate accounting')
  if row['validPlayers']<=0 or row['attacks']<0:raise ValueError('invalid execution proof')
  out.setdefault(row['stage'],[]).append(row)
 result={}
 for stage,rs in out.items():
  ticks=sorted(r['tickNs']/1e6 for r in rs);keys=set().union(*(r['checkNs'] for r in rs))
  result[stage]={'ticks':len(rs),'tickMsP50':statistics.median(ticks),'tickMsP95':ticks[math.ceil(.95*len(ticks))-1],
   'checkMeanMs':{k:sum(r['checkNs'].get(k,0) for r in rs)/len(rs)/1e6 for k in keys},
   'gcPauseMsTotal':sum(r['gcPauseNs'] for r in rs)/1e6,'validPlayersMin':min(r['validPlayers'] for r in rs),
   'attacks':sum(r['attacks'] for r in rs),'status':'DESCRIPTIVE_ONLY_NOT_CLEARANCE'}
 return result
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--summarize',type=Path);a=p.parse_args()
 print(json.dumps(summarize([json.loads(l) for l in a.summarize.read_text().splitlines() if l.strip()]) if a.summarize else make_plan(),indent=2))
