#!/usr/bin/env python3
"""Machine-checks the NEWBOUNDS capture-row spec (rig/newbounds_controls.py). No rig, no server."""
import pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import newbounds_controls

failed = 0
def check(ok, what):
    global failed
    if not ok: print('FAIL ' + what); failed += 1

check(newbounds_controls.validate() is True, 'spec validates')
S = newbounds_controls.S
# one vanilla + one cheat row per newly-bound check family
for fam, cheat in (('fly', 'fly'), ('speed', 'speed'), ('combat', 'combat'), ('mine', 'mining'),
                   ('interact', 'interact'), ('inv', 'inventory')):
    rows = [k for k in S if k.startswith('nb-' + fam + '-')]
    check(len(rows) == 2, fam + ' has a legit and a cheat row')
    z = [k for k in rows if S[k]['expect'] == 'zero']; p = [k for k in rows if S[k]['expect'] == 'signal']
    check(len(z) == 1 and len(p) == 1, fam + ' zero/signal split')
    check(S[p[0]]['checks'] == (cheat,), fam + ' cheat row binds ' + cheat)
# grader accepts a cheat-row result built from this spec (end-to-end through pb_grade)
import pb_grade
r = {'scenario': 'nb-fly-cheat', 'row': S['nb-fly-cheat']['row'], 'player': 'P', 'jarSHA': 's',
     'expect': 'signal', 'session': 's1', 'events': [{'leg': 'hold', 'posDelta': [0, 3.0, 0]}],
     'signals': ['[meshac] SIGNAL P fly hover dy 0.010 heat=2 -> log'], 'tickHealth': '20.0',
     'modLiveness': 'cases ok', 'setupReceipt': 'coords', 'needs': 'cheat', 'offState': None,
     'checks': S['nb-fly-cheat']['checks'], 'expectMove': True}
check(pb_grade.grade(r, [r])['verdict'] == 'PASS', 'spec row grades PASS through pb_grade')
r2 = dict(r); r2['signals'] = ['[meshac] SIGNAL P speed 12.3 heat=1 -> log']
check(pb_grade.grade(r2, [r2])['verdict'] == 'INVALID', 'off-target signal INVALID through pb_grade')

if failed: raise SystemExit('%d failures' % failed)
print('test-newbounds_controls ok')
