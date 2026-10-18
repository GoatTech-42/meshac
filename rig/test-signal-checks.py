#!/usr/bin/env python3
"""Synthetic pins for signal_checks.py (TEXT2CHECK) + drift scan against CheckNames.java. No rig, no server."""
import pathlib, re, sys
sys.path.insert(0, str(pathlib.Path(__file__).parent))
import signal_checks

failed = 0
def check(ok, what):
    global failed
    if not ok: print('FAIL ' + what); failed += 1

# every known check binds from a realistic SIGNAL line (Verdict.java format)
L = '[meshac] SIGNAL P %s %s heat=1 -> log'
for c in signal_checks.CHECKS:
    check(signal_checks.bind(L % (c, signal_checks.TEXT2CHECK[c])) == c, 'bind ' + c)
# timestamps / logger prefixes before SIGNAL are fine
check(signal_checks.bind('[10:00:00] [Server thread/WARN] ' + L % ('nofall', 'ground spoof')) == 'nofall', 'prefixed line')
# older capture shape from ground-replay selftest
check(signal_checks.bind('[10:00:00] x SIGNAL .a nofall ground spoof heat=1') == 'nofall', 'old capture shape')
# the pre-extension blind spots: these lines used to grade as silence
for c, d in (('fly', 'hover dy 0.01'), ('speed', 'speed 12.3'), ('combat', 'reach 4.2'), ('timer', 'timer x1.2'),
             ('glide', 'no air drag in flight'), ('antiknockback', 'no push after three hits')):
    check(signal_checks.bind(L % (c, d)) == c, 'formerly unbound ' + c)
# non-SIGNAL lines and unknown check names bind to nothing
check(signal_checks.bind('[meshac] [trace] groundctx P flag=true') is None, 'trace line is no signal')
check(signal_checks.bind('[meshac] SIGNAL P wxyz made-up-check heat=1 -> log') is None, 'unknown check rejected')
check(signal_checks.bind('') is None, 'empty line')
# TEXT2CHECK covers exactly the known checks
check(set(signal_checks.TEXT2CHECK) == set(signal_checks.CHECKS), 'TEXT2CHECK covers every check once')
# drift scan: CHECKS must equal CheckNames.ALL in the jar source (same idea as sim/CheckNamesTest)
src = (pathlib.Path(__file__).parent.parent / 'src/main/java/dev/meshac/CheckNames.java').read_text()
names = set(re.findall(r'"([a-z0-9-]+)"', src.split('Set.of(')[1]))
check(names == set(signal_checks.CHECKS), 'CheckNames.ALL drift: jar=%s grader=%s' % (sorted(names - signal_checks.CHECKS), sorted(signal_checks.CHECKS - names)))

# probe mirror (reviewer verify item, relayed via parent 10-17 7:42pm): the P3-strong shadow watcher's
# support probe must be textually the same probe as the real nfAt watcher's (same box, same 0.1 slab)
mv = (pathlib.Path(__file__).parent.parent / 'src/main/java/dev/meshac/Movement.java').read_text()
probes = re.findall(r'!pl\.level\(\)\.noCollision\(pl, new AABB\((\w+)\.minX, \1\.minY - 0\.1, \1\.minZ, \1\.maxX, \1\.minY, \1\.maxZ\)\)', mv)
check('tb' in probes, 'real nfAt watcher probe present (tb box, minY-0.1 slab)')
check('sb' in probes, 'P3-strong shadow watcher probe mirrors it (sb box, same expression)')

if failed: raise SystemExit('%d failures' % failed)
print('test-signal-checks ok')
