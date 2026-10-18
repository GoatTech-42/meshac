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
# 13. formerly unbound checks now count as positive-control signals (TEXT2CHECK covers all CheckNames.ALL)
for w in ('[meshac] SIGNAL P fly hover dy 0.010', '[meshac] SIGNAL P glide no air drag in flight',
          '[meshac] SIGNAL P combat reach 4.2', '[meshac] SIGNAL P timer x1.2', '[meshac] SIGNAL P antiknockback no push after three hits'):
    g = base(scenario='x', expect='signal', needs='cheat', offState=None, signals=[w])
    check(grade(g, [g])['verdict'] == 'PASS', 'formerly unbound ' + w)
# 14. 'checks' scoping: an off-target signal does not satisfy a check-scoped row
g = base(scenario='pb-n', expect='signal', needs='cheat', offState=None, checks=('step',), signals=['[meshac] SIGNAL P air jump dy 0.5'])
r = grade(g, [g]); check(r['verdict'] == 'INVALID' and any('off-target' in x for x in r['reasons']), 'off-target signal rejected: ' + str(r))
g = base(scenario='pb-e', expect='signal', needs='cheat', offState=None, checks=('air', 'step'), signals=['[meshac] SIGNAL P step dy 0.610 onGround'])
check(grade(g, [g])['verdict'] == 'PASS', 'on-target alternative accepted: ' + str(g))
# 15. a witness can now come from any known check, not just the legacy five
r = grade(base(), [base(), base(scenario='w', expect='signal', needs='cheat', offState=None, signals=['[meshac] SIGNAL P fly hover dy 0.010'])])
check(r['verdict'] == 'PASS', 'witness from a newly-bound check: ' + str(r))
# 16b. T1 GS01-GS03 verdict paths (EXPECT-REBIND-PATCH1 + control matrix B2 strings; INCONCLUSIVE never CLEAN on zero signals)
gs01 = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',),
            signals=['[meshac] SIGNAL P nofall ground spoof heat=3 -> log'])
check(grade(gs01, [gs01])['verdict'] == 'PASS', 'GS01 interleaved spoof binds nofall: ' + str(grade(gs01, [gs01])))
gs01b = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',),
             signals=['[meshac] SIGNAL P nofall ground flag in mid-air heat=3 -> log'])
check(grade(gs01b, [gs01b])['verdict'] == 'PASS', 'GS01 alt nofall string binds: ' + str(grade(gs01b, [gs01b])))
gs01silent = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',))
r = grade(gs01silent, [gs01silent]); check(r['verdict'] == 'INVALID' and any('never PASS' in x for x in r['reasons']), 'GS01 blind control = INCONCLUSIVE never CLEAN: ' + str(r))
gs02 = base(scenario='gs02', expect='signal', needs='cheat', offState=None, checks=('high',),
            signals=['[meshac] SIGNAL P high jump rose 2.30 heat=1 -> log'])
check(grade(gs02, [gs02])['verdict'] == 'PASS', 'GS02 split-rise binds high: ' + str(grade(gs02, [gs02])))
gs03 = base(scenario='gs03', expect='signal', needs='cheat', offState=None, checks=('jump', 'step'),
            signals=['[meshac] SIGNAL P jump dy 0.750 heat=2 -> log'])
check(grade(gs03, [gs03])['verdict'] == 'PASS', 'GS03 above-band kick binds jump: ' + str(grade(gs03, [gs03])))
gs03off = base(scenario='gs03', expect='signal', needs='cheat', offState=None, checks=('jump', 'step'),
               signals=['[meshac] SIGNAL P nofall ground spoof heat=1 -> log'])
r = grade(gs03off, [gs03off]); check(r['verdict'] == 'INVALID' and any('off-target' in x for x in r['reasons']), 'GS03 nofall-only is off-target: ' + str(r))
# 16. a line that is not a SIGNAL for a known check is still silence
g = base(scenario='x', expect='signal', needs='cheat', offState=None, signals=['[meshac] SIGNAL P wxyz made up'])
check(grade(g, [g])['verdict'] == 'INVALID', 'unknown check is silence: ' + str(g))

# 17. blink lane vs the control matrix (intentional blink = control; legit bunching BF08/BF14 = FP-risk rows)
# intentional blink (packet hold/release) binds 'blink' with the exact matrix string
bl = base(scenario='bl-burst', expect='signal', needs='cheat', offState=None, checks=('blink',),
          signals=['[meshac] SIGNAL P blink held its movement packets back and released them in one burst heat=2 -> log'])
check(grade(bl, [bl])['verdict'] == 'PASS', 'intentional blink binds (control works): ' + str(grade(bl, [bl])))
# blind blink control = INCONCLUSIVE, never CLEAN (matrix reading rule 1)
blsilent = base(scenario='bl-burst', expect='signal', needs='cheat', offState=None, checks=('blink',))
check(grade(blsilent, [blsilent])['verdict'] == 'INVALID', 'blind blink control: ' + str(grade(blsilent, [blsilent])))
# BF08 legit bunching (hold 800ms, release): a blink FLAG here is an FP finding - zero row with a signal FAILs (finding, never CLEAN)
bf08 = base(scenario='bf08', expect='zero', signals=['[meshac] SIGNAL P blink held its movement packets back and released them in one burst heat=1 -> log'])
r = grade(bf08, [bl]); check(r['verdict'] == 'FAIL' and any('signals on a zero row' in x for x in r['reasons']), 'BF08 bunching flag = FP finding: ' + str(r))
# BF08 clean (no blink line on legit bunching) grades PASS with a witness - the check stayed quiet, as hoped
bf08clean = base(scenario='bf08', expect='zero')
check(grade(bf08clean, [bf08clean, bl])['verdict'] == 'PASS', 'BF08 clean bunching: ' + str(grade(bf08clean, [bf08clean, bl])))
# a nofall line on a blink-scoped row is off-target (check-scoped controls cannot borrow each other)
bloff = base(scenario='bl-burst', expect='signal', needs='cheat', offState=None, checks=('blink',),
             signals=['[meshac] SIGNAL P nofall ground spoof heat=1 -> log'])
check(grade(bloff, [bloff])['verdict'] == 'INVALID', 'blink row with nofall signal is off-target: ' + str(grade(bloff, [bloff])))

# 18. grace-boundary persistence (patch 6 Stage-1 battery rows, BATTERY-SHADOW-DIVERGENCE-PLAN).
# Producer contract (pb-controls): 'signals' carries only "SIGNAL <player>" lines; the row's
# 'shadowSignals' carries the captured [trace] decay-shadow telemetry. Shadow telemetry is log-only
# and must NEVER bind as a signal; gndclaim/gndclear/gndreset go to GroundReplay, never the grader.
SH = '[trace] nofall decay-shadow P count=3 liveCount=0 liveFlagged=false (grace-boundary persistence draft, log-only)'
CAP = ['[trace] gndclaim P air=true count=3', '[trace] gndclear P decayCount=2', '[trace] gndreset P cause=post-flag count=0 decayCount=3']
nf_ctrl = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',),
               signals=['[meshac] SIGNAL P nofall ground spoof heat=3 -> log'])
# GS-D grace-farm divergence row: live window silent by design today, shadow must FLAG -> PASS only with shadow telemetry + session witness
gsd = base(scenario='gs-d', expect='zero', expectShadow=True, shadowSignals=[SH])
r = grade(gsd, [gsd, nf_ctrl]); check(r['verdict'] == 'PASS', 'GS-D divergence row: ' + str(r))
# GS-D blind (no shadow line) is INCONCLUSIVE, never divergence-PASS
gsd_blind = base(scenario='gs-d', expect='zero', expectShadow=True)
r = grade(gsd_blind, [gsd_blind, nf_ctrl]); check(r['verdict'] == 'INVALID' and any('divergence' in x for x in r['reasons']), 'GS-D blind shadow: ' + str(r))
# GS-D with a LIVE nofall SIGNAL today = signals on a zero row -> FAIL (a live flag would mean the hole closed unreviewed)
gsd_live = base(scenario='gs-d', expect='zero', expectShadow=True, signals=['[meshac] SIGNAL P nofall ground spoof heat=3 -> log'], shadowSignals=[SH])
r = grade(gsd_live, [gsd_live, nf_ctrl]); check(r['verdict'] == 'FAIL', 'GS-D unexpected live flag: ' + str(r))
# GS-E water-dip divergence row, same shape
gse = base(scenario='gs-e', expect='zero', expectShadow=True, shadowSignals=[SH.replace('count=3', 'count=4')])
r = grade(gse, [gse, nf_ctrl]); check(r['verdict'] == 'PASS', 'GS-E divergence row: ' + str(r))
# Legit medium dip (shadowZero kill row): clean -> PASS; ANY decay-shadow line -> FAIL (pre-committed kill criterion)
dip = base(scenario='legit-medium-dip', expect='zero', shadowZero=True)
r = grade(dip, [dip, nf_ctrl]); check(r['verdict'] == 'PASS', 'legit dip clean: ' + str(r))
dip_kill = base(scenario='legit-medium-dip', expect='zero', shadowZero=True, shadowSignals=[SH])
r = grade(dip_kill, [dip_kill, nf_ctrl]); check(r['verdict'] == 'FAIL' and any('kill criterion' in x for x in r['reasons']), 'legit dip shadow flag = kill: ' + str(r))
# Legit knockback landing + medium exit (elytra): the other two kill rows, both shapes
for sc in ('legit-kb-landing', 'legit-medium-exit-elytra'):
    row = base(scenario=sc, expect='zero', shadowZero=True)
    check(grade(row, [row, nf_ctrl])['verdict'] == 'PASS', sc + ' clean: ' + str(grade(row, [row, nf_ctrl])))
    kill = base(scenario=sc, expect='zero', shadowZero=True, shadowSignals=[SH.replace('liveFlagged=false', 'liveFlagged=true')])
    check(grade(kill, [kill, nf_ctrl])['verdict'] == 'FAIL', sc + ' shadow flag = kill')
# decay-shadow telemetry NEVER binds as a nofall signal, even on a check-scoped positive row
pos_shadow_only = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',), shadowSignals=[SH])
r = grade(pos_shadow_only, [pos_shadow_only]); check(r['verdict'] == 'INVALID', 'shadow line is not a catch: ' + str(r))
pos_shadow2 = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',), shadowSignals=[SH.replace('liveFlagged=false', 'liveFlagged=true')])
r = grade(pos_shadow2, [pos_shadow2]); check(r['verdict'] == 'INVALID', 'shadow line (liveFlagged=true) is not a catch: ' + str(r))
# capture lines never bind either: positive nofall row whose signals list carries only gndclaim/gndclear/gndreset is INVALID
pos_cap_only = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',), signals=CAP)
r = grade(pos_cap_only, [pos_cap_only]); check(r['verdict'] == 'INVALID', 'capture lines are not a catch: ' + str(r))
# a shadow-divergence row is NOT a session witness (witness must be a bound expect=signal row)
r = grade(base(), [base(), gsd]); check(r['verdict'] == 'INVALID' and any('witness' in x for x in r['reasons']), 'divergence row is no witness: ' + str(r))
r = grade(base(), [base(), gsd, nf_ctrl]); check(r['verdict'] == 'PASS', 'real nofall control witnesses the session: ' + str(r))
# one-shape contract (0061): SIGNAL-SUPPRESSED receipts never bind as signals
supp = base(scenario='gs01', expect='signal', needs='cheat', offState=None, checks=('nofall',),
            signals=['[meshac] SIGNAL-SUPPRESSED P uuid nofall count=7 (window closed)'])
r = grade(supp, [supp]); check(r['verdict'] == 'INVALID', 'SIGNAL-SUPPRESSED receipt is not a signal: ' + str(r))

if failed: raise SystemExit('%d failures' % failed)
print('test-pb-grade ok')
