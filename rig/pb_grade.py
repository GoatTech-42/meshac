#!/usr/bin/env python3
"""Grader for PB/GS/BL capture results (jar 3fffe5a spec). Reviewer lesson set:
provenance on every PASS, a positive witness in every session, disarm/OFF state verified.
Zero signals without a witness is INVALID, never PASS. Usage: pb_grade.py RESULT.json [SESSION_DIR]
Signal lines bind to checks via signal_checks (TEXT2CHECK): any known check's SIGNAL counts; a row
carrying 'checks' (tuple of check names from its scenario) must bind one of those specifically.
Grace-boundary persistence rows (patch 6 Stage-1 battery, BATTERY-SHADOW-DIVERGENCE-PLAN): the row's
'shadowSignals' list carries the captured [trace] decay-shadow lines (the producer keeps 'signals' to
SIGNAL lines only). A row with 'expectShadow': True is INVALID without a decay-shadow line (a blind
shadow row is INCONCLUSIVE, never divergence-PASS); a row with 'shadowZero': True FAILs on any
decay-shadow line (the pre-committed legit-row kill criterion). Shadow lines never bind as signals."""
import json, sys

import signal_checks

REQ_PROV = ['scenario', 'row', 'player', 'jarSHA', 'expect', 'session', 'events', 'tickHealth', 'modLiveness', 'setupReceipt']

def _moved(events):
    for e in events:
        d = e.get('posDelta')
        if d and len(d) == 3 and (abs(d[0]) + abs(d[1]) + abs(d[2])) > 0.5: return True
    return False

def grade(result, session):
    """session: list of result dicts from the same capture session (may be just [result])."""
    reasons = []
    for k in REQ_PROV:
        if k not in result or result[k] in (None, '', []): reasons.append('missing provenance: ' + k)
    if reasons: return {'verdict': 'INVALID', 'reasons': reasons}
    sig = result.get('signals') or []
    sh = signal_checks.shadow_lines(result.get('shadowSignals') or [])
    if result.get('shadowZero') and sh:
        return {'verdict': 'FAIL', 'reasons': ['decay-shadow flagged on a legit row: pre-committed kill criterion (GRACE-DECAY-PROMOTION-DRAFT): ' + sh[0][:120]]}
    if result.get('needs') == 'vanilla' and not (result.get('offState') or {}).get('moduleOffPixelConfirmed'):
        reasons.append('OFF state not verified: no module-off pixel confirmation')
    if result.get('needsDisarm') and not result.get('disarmVerified'):
        reasons.append('disarm not verified: no sneak-pose sample from the disarm leg')
    if result.get('expectMove', True) and not _moved(result['events']):
        reasons.append('no position delta in any leg: input may not have executed')
    if result['expect'] == 'zero':
        if sig:
            return {'verdict': 'FAIL', 'reasons': ['signals on a zero row: ' + '; '.join(sig[:3])]}
        if not any(r.get('expect') == 'signal' and signal_checks.bound(r.get('signals') or []) for r in session):
            reasons.append('no positive witness: no signaled positive-control row in this session')
    else:
        bound = signal_checks.bound(sig)
        want = set(result.get('checks') or ())
        if want and not (bound & want):
            reasons.append('expected SIGNAL for ' + '/'.join(sorted(want)) + ' absent (bound: ' + ('/'.join(sorted(bound)) or 'none') + ') - zero or off-target signals is INVALID, never PASS')
        elif not want and not bound:
            reasons.append('expected SIGNAL absent on a positive-control row (zero signals is INVALID, never PASS)')
    if result.get('expectShadow') and not sh:
        reasons.append('expected decay-shadow divergence absent: a blind shadow row is INCONCLUSIVE, never divergence-PASS')
    return {'verdict': 'INVALID' if reasons else 'PASS', 'reasons': reasons}

def main():
    result = json.load(open(sys.argv[1]))
    session = [result]
    if len(sys.argv) > 2:
        import pathlib
        session = [json.load(open(p)) for p in sorted(pathlib.Path(sys.argv[2]).glob('*/result.json'))] or [result]
    print(json.dumps(grade(result, session), indent=2))
if __name__ == '__main__': main()
