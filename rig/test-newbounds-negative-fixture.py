#!/usr/bin/env python3
"""DELIBERATELY BROKEN fixture: proves the check/finish failure path exits rc1.
Run as a subprocess by test-newbounds-controls.py only; it must NEVER exit 0."""
import pathlib, sys
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import newbounds_controls

failed = 0
def check(ok, what):
    global failed
    if not ok: print('FAIL ' + what); failed += 1

def finish():
    if failed: raise SystemExit('%d failures' % failed)
    print('fixture ok')

check(newbounds_controls.validate() is False, 'deliberately broken: validate() cannot be False')
finish()
