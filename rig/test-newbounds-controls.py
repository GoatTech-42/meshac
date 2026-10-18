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
# ground-spoof zero-FP battery rows: GC01 + GC05 are reviewer GO conditions on patch 1
gc = [k for k in S if k.startswith('nb-gc')]
check(sorted(gc) == ['nb-gc01-sneakedge', 'nb-gc02-autostep', 'nb-gc05-jb-powder'], 'GC battery rows present: ' + str(gc))
for k in gc:
    check(S[k]['expect'] == 'zero' and S[k]['needs'] == 'vanilla', k + ' is a vanilla zero row')
r1 = S['nb-gc01-sneakedge']['row']
check('ZERO nofall SIGNAL' in r1 and '300s' in r1 and '150 cycles' in r1, 'GC01 pre-commits the 5-minute (300s) zero-flag jitter bound, bound IN the legs')
check(sum(l[3] for l in S['nb-gc01-sneakedge']['legs']) == 300, 'GC01 legs total 300s (the pre-committed bound, not 16s)')
check('DRAFT until' in r1, 'GC01 stays a DRAFT until the driver receipt proves duration')
check('no silent widening' in r1, 'GC01 forbids silent widening')
r5 = S['nb-gc05-jb-powder']['row']
check('Jump Boost' in r5 and '0 nofall flags' in r5, 'GC05 binds Jump Boost + the 0-flag acceptance')
check('dy' in r5 and 'never widen' in r5, 'GC05 pre-commits dy reporting over silent band widening')
check('POWDER-EXEMPT-DRAFT' in r5, 'GC05 names the flag-route doc')
check('/effect give @p jump_boost 60 0' in r5 and '/effect give @p jump_boost 60 1' in r5, 'GC05 sets JB I then JB II executably')
check('leather_boots' in r5 and 'PHASE 2' in r5 and '/effect clear' in r5, 'GC05 verifies no boots, names both phases, and tears down')
check('DRAFT until' in r5, 'GC05 stays a DRAFT until the setup receipts are captured')
# single-row positive controls whose legit twins already exist in other families
for k, chk, twin in (('nb-nuker-cheat', ('mining',), 'nb-mine-legit'), ('nb-ghosthand-cheat', ('interact',), 'nb-interact-legit')):
    check(S[k]['expect'] == 'signal' and S[k]['checks'] == chk and 'module' in S[k], k + ' is a bound cheat row')
    check(twin in S and S[twin]['expect'] == 'zero', k + ' names an existing legit twin ' + twin)
    check(twin in S[k]['row'], k + ' row text names its twin')
# microhop: legit row only - the attack side is honestly blocked (no meadow microhop module)
mh = [k for k in S if k.startswith('nb-microhop-')]
check(mh == ['nb-microhop-legit'] and S['nb-microhop-legit']['expect'] == 'zero', 'microhop: legit row only until a real attack exists')
check('ceiling' in S['nb-microhop-legit']['row'] and 'lowCeiling' in S['nb-microhop-legit']['row'], 'microhop legit row exercises the lowCeiling guard')
# receipt-backed DRAFT labels on the rows whose setup/effect the reviewer flagged
for k in ('nb-boatfly-cheat', 'nb-ghosthand-cheat', 'nb-glide-legit', 'nb-glide-cheat',
          'nb-totem-legit', 'nb-totem-cheat', 'nb-kb-legit', 'nb-kb-cheat'):
    check('DRAFT until' in S[k]['row'], k + ' stays a DRAFT until receipts')
for k in ('nb-ghosthand-cheat', 'nb-boatfly-cheat', 'nb-totem-cheat', 'nb-kb-cheat'):
    check('INCONCLUSIVE' in S[k]['row'], k + ': no attack-effect proof = INCONCLUSIVE, never EVADE')
check('/summon minecraft:zombie' in S['nb-kb-legit']['row'], 'kb rows make the zombie-hitter precondition executable')
# kb cheat row must NOT require self-movement (the point of the attack is standing still)
check(S['nb-kb-cheat']['expectMove'] is False, 'kb cheat row is stationary by design')
# one vanilla + one cheat row per newly-bound check family
for fam, cheat in (('fly', 'fly'), ('speed', 'speed'), ('combat', 'combat'), ('mine', 'mining'),
                   ('interact', 'interact'), ('inv', 'inventory'), ('boatfly', 'boatfly'),
                   ('high', 'high'), ('timer', 'timer'), ('glide', 'glide'), ('totem', 'inventory'),
                   ('kb', 'antiknockback')):
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
