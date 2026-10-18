#!/usr/bin/env python3
"""Grader for PB/GS/BL capture results (jar 3fffe5a spec). Reviewer lesson set:
provenance on every PASS, a positive witness in every session, disarm/OFF state verified.
Zero signals without a witness is INVALID, never PASS. Usage: pb_grade.py RESULT.json [SESSION_DIR]"""
import json, sys

REQ_PROV = ['scenario', 'row', 'player', 'jarSHA', 'expect', 'session', 'events', 'tickHealth', 'modLiveness', 'setupReceipt']
SIGNAL_WORDS = ('air jump', 'step', 'nofall', 'ground spoof', 'blink')

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
    if result.get('needs') == 'vanilla' and not (result.get('offState') or {}).get('moduleOffPixelConfirmed'):
        reasons.append('OFF state not verified: no module-off pixel confirmation')
    if result.get('needsDisarm') and not result.get('disarmVerified'):
        reasons.append('disarm not verified: no sneak-pose sample from the disarm leg')
    if result.get('expectMove', True) and not _moved(result['events']):
        reasons.append('no position delta in any leg: input may not have executed')
    if result['expect'] == 'zero':
        if sig:
            return {'verdict': 'FAIL', 'reasons': ['signals on a zero row: ' + '; '.join(sig[:3])]}
        if not any(r.get('expect') == 'signal' and any(w in l for l in (r.get('signals') or []) for w in SIGNAL_WORDS) for r in session):
            reasons.append('no positive witness: no signaled positive-control row in this session')
    else:
        if not any(w in l for l in sig for w in SIGNAL_WORDS):
            reasons.append('expected SIGNAL absent on a positive-control row (zero signals is INVALID, never PASS)')
    return {'verdict': 'INVALID' if reasons else 'PASS', 'reasons': reasons}

def main():
    result = json.load(open(sys.argv[1]))
    session = [result]
    if len(sys.argv) > 2:
        import pathlib
        session = [json.load(open(p)) for p in sorted(pathlib.Path(sys.argv[2]).glob('*/result.json'))] or [result]
    print(json.dumps(grade(result, session), indent=2))
if __name__ == '__main__': main()
