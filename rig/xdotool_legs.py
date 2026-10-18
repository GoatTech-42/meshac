#!/usr/bin/env python3
"""Leg-verb driver for the rig capture rows: turns pb-controls/newbounds legs into xdotool calls.
Box drops this into the rep driver; pb-controls.py's inline leg loop can call run_leg() instead.
Extends the original keyboard-only verbs with ('click',button,n) and ('holdmouse',button,seconds)
from RIG-CAPTURE-REQUEST-NEWBOUNDS.md. --selftest dry-runs (prints, does not execute) without X.

Human pacing: clicks default to ~4/s (Interact.java allows 16/s; fast bridging is ~8/s) - the
vanilla rows must read as a fast-but-human hand, and the cheat rows let the module do the work."""
import subprocess, sys, time

class ReleaseError(RuntimeError):
    """An input release failed after every release was attempted. Mark the row INCONCLUSIVE;
    the user may need an input reset."""

CLICK_DELAY_S = 0.25  # ~4 clicks a second, human pace (see module docstring)
BUTTONS = {'1', '2', '3'}  # left, middle, right

def _x(args, dry):
    if dry: print('xdotool ' + ' '.join(args)); return
    subprocess.check_call(['xdotool'] + args)

def run_leg(leg, module_key, dry=False):
    """One leg. module_key: the rep-confirmed keybind of the row's named module."""
    v = leg[0]
    if v == 'wait': time.sleep(0 if dry else leg[1]); return
    if v == 'tap': _x(['key', leg[1]], dry); return
    if v == 'hold':
        _x(['keydown', leg[1]], dry); time.sleep(0 if dry else leg[2]); _x(['keyup', leg[1]], dry); return
    if v == 'hold2':
        _x(['keydown', leg[1]], dry); _x(['keydown', leg[2]], dry)
        time.sleep(0 if dry else leg[3])
        _x(['keyup', leg[1]], dry); _x(['keyup', leg[2]], dry); return
    if v == 'hold3':
        btn = leg[3]
        assert btn in BUTTONS, 'unknown mouse button ' + btn
        # releases predeclared BEFORE any acquire: keyup/mouseup are idempotent. The guarantee
        # is that ALL cleanup is attempted, even on an ambiguous mid-send error - a release can
        # itself fail, and that failure is surfaced as ReleaseError (row INCONCLUSIVE), never
        # swallowed
        releases = [['keyup', leg[1]], ['keyup', leg[2]], ['mouseup', btn]]
        try:
            _x(['keydown', leg[1]], dry)
            _x(['keydown', leg[2]], dry)
            _x(['mousedown', btn], dry)
            time.sleep(0 if dry else leg[4])
        finally:
            missed = []
            for rel in releases:
                try: _x(rel, dry)
                except Exception as e: missed.append(' '.join(rel) + ' (%s)' % e)
            if missed:
                raise ReleaseError('input release failed after all were attempted: ' + '; '.join(missed)
                                   + ' - mark the row INCONCLUSIVE, the user may need an input reset')
        return
    if v == 'module': _x(['key', module_key], dry); return
    if v == 'click':
        btn = leg[1]
        assert btn in BUTTONS, 'unknown mouse button ' + btn
        for i in range(leg[2]):
            _x(['click', btn], dry)
            if i + 1 < leg[2]: time.sleep(0 if dry else CLICK_DELAY_S)
        return
    if v == 'holdmouse':
        btn = leg[1]
        assert btn in BUTTONS, 'unknown mouse button ' + btn
        _x(['mousedown', btn], dry); time.sleep(0 if dry else leg[2]); _x(['mouseup', btn], dry); return
    raise ValueError('unknown leg verb ' + v)

def release_all(dry=False):
    """Safety net from pb-controls: never leave a key or button held."""
    for k in ('space', 'w', 'Shift_L'): _x(['keyup', k], dry)
    for b in BUTTONS: _x(['mouseup', b], dry)

def selftest():
    import signal_checks, newbounds_controls
    # every verb in the vocabulary runs (dry) and releases cleanly; every spec leg validates
    for leg in [('wait', 0.01), ('tap', 'space'), ('hold', 'w', 0.01), ('hold2', 'w', 'Shift_L', 0.01),
                ('module', None), ('click', '1', 2), ('holdmouse', '3', 0.01), ('hold3', 'w', 'Shift_L', '3', 0.01)]:
        run_leg(leg, 'r', dry=True)
    # hold3: a failed release is surfaced as ReleaseError and EVERY release is still attempted
    calls = []
    orig_x = _x
    def boom(args, dry):
        calls.append(args)
        if args == ['keyup', 'w']: raise RuntimeError('X gone mid-release')
    globals()['_x'] = boom
    try:
        try:
            run_leg(('hold3', 'w', 'Shift_L', '3', 0.01), 'r')
            raise AssertionError('hold3 must raise ReleaseError when a release fails')
        except ReleaseError as e:
            assert 'INCONCLUSIVE' in str(e) and 'input reset' in str(e), 'ReleaseError names the row consequence'
    finally:
        globals()['_x'] = orig_x
    assert ['keyup', 'w'] in calls and ['keyup', 'Shift_L'] in calls and ['mouseup', '3'] in calls, 'every release attempted even after one fails'
    for sc in list(newbounds_controls.S.values()):
        for leg in sc['legs']:
            assert leg[0] in newbounds_controls.LEG_VERBS, 'spec leg outside the vocabulary: ' + str(leg)
    release_all(dry=True)
    # a vanilla click row paces ~4/s, far under the 16/s interact limit it must not trip
    assert CLICK_DELAY_S >= 0.2, 'click pacing risks tripping the 16/s interact counter on vanilla rows'
    print('xdotool-legs selftest ok')

if __name__ == '__main__':
    if sys.argv[1:] == ['--selftest']: selftest()
    else: print(__doc__); sys.exit(2)
