#!/usr/bin/env python3
"""Machine-checks the NEWBOUNDS capture-row spec (rig/newbounds_controls.py). No rig, no server.
SOURCE-SHAPE CHECKS ONLY: these pins verify the spec's structure (fields, commands, DRAFT labels,
pre-committed bounds) - they do NOT prove any runner executes the setup or validates receipts."""
import os, pathlib, sys
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
_GC01_TOTAL = 301 if os.environ.get('MESHAC_TEST_MUTATE') == 'gc01' else 300  # mutation hook (0039)
check(sum(l[3] for l in S['nb-gc01-sneakedge']['legs']) == _GC01_TOTAL, 'GC01 legs total 300s (the pre-committed bound, not 16s)')
check('DRAFT until' in r1, 'GC01 stays a DRAFT until the driver receipt proves duration')
check('no silent widening' in r1, 'GC01 forbids silent widening')
r5 = S['nb-gc05-jb-powder']['row']
check('Jump Boost' in r5 and '0 nofall flags' in r5, 'GC05 binds Jump Boost + the 0-flag acceptance')
check('dy' in r5 and 'never widen' in r5, 'GC05 pre-commits dy reporting over silent band widening')
check('POWDER-EXEMPT-DRAFT' in r5, 'GC05 names the flag-route doc')
g5 = S['nb-gc05-jb-powder']
check(g5['setup_rcon'] == ['/effect give @p minecraft:jump_boost 60 0', '/clear @p minecraft:leather_boots'], 'GC05 setup_rcon sets JB I + verifies no boots')
check(g5['mid_rcon'] == ['/effect give @p minecraft:jump_boost 60 1', '/clear @p minecraft:leather_boots'], 'GC05 mid_rcon sets JB II + re-verifies')
check(g5['teardown_rcon'] == ['/effect clear @p minecraft:jump_boost'], 'GC05 tears down')
check('DRAFT until' in r5, 'GC05 stays a DRAFT until the setup receipts are captured')
# structured setup/effect fields: driver-executable, not prose
for k in ('nb-gc05-jb-powder', 'nb-boatfly-cheat', 'nb-glide-legit', 'nb-glide-cheat',
          'nb-totem-legit', 'nb-totem-cheat', 'nb-kb-legit', 'nb-kb-cheat'):
    for f in ('setup_rcon', 'mid_rcon', 'teardown_rcon'):
        if f in S[k]:
            check(isinstance(S[k][f], list) and S[k][f] and all(isinstance(c, str) and c.startswith('/') for c in S[k][f]),
                  k + '.' + f + ' is a non-empty list of RCON commands')
for k in ('nb-boatfly-cheat', 'nb-ghosthand-cheat', 'nb-glide-cheat', 'nb-totem-cheat', 'nb-kb-cheat', 'nb-scaffold-cheat'):
    check('effect_check' in S[k], k + ' binds a server-confirmed effect_check')
gh = S['nb-ghosthand-cheat']
check('packets observed' not in gh['row'] and 'server-confirmed' in gh['effect_check'], 'GhostHand: packets alone are not effect proof - server-confirmed target change required')
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
          'nb-totem-legit', 'nb-totem-cheat', 'nb-kb-legit', 'nb-kb-cheat', 'nb-scaffold-legit', 'nb-scaffold-cheat'):
    check('DRAFT until' in S[k]['row'], k + ' stays a DRAFT until receipts')
for k in ('nb-ghosthand-cheat', 'nb-boatfly-cheat', 'nb-totem-cheat', 'nb-kb-cheat', 'nb-scaffold-cheat'):
    check('INCONCLUSIVE' in S[k]['row'], k + ': no attack-effect proof = INCONCLUSIVE, never EVADE')
check('/summon minecraft:zombie 158.5 70 160' in S['nb-kb-legit']['setup_rcon'] and '/summon minecraft:zombie 158.5 70 160' in S['nb-kb-cheat']['setup_rcon'], 'kb rows make the zombie-hitter precondition executable')
# kb cheat row must NOT require self-movement (the point of the attack is standing still)
check(S['nb-kb-cheat']['expectMove'] is False, 'kb cheat row is stationary by design')
# one vanilla + one cheat row per newly-bound check family
for fam, cheat in (('fly', 'fly'), ('speed', 'speed'), ('combat', 'combat'), ('mine', 'mining'),
                   ('interact', 'interact'), ('inv', 'inventory'), ('boatfly', 'boatfly'),
                   ('high', 'high'), ('timer', 'timer'), ('glide', 'glide'), ('totem', 'inventory'),
                   ('kb', 'antiknockback'), ('scaffold', 'interact')):
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

# scaffold rows: swap-use binding and the held-block BY-DESIGN hole named, not hidden
sc = S['nb-scaffold-cheat']
check(sc['setup_rcon'] == ['/clear @p',
                           '/item replace entity @p weapon.mainhand with minecraft:stick',
                           '/item replace entity @p container.1 with minecraft:stone 64'],
      'scaffold cheat setup clears the inventory, then forces a stick in hand + stone in a KNOWN hotbar slot')
check(sc['teardown_rcon'] == ['/clear @p'], 'scaffold cheat tears the inventory down')
check('HYPOTHESIS' in sc['row'] and 'TARGET' in sc['row'], 'scaffold cheat row labels setup/signal claims as hypotheses, fields as TARGET')
check('40ms' in sc['row'] and 'BY-DESIGN quiet' in sc['row'], 'scaffold cheat row binds the swap-use window and names the uncovered held-block variant')
check('ROTATION-SNAP-AT-PLACE-PROPOSAL' in sc['row'], 'scaffold cheat row names the proposal that would close the quiet variant')
sl = S['nb-scaffold-legit']
check('span' in sl['row'] and 'DRAFT' in sl['row'] and 'HYPOTHESIS' in sl['row'], 'scaffold legit row: span DRAFT + hypothesis wording')
check(sl.get('pitch') == 60 and sc.get('pitch') == 60, 'both scaffold rows pin the pitch onto the placement face')
check(sl['setup_rcon'] == ['/clear @p', '/item replace entity @p weapon.mainhand with minecraft:stone 64'],
      'scaffold legit setup holds blocks in hand')
check('effect_check' in sl and 'effect_check' in sc, 'both scaffold rows bind a server-confirmed placement receipt')
check(('hold3', 'Shift_L', 's', '3', 8) in sl['legs'], 'scaffold legit leg holds sneak+back+use (the hold3 verb)')

check(sl.get('selected_slot') == 0 and sc.get('selected_slot') == 0,
      'scaffold rows pin selected_slot 0 so weapon.mainhand and container.1 never collide')

def finish():
    if failed: raise SystemExit('%d failures' % failed)
    print('test-newbounds_controls ok')

# combat family (nb-cbt-, W3/W4 draft rows, v3 after the 0048 retraction: LIVE path, monitor mode).
# Placed BEFORE the negative controls so the single finish() at EOF gates on these too.
cbt = sorted(k for k in S if k.startswith('nb-cbt-'))
check(cbt == ['nb-cbt-aim-cheat', 'nb-cbt-crowd-legit', 'nb-cbt-flick-legit', 'nb-cbt-lag100-legit',
              'nb-cbt-lag300-legit', 'nb-cbt-los-cheat', 'nb-cbt-multi-cheat', 'nb-cbt-reach-cheat'],
      'nb-cbt- family: 4 cheat + 4 legit rows, got ' + str(cbt))
for k in cbt:
    check('DRAFT until' in S[k]['row'], k + ' stays a DRAFT until receipts')
    check('CONDITIONAL' not in S[k]['row'] and 'BLIND' not in S[k]['row'] and 'dead path' not in S[k]['row'],
          k + ': no retracted dead-path wording (0048)')
    for f in ('setup_rcon', 'teardown_rcon'):
        for c in S[k].get(f, []):
            check(c.startswith('/execute in minecraft:overworld run '),
                  k + '.' + f + ': every command runs /execute in minecraft:overworld (world is real, not prose)')
            check('@p' not in c, k + '.' + f + ': no @p (RCON @p can hit the wrong nearest player)')
for k in ('nb-cbt-reach-cheat', 'nb-cbt-los-cheat', 'nb-cbt-aim-cheat', 'nb-cbt-multi-cheat'):
    check(S[k]['expect'] == 'signal' and S[k]['checks'] == ('combat',) and 'module' in S[k], k + ' is a bound combat cheat row')
    check('monitor mode' in S[k]['row'] and 'LIVE' in S[k]['row'], k + ' targets the LIVE path under monitor mode (real mon heat)')
    check('effect_check' in S[k], k + ' binds a server-confirmed receipt')
    check('never EVADE' in (S[k]['row'] + S[k].get('effect_check', '')), k + ': failure stays INCONCLUSIVE, never EVADE')
for k in ('nb-cbt-reach-cheat', 'nb-cbt-los-cheat', 'nb-cbt-aim-cheat', 'nb-cbt-multi-cheat'):
    check('[a15]' in S[k]['effect_check'], k + ' binds the [a15] attempt-trace receipt (vanilla rejection can suppress damage)')
check('PREFLIGHT' in S['nb-cbt-reach-cheat']['effect_check'] and 'SEPARATELY' in S['nb-cbt-reach-cheat']['effect_check'],
      'reach row: preflight trace-coverage proof + attempt receipt separate from damage; absence-unknown = INCONCLUSIVE')
check('silent=2' in S['nb-cbt-multi-cheat']['module'] and 'MISS-DRIVEN' in S['nb-cbt-multi-cheat']['row'],
      'multi row is miss-driven (attack-before-rotate switching), not only the calibrated-quiet module')
check('BY-DESIGN quiet' in S['nb-cbt-multi-cheat']['row'], 'multi row keeps the calibrated-KillAura quiet caveat honest')
for k, tag in (('nb-cbt-multi-cheat', 'nbcbt_multi_<run>'), ('nb-cbt-crowd-legit', 'nbcbt_crowd_<run>'), ('nb-cbt-flick-legit', 'nbcbt_flick_<run>')):
    check(all(('Tags:["%s"]' % tag) in c for c in S[k]['setup_rcon'] if ' run summon ' in c),
          k + ': every summon carries the row-unique + run-unique tag')
    check(S[k]['teardown_rcon'] == ['/execute in minecraft:overworld run kill @e[type=minecraft:zombie,tag=%s]' % tag],
          k + ': teardown kills only the row+run-owned tag (no untagged or cross-run selector)')
check('/execute in minecraft:overworld run clone 157 70 158 157 73 162 157 80 158' in S['nb-cbt-los-cheat']['setup_rcon']
      and '/execute in minecraft:overworld run clone 157 80 158 157 83 162 157 70 158' in S['nb-cbt-los-cheat']['teardown_rcon'],
      'los row clones the wall volume to a backup and RESTORES it (no prior blocks destroyed)')
check(any('forceload add' in c for c in S['nb-cbt-los-cheat']['setup_rcon'])
      and any('forceload remove' in c for c in S['nb-cbt-los-cheat']['teardown_rcon']),
      'los row forceloads the wall + backup volumes and releases them')
check('<bot>' in ' '.join(S['nb-cbt-crowd-legit']['setup_rcon']) and 'minecraft:iron_sword' in ' '.join(S['nb-cbt-crowd-legit']['setup_rcon']),
      'crowd setup hands the named rig bot (<bot>, not @p) an iron sword')
check(S['nb-cbt-crowd-legit']['legs'] == [('click', '1', 1), ('wait', 0.8)] * 8,
      'crowd legs: 8 full-charge swings at 800 ms spacing (cooldown-true)')
check('625 ms' in S['nb-cbt-crowd-legit']['row'] and '800 ms' in S['nb-cbt-crowd-legit']['row'],
      'crowd row shows the cooldown arithmetic (800 > 625)')
check(S['nb-cbt-lag100-legit'].get('ping_ms') == 100 and S['nb-cbt-lag300-legit'].get('ping_ms') == 300,
      'lag rows pin the W3 100/300 ms passes')
check('TARGET' in S['nb-cbt-lag100-legit']['row'] and 'TARGET' in S['nb-cbt-flick-legit']['row'],
      'unconsumed fields (ping_ms, flick macro) are labeled TARGET')
check('no runtime GO' in S['nb-cbt-lag100-legit']['row'] and 'no runtime GO' in S['nb-cbt-lag300-legit']['row']
      and 'no runtime GO' in S['nb-cbt-flick-legit']['row'], 'executor-less TARGET rows are DRAFT, no runtime GO')
check('never emit' in S['nb-cbt-flick-legit']['row'], 'flick row pins low-confidence patterns as evidence-only')
check('dead path' not in S['nb-combat-cheat']['row'] and 'monitor mode' in S['nb-combat-cheat']['row'],
      'nb-combat-cheat positive control targets the live monitor-mode path (0048 retraction)')

# negative controls (skipped inside a mutated run - no recursion)
if not os.environ.get('MESHAC_TEST_MUTATE'):
    import subprocess
    # (a) isolated broken fixture harness MUST exit rc1 (0036)
    _fx = pathlib.Path(__file__).parent / 'test-newbounds-negative-fixture.py'
    _rc = subprocess.run([sys.executable, str(_fx)], capture_output=True).returncode
    check(_rc == 1, 'broken fixture suite must exit rc1 (got %s)' % _rc)
    # (b) mutation of a REAL production pin (GC01 total -> 301) MUST exit rc1 (0039)
    _env = dict(os.environ); _env['MESHAC_TEST_MUTATE'] = 'gc01'
    _p2 = subprocess.run([sys.executable, str(pathlib.Path(__file__))], capture_output=True, text=True, env=_env)
    check(_p2.returncode == 1, 'mutated production pin (GC01 total) must exit rc1 (got %s)' % _p2.returncode)
    check('FAIL GC01 legs total 300s (the pre-committed bound, not 16s)' in _p2.stdout,
          'mutated run emits the exact expected FAIL line (a PASS line sharing text does not count)')
    check(_p2.stderr.strip() == '1 failures', 'mutated run fails with exactly one failure, got stderr: ' + _p2.stderr.strip()[:200])
    check('Traceback' not in _p2.stderr, 'mutated run fails by assertion, not a crash: ' + _p2.stderr[:200])
finish()
