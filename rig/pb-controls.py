#!/usr/bin/env python3
"""PB powder/bounce capture cells. Draft runner for the meshac rig rep, jar spec 3fffe5a (production-identical 9974eba).
Input-only legs against one already-prepared client; no second client, no client start/stop, no world/config writes.
Rows and pass criteria per rig/RIG-CAPTURE-REQUEST-POWDER-BOUNCE.md; falsification list per the delivered jar spec."""
import argparse, datetime, hashlib, json, os, subprocess, sys, time
from pathlib import Path

import signal_checks

# World geometry per the capture request; the rep passes --setup-receipt confirming the actual built coordinates.
G = {
 'powder_top': '8.5 70 8.5',      # drop point above the 3x3 powder pit, 6 deep on solid floor
 'powder_rest': '8.5 65 8.5',     # supported spot inside powder, body overlapping
 'powder_low': '8.5 64.5 8.5',    # inside the column near the floor
 'powder_exit': '12.5 68.5 8.5',  # inside the exit column, snow-layer ledge one block east at its top
 'ledge_face': 90,                # yaw facing the ledge
 'slime_top': '24.5 74 24.5',     # 10-block drop onto the slime pad centre
 'slime_edge_top': '26.0 74 24.5', # 10-block drop straddling the slime pad edge (slime + ordinary below)
 'bed_on_slime_top': '32.5 74 32.5', # 10-block drop onto a bed placed on slime
 'rim_spot': '8.5 70.5 11.5',     # standing on the powder column rim (adjacent to, not inside, powder)
 'rim_cheat': '8.5 64 12.5',      # beside the powder column, body NOT intersecting powder
 'strip_start': '40.5 70 40.5',   # one end of the partial-block strip (slab/stair/snow/carpet line)
 'strip_edge': '48.5 70 40.5',    # strip edge drop-off for the ground-spoof probe
}
# legs: ('wait',seconds) ('hold',key,seconds) ('tap',key) ('module',None) - module = rep-confirmed cheat keybind press
S = {
 'pb-a': {'tp': G['powder_top'], 'needs': 'vanilla', 'expect': 'zero',
   'legs': [('wait', 4)], 'row': 'powder descend: stuck -0.1176/tick chain, no fly/step SIGNAL'},
 'pb-b': {'tp': G['powder_rest'], 'needs': 'vanilla', 'expect': 'zero',
   'legs': [('wait', 10)], 'row': 'resting supported inside powder: dy ~0, no fly/step SIGNAL'},
 'pb-c': {'tp': G['powder_low'], 'needs': 'vanilla', 'expect': 'zero',
   'legs': [('hold', 'space', 5), ('wait', 2)], 'row': 'powder climb 0.2/tick: no fly/high-jump/microhop/step SIGNAL'},
 'pb-d': {'tp': G['powder_exit'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': G['ledge_face'],
   'legs': [('hold', 'space', 2), ('hold2', 'space', 'w', 3), ('wait', 2)], 'row': 'climb exit onto 0.125 snow layer: no step fold SIGNAL'},
 'pb-e': {'tp': G['powder_low'], 'needs': 'cheat', 'checks': ('air', 'step'), 'expect': 'signal',
   'legs': [('module', None), ('hold', 'space', 5), ('module', None)], 'row': 'POSITIVE CONTROL: air-jump kicks inside powder must SIGNAL (air jump and/or step)'},
 'pb-l': {'tp': G['slime_top'], 'needs': 'cheat', 'checks': ('air',), 'expect': 'signal',
   'legs': [('hold', 'Shift_L', 3), ('wait', 1), ('module', None), ('hold', 'space', 5), ('module', None)],
   'row': 'POSITIVE CONTROL: kicks right after a sneak-disarm on slime must SIGNAL air jump',
   'falsifies': 'the no-hole claim for the disarm paths: silence here means the tracker still covers after vanilla stopped bouncing'},
 'pb-m': {'tp': G['rim_spot'], 'needs': 'vanilla', 'expect': 'zero',
   'legs': [('wait', 2)] + [('tap', 'space'), ('wait', 1.2)] * 5,
   'row': 'powder-snow rim: standing/jumping on the rim (near, not inside) - zero SIGNAL',
   'falsifies': 'the rim half of the body-intersection gate: legit small movement NEXT to powder must not flag'},
 'pb-n': {'tp': G['rim_cheat'], 'needs': 'cheat', 'checks': ('step',), 'expect': 'signal', 'yaw': 0,
   'legs': [('module', None), ('hold', 'w', 5), ('module', None)],
   'row': 'NEGATIVE (window-taint, near-not-in): step-module climb BESIDE the powder column, body never inside, must SIGNAL step - the 0.2 climb band must not be inherited from mere proximity',
   'falsifies': 'the body-intersection gate on powderClimb: silence here means near-powder hacks inherit the climb exemption'},
 'pb-o': {'tp': G['slime_edge_top'], 'needs': 'vanilla', 'expect': 'zero',
   'legs': [('wait', 8)],
   'row': 'slime edge: 10-block drop straddling the pad edge, whatever vanilla does (partial bounce or none) - zero SIGNAL',
   'falsifies': 'the block-below lookup alignment with vanilla landed-block restitution: a SIGNAL means the arm read the wrong block for an edge landing'},
 'pb-p': {'tp': G['bed_on_slime_top'], 'needs': 'vanilla', 'expect': 'zero',
   'legs': [('wait', 8)],
   'row': 'bed-on-slime: 10-block drop onto a bed on slime - bed restitution (0.66) governs, zero SIGNAL',
   'falsifies': 'the landed-block restitution read: if the observed bounce is slime-scale (~1.2+ maxDy) with zero signals, the tracker over-covered by reading slime under the bed (hole); if it flags, it read no bounce (FP)'},
 'pb-q': {'tp': G['powder_low'], 'needs': 'cheat', 'checks': ('step',), 'expect': 'signal',
   'legs': [('module', None), ('hold2', 'space', 'w', 5), ('module', None)],
   'row': 'NEGATIVE (window-taint): step-module mixed-height climb INSIDE powder (dy above and below 0.21) must SIGNAL step - the tainted window must count the band packets',
   'falsifies': 'the 3fffe5a window-taint no-hole claim: silence here means leading-hack-then-band slips through live'},
 'gs-legit-strip': {'tp': G['strip_start'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': 90,
   'legs': [('hold', 'w', 8)],
   'row': 'ground-spoof legit strip: walk the partial-block strip (slab/stair/snow/carpet) - zero nofall SIGNAL (0.1 strip tolerance, GROUND-SPOOF-TOLERANCE.md)',
   'falsifies': 'the 0.1 strip tolerance on partial blocks: a nofall SIGNAL here means a server/client shape disagreement the tolerance does not cover'},
 'se-legit-edge': {'tp': G['strip_start'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': 90,
   'legs': [('hold2', 'Shift_L', 'w', 6)],
   'row': 'sneak-edge: sneak-walk along the strip edges - zero nofall SIGNAL (clamp preserves overlap, settled from source)',
   'falsifies': 'the sneak-edge source finding: a SIGNAL means the real client lost all XZ overlap under the box, contradicting the clamp'},
 'gs-nofall': {'tp': G['strip_edge'], 'needs': 'cheat', 'checks': ('nofall',), 'expect': 'signal', 'yaw': 90,
   'legs': [('module', None), ('hold', 'w', 5), ('module', None)],
   'row': 'POSITIVE CONTROL: NoFall/ground-spoof module walking off the strip edge must SIGNAL nofall ground spoof',
   'falsifies': 'ground-spoof detection itself: silence here means the 0.1 strip check is dead in this jar'},
 'bl-burst': {'tp': G['strip_start'], 'needs': 'cheat', 'checks': ('blink',), 'expect': 'signal', 'yaw': 90,
   'legs': [('module', None), ('hold', 'w', 4), ('module', None), ('wait', 3)],
   'row': 'POSITIVE CONTROL: Blink module hold-and-release while walking must SIGNAL blink (packet burst)',
   'falsifies': 'the blink burst detector: silence here means held-and-released packets are not seen in this jar'},
}

# Grading expectations for jar 3fffe5a, against the bounce-7e live baselines (7e jar, clean rows):
# slime-25 chain maxDy 1.579 (the same launch dy that flagged "jump dy 1.579" on candidate8 pre-BounceTracker),
# bed-10 maxDy 0.787, honey maxDy 0 (no positive dy after landing). maxDy is graded offline from the captured log.
GRADE = {
 # 7e raw reps (bounce-7e-result-9faea96f.json): slime-25 peak_dy 1.256/1.361/1.579 (rep spread from
 # landing-tick alignment), bed-10 peak_dy 0.601/0.747, honey-10 no positive dy samples (end y 70.9375 =
 # honey surface). Windows below are the observed rep envelope +-5%, not a single rep.
 '7e-baseline': {'slime-25': {'maxDyReps': [1.256, 1.361, 1.579], 'signals': 0}, 'slime-10': {'signals': 0},
   'bed-10': {'maxDyReps': [0.601, 0.747], 'signals': 0}, 'honey-10': {'maxDy': None, 'signals': 0, 'endY': 70.9375}},
 '3fffe5a-expect': {
   'PB-f slime-25 (if re-run)': 'zero SIGNAL; peak_dy within [1.19, 1.66] (7e rep envelope 1.256..1.579, +-5%)',
   'PB-g bed-10 (if re-run)': 'zero SIGNAL; peak_dy within [0.57, 0.78] (7e rep envelope 0.601..0.747, +-5%)',
   'PB-j honey-10 (if re-run)': 'zero SIGNAL; no positive post-landing dy (any is a bounce-cover bug); end y ~70.9375 (honey surface)',
   'pb-a..d powder legit': 'zero SIGNAL',
   'pb-e/pb-l cheat controls': '>=1 air jump/step SIGNAL; zero is INVALID',
   'pb-m rim legit': 'zero SIGNAL; jump dy 0.42 normal',
   'pb-n rim step-cheat': 'step SIGNAL (near-not-in gate)',
   'pb-o slime edge': 'zero SIGNAL; record observed maxDy (partial bounce legal either way)',
   'pb-p bed-on-slime': 'zero SIGNAL; peak_dy within [0.57, 0.78] (bed envelope); >0.85 silent approaches slime-10 scale (~0.86 launch) = over-cover hole',
   'pb-q powder step-cheat': 'step SIGNAL (window taint)'},
}

for _k, _v in S.items():
    _v.setdefault('expectMove', _k not in ('pb-b', 'pb-m'))
    _v['needsDisarm'] = _k == 'pb-l'

def main():
    ap = argparse.ArgumentParser(description=__doc__)
    ap.add_argument('scenario', choices=sorted(S))
    ap.add_argument('--harness', type=Path, default=Path('/home/luke/meshac-work/meadow-meshac-round1-20261015'))
    ap.add_argument('--output', type=Path, required=True)
    ap.add_argument('--player', default='McR1CombatCtl16')
    ap.add_argument('--jar-sha256', required=True)
    ap.add_argument('--execute', action='store_true')
    ap.add_argument('--prepared-vanilla-client-confirmed', action='store_true')
    ap.add_argument('--prepared-cheat-client-confirmed', action='store_true')
    ap.add_argument('--module-key', default='r', help='rep-confirmed keybind toggling the air-jump-capable module on the prepared cheat client')
    ap.add_argument('--setup-receipt', type=Path, help='rep receipt of the built world geometry (powder pit, ledge, slime pad coordinates)')
    ap.add_argument('--session', default=datetime.datetime.now(datetime.timezone.utc).strftime('pb-%Y%m%dT%H%M%SZ'), help='capture session id shared by every row graded together; the positive witness must live in the same session')
    a = ap.parse_args(); sc = S[a.scenario]
    blocked = []
    if not a.setup_receipt: blocked.append('no --setup-receipt: world geometry coordinates unconfirmed')
    if sc['needs'] == 'cheat' and not a.prepared_cheat_client_confirmed: blocked.append('needs the prepared cheat client (module-on pixel check), not a second client')
    if sc['needs'] == 'vanilla' and not a.prepared_vanilla_client_confirmed: blocked.append('needs the prepared vanilla client (module-OFF pixel check)')
    if not a.execute:
        print(json.dumps({'scenario': a.scenario, 'row': sc['row'], 'needs': sc['needs'], 'expect': sc['expect'],
            'tp': sc['tp'], 'yaw': sc.get('yaw'), 'legs': sc['legs'], 'moduleKey': a.module_key if sc['needs'] == 'cheat' else None,
            'blocked': blocked or 'none (still requires --execute and all guards)',
            'falsifies': sc.get('falsifies', 'see the jar-spec falsification list'),
            'expectMove': sc['expectMove'], 'needsDisarm': sc['needsDisarm'], 'session': a.session,
            'grading': GRADE['3fffe5a-expect'],
            'passCriterion': ('zero [meshac] SIGNAL lines for the player in the capture window' if sc['expect'] == 'zero'
                else 'at least one air jump/step SIGNAL for the player; zero signals on a cheat row is INVALID, never PASS'),
            'verdictWait': 10, 'execution': 'input-only legs via the prepared client window; RCON tp only; no client start/stop'}, indent=2)); return
    if blocked: raise SystemExit('BLOCKED: ' + '; '.join(blocked))
    if not a.setup_receipt.is_file(): raise SystemExit('setup receipt path is not a file')
    if a.output.exists() and any(a.output.iterdir()): raise SystemExit('Output folder not empty; do not overwrite evidence.')
    sys.path.insert(0, str(a.harness)); from helpers import rc, x
    jar = Path('/home/luke/meshac-work/rig/red-26.2/mods/meshac.jar')
    if hashlib.sha256(jar.read_bytes()).hexdigest() != a.jar_sha256: raise SystemExit('Rig jar differs from reviewed candidate 3fffe5a build.')
    if int(Path('/proc/meminfo').read_text().split('MemAvailable:')[1].split()[0]) < 2097152: raise SystemExit('RAM guard')
    if os.statvfs('/').f_bavail * os.statvfs('/').f_frsize < 3072 * 1048576: raise SystemExit('Disk guard')
    if a.player not in rc('list'): raise SystemExit('Prepared player not online')
    pid = int((a.harness / 'client-control.pid').read_text()); cmd = Path(f'/proc/{pid}/cmdline').read_bytes()
    if b'client-control' not in cmd: raise SystemExit('Control PID ownership mismatch')
    # No client start/stop, server/world copies, entity kills or jar/config writes.
    a.output.mkdir(parents=True, exist_ok=True)
    def shot(name): subprocess.run(['import', '-display', ':90', '-window', 'root', str(a.output / name)], check=True)
    w = subprocess.check_output(['xdotool', 'search', '--onlyvisible', '--pid', str(pid)], env={**os.environ, 'DISPLAY': ':90'}, text=True).splitlines()[-1]
    x('windowfocus', w); shot('before.png')
    def pos():
        out = rc('data get entity ' + a.player + ' Pos')
        vec = out.split('[', 1)[1].split(']')[0].replace('d', '').split(',')
        return [float(v) for v in vec]
    tick_health = rc('tick query')
    mod_liveness = rc('mesh cases')[:200] # the /mesh command answering proves the jar is live and watching
    since = datetime.datetime.now(datetime.timezone.utc).isoformat(); events = []; disarm = None
    try:
        rc('tp ' + a.player + ' ' + sc['tp'] + (' ' + str(sc.get('yaw')) + ' 0' if sc.get('yaw') is not None else ''))
        time.sleep(2); start = time.monotonic()
        for leg in sc['legs']:
            p0 = pos()
            if leg[0] == 'wait': time.sleep(leg[1])
            elif leg[0] == 'hold' and sc['needsDisarm'] and leg[1] == 'Shift_L':
                x('keydown', leg[1]); time.sleep(leg[2] / 2)
                disarm = 'SNEAKING' in rc('data get entity ' + a.player + ' Pose') # the sneak leg really sneaked
                time.sleep(leg[2] / 2); x('keyup', leg[1])
            elif leg[0] == 'hold': x('keydown', leg[1]); time.sleep(leg[2]); x('keyup', leg[1])
            elif leg[0] == 'hold2': x('keydown', leg[1]); x('keydown', leg[2]); time.sleep(leg[3]); x('keyup', leg[1]); x('keyup', leg[2])
            elif leg[0] == 'tap': x('key', leg[1])
            elif leg[0] == 'module': x('key', a.module_key)
            p1 = pos()
            events.append({'leg': leg[0], 't': time.monotonic() - start, 'posDelta': [round(p1[i] - p0[i], 3) for i in range(3)]})
        time.sleep(10); shot('end.png')
    finally:
        for k in ('space', 'w', 'Shift_L'): x('keyup', k)
        log = subprocess.check_output(['docker', 'logs', 'meshac-red', '--since', since], stderr=subprocess.STDOUT, text=True)
        (a.output / 'server.log').write_text(log)
        signals = [l for l in log.splitlines() if 'SIGNAL ' + a.player in l]
        bound = signal_checks.bound(signals) # TEXT2CHECK: exact check-field binding, all CheckNames.ALL checks
        passed = (len(signals) == 0) if sc['expect'] == 'zero' else (bool(bound & set(sc['checks'])) if sc.get('checks') else bool(bound))
        result = {'scenario': a.scenario, 'row': sc['row'], 'player': a.player, 'jarSHA': a.jar_sha256, 'expect': sc['expect'],
            'needs': sc['needs'], 'session': a.session, 'expectMove': sc['expectMove'], 'needsDisarm': sc['needsDisarm'],
            'offState': {'moduleOffPixelConfirmed': bool(a.prepared_vanilla_client_confirmed)} if sc['needs'] == 'vanilla' else None,
            'disarmVerified': disarm, 'tickHealth': tick_health, 'modLiveness': mod_liveness,
            'falsifies': sc.get('falsifies'), 'grading': GRADE['3fffe5a-expect'],
            'events': events, 'signals': signals, 'checks': sc.get('checks'), 'verdict': 'PASS' if passed else ('FAIL' if sc['expect'] == 'zero' else 'INVALID'),
            'setupReceipt': a.setup_receipt.read_text(),
            'reviewRequired': 'Driver verdict is provisional; pb_grade.py owns the final verdict (provenance, session witness, OFF/disarm). Zero signals is INVALID on a cheat row, PASS-only on a zero row. xdotool legs are not a human motor baseline.'}
        (a.output / 'result.json').write_text(json.dumps(result, indent=2))
        print(json.dumps(result, indent=2))
if __name__ == '__main__': main()
