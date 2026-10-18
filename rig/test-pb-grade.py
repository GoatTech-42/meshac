#!/usr/bin/env python3
"""Synthetic grader cases for pb_grade.py: the reviewer lesson set as fixtures. No rig, no server."""
import sys, pathlib
sys.path.insert(0, str(pathlib.Path(__file__).parent))
from pb_grade import grade

failed = 0
def check(ok, what):
    global failed
    if not ok: print('FAIL ' + what); failed += 1

def base(**kw):
    r = {'scenario': 'pb-a', 'row': 'x', 'player': 'P', 'jarSHA': 's', 'expect': 'zero', 'session': 'sess1',
         'events': [{'leg': 'wait', 'posDelta': [0, -4.2, 0]}], 'signals': [], 'tickHealth': '20.0',
         'modLiveness': 'cases ok', 'setupReceipt': 'coords', 'needs': 'vanilla',
         'offState': {'moduleOffPixelConfirmed': True}}
    r.update(kw); return r

cheat_pass = base(scenario='pb-e', expect='signal', needs='cheat', offState=None,
    signals=['[meshac] SIGNAL P air jump dy 0.500'])
cheat_silent = base(scenario='pb-e', expect='signal', needs='cheat', offState=None)

# 1. clean zero row with a session witness passes
r = grade(base(), [base(), cheat_pass]); check(r['verdict'] == 'PASS', 'zero row + witness: ' + str(r))
# 2. zero row without any witness is INVALID, not PASS
r = grade(base(), [base()]); check(r['verdict'] == 'INVALID' and any('witness' in x for x in r['reasons']), 'no witness: ' + str(r))
# 3. signals on a zero row FAIL even with witness
r = grade(base(signals=['[meshac] SIGNAL P step rose 0.70']), [cheat_pass]); check(r['verdict'] == 'FAIL', 'zero row with signal: ' + str(r))
# 4. movement row with no position delta is INVALID (legs may not have executed)
r = grade(base(events=[{'leg': 'wait', 'posDelta': [0, 0, 0]}]), [cheat_pass]); check(r['verdict'] == 'INVALID' and any('position delta' in x for x in r['reasons']), 'no pos delta: ' + str(r))
# 5. rest row (expectMove False) needs no delta
r = grade(base(expectMove=False, events=[{'leg': 'wait', 'posDelta': [0, 0, 0]}]), [cheat_pass]); check(r['verdict'] == 'PASS', 'rest row: ' + str(r))
# 6. missing provenance field is INVALID
b = base(); del b['tickHealth']
r = grade(b, [cheat_pass]); check(r['verdict'] == 'INVALID' and any('tickHealth' in x for x in r['reasons']), 'provenance: ' + str(r))
# 7. positive-control row with its signal passes
r = grade(cheat_pass, [cheat_pass]); check(r['verdict'] == 'PASS', 'cheat pass: ' + str(r))
# 8. positive-control row silent is INVALID, never PASS
r = grade(cheat_silent, [cheat_silent]); check(r['verdict'] == 'INVALID' and any('never PASS' in x for x in r['reasons']), 'cheat silent: ' + str(r))
# 9. vanilla row without module-off confirmation is INVALID
r = grade(base(offState={'moduleOffPixelConfirmed': False}), [cheat_pass]); check(r['verdict'] == 'INVALID' and any('OFF state' in x for x in r['reasons']), 'off state: ' + str(r))
# 10. disarm row unverified vs verified
d = base(scenario='pb-l', expect='signal', needs='cheat', offState=None, needsDisarm=True, signals=['[meshac] SIGNAL P air jump dy 0.5'])
r = grade(d, [d]); check(r['verdict'] == 'INVALID' and any('disarm' in x for x in r['reasons']), 'disarm unverified: ' + str(r))
d2 = dict(d); d2['disarmVerified'] = True
r = grade(d2, [d2]); check(r['verdict'] == 'PASS', 'disarm verified: ' + str(r))
# 11. ground-spoof and blink keywords count as positive-control signals
for w in ('[meshac] SIGNAL P nofall ground spoof', '[meshac] SIGNAL P blink held its movement packets back'):
    g = base(scenario='gs', expect='signal', needs='cheat', offState=None, signals=[w])
    check(grade(g, [g])['verdict'] == 'PASS', 'keyword ' + w)
# 12. witness from a FAIL-ing zero row does not count (no signals, so not a witness)
r = grade(base(), [base(signals=['[meshac] SIGNAL P step rose 0.70'])]); check(r['verdict'] == 'INVALID', 'failed zero row is no witness: ' + str(r))

if failed: raise SystemExit('%d failures' % failed)
print('test-pb-grade ok')
