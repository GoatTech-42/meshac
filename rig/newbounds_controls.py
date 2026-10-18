#!/usr/bin/env python3
"""Capture-row SPEC for the newly-bound checks (fly/speed/combat/mining/interact/inventory).
Same shape as pb-controls.py S/G so Box can fold it into the driver at rig time; rows and world
geometry per rig/RIG-CAPTURE-REQUEST-NEWBOUNDS.md. Leg verbs: pb-controls' wait/hold/hold2/tap/module
plus the two NEWBOUNDS extensions ('click',button,n) and ('holdmouse',button,seconds).
This file is a spec, not a wired driver - test-newbounds_controls.py machine-checks it."""
import signal_checks

G = {
 'lane_start': '100.5 70 100.5',   # 40-block flat lane toward +x, surface y 70
 'combat_legit': '158.5 70 160',   # 2.5 blocks from the penned zombie (vanilla reach 3.0)
 'combat_cheat': '155.5 70 160',   # 4.5 blocks - beyond the 3.0 + 0.2 margin (Combat.java:123)
 'mine_wall': '179.5 70 181.5',    # facing the 3x10 stone wall at x 180
 'place_floor': '210.5 70 210.5',  # 20x20 stone floor, stone stack in hand
 'inv_chest': '220.5 70 221.5',    # facing the filled double chest
}

S = {
 # fly
 'nb-fly-legit': {'tp': G['lane_start'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('hold2', 'w', 'space', 8)],
   'row': 'sprint-jump the lane: zero fly SIGNAL',
   'falsifies': 'normal sprint-jumping must not trip the fly hover/ascend buffers'},
 'nb-fly-cheat': {'tp': G['lane_start'], 'needs': 'cheat', 'expect': 'signal', 'module': 'Fly (abilities mode)', 'checks': ('fly',), 'yaw': -90,
   'legs': [('module', None), ('hold', 'space', 5), ('wait', 2), ('module', None)],
   'row': 'POSITIVE CONTROL: Fly (0011 abilities mode) hover+ascend must SIGNAL fly',
   'falsifies': 'fly detection itself: silence here means the fly lane is unmonitored on this jar'},
 # speed
 'nb-speed-legit': {'tp': G['lane_start'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('hold2', 'w', 'LCtrl', 8)],
   'row': 'plain sprint down the lane: zero speed SIGNAL',
   'falsifies': 'vanilla sprint (5.6 b/s) must stay under the speed envelope'},
 'nb-speed-cheat': {'tp': G['lane_start'], 'needs': 'cheat', 'expect': 'signal', 'module': 'Speed', 'checks': ('speed',), 'yaw': -90,
   'legs': [('module', None), ('hold', 'w', 8), ('module', None)],
   'row': 'POSITIVE CONTROL: Speed module down the lane must SIGNAL speed',
   'falsifies': 'speed detection itself'},
 # combat
 'nb-combat-legit': {'tp': G['combat_legit'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('click', '1', 12)],
   'row': 'attack the zombie at 2.5 blocks ~4/s: zero combat SIGNAL',
   'falsifies': 'the 750 ms (eye,box) rewind window must not FP on a static in-range target'},
 'nb-combat-cheat': {'tp': G['combat_cheat'], 'needs': 'cheat', 'expect': 'signal', 'module': 'Aimbot', 'checks': ('combat',), 'yaw': -90,
   'legs': [('module', None), ('wait', 6), ('module', None)],
   'row': 'POSITIVE CONTROL: Aimbot attacking from 4.5 blocks must SIGNAL combat (reach > 3.2)',
   'falsifies': 'the reach check: silence here means reach > 3.2 is unseen on this jar'},
 # mining
 'nb-mine-legit': {'tp': G['mine_wall'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('holdmouse', '1', 12)],
   'row': 'mine 5 stone, unenchanted diamond pick: zero mining SIGNAL',
   'falsifies': 'MiningRate must cover vanilla stone breaking (incl. the break-tick progress edge)'},
 'nb-mine-cheat': {'tp': G['mine_wall'], 'needs': 'cheat', 'expect': 'signal', 'module': 'NoBreakDelay + PacketMine', 'checks': ('mining',), 'yaw': -90,
   'legs': [('module', None), ('holdmouse', '1', 10), ('module', None)],
   'row': 'POSITIVE CONTROL: NoBreakDelay/PacketMine on the wall must SIGNAL mining (work rate)',
   'falsifies': 'MiningRate vs no-delay packet mining'},
 # interact
 'nb-interact-legit': {'tp': G['place_floor'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('click', '3', 12)],
   'row': 'place 12 blocks at ~4/s: zero interact SIGNAL (limit 16/s, Interact.java:15)',
   'falsifies': 'fast-but-human bridging (~8/s) must stay under the use counter'},
 'nb-interact-cheat': {'tp': G['place_floor'], 'needs': 'cheat', 'expect': 'signal', 'module': 'FastPlace', 'checks': ('interact',), 'yaw': -90,
   'legs': [('module', None), ('holdmouse', '3', 6), ('module', None)],
   'row': 'POSITIVE CONTROL: FastPlace must SIGNAL interact (uses/s)',
   'falsifies': 'the 16/s use counter vs FastPlace'},
 # inventory
 'nb-inv-legit': {'tp': G['inv_chest'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('click', '3', 1), ('wait', 1), ('click', '1', 6), ('wait', 1), ('click', '1', 6)],
   'row': 'move 3 stacks chest<->inventory at human pace: zero inventory SIGNAL (limit 10/100 ms)',
   'falsifies': 'normal looting must not trip InventoryBurst'},
 'nb-inv-cheat': {'tp': G['inv_chest'], 'needs': 'cheat', 'expect': 'signal', 'module': 'ChestStealer', 'checks': ('inventory',), 'yaw': -90,
   'legs': [('module', None), ('click', '3', 1), ('wait', 5), ('module', None)],
   'row': 'POSITIVE CONTROL: ChestStealer on the full chest must SIGNAL inventory (burst)',
   'falsifies': 'InventoryBurst vs chest stealing'},
}

LEG_VERBS = {'wait': 2, 'hold': 3, 'hold2': 4, 'tap': 2, 'module': 2, 'click': 3, 'holdmouse': 3}

for _k, _v in S.items():
    _v.setdefault('expectMove', _v['expect'] == 'zero' or _k not in ('nb-combat-cheat', 'nb-inv-cheat'))

def validate():
    """Machine-checks the spec; raises AssertionError on the first malformed row."""
    for name, sc in S.items():
        assert sc['expect'] in ('zero', 'signal'), name
        assert sc['needs'] in ('vanilla', 'cheat'), name
        assert sc['tp'].split()[0] and sc['row'] and sc['falsifies'], name
        for leg in sc['legs']:
            assert leg[0] in LEG_VERBS and len(leg) == LEG_VERBS[leg[0]], (name, leg)
        if sc['expect'] == 'signal':
            assert sc.get('checks') and set(sc['checks']) <= signal_checks.CHECKS, name
            assert sc.get('module'), name + ': cheat row must name its meadow module'
        else:
            assert 'checks' not in sc, name + ': zero rows take no checks (any signal fails them)'
    assert all(isinstance(v, str) and len(v.split()) == 3 for v in G.values())
    return True

if __name__ == '__main__':
    validate(); print('newbounds spec ok: %d rows' % len(S))
