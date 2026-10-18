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
 'sneak_edge': '100.5 70 180.5',   # north edge of the lane platform, 1-block drop past z 180 (partial overhang work)
 'powder_pit': '240.5 70 240.5',   # center of a 5x5x3 powder-snow pit, floor y 68 (Box places the powder)
 'autostep_lane': '260.5 70 100.5', # 20-block alternating slab/stair lane toward +x
 'boat_lake': '280.5 69 140.5',    # 10x10 water pool, surface y 69 (Box places the boat)
 'boat_pad': '280.5 70 180.5',     # flat stone pad, no water/bubble column anywhere near (Box places the boat)
 'snow_ramp': '300.5 70 300.5',    # 12-long ascending snow-layer ramp (1..8 layers) toward +x with a 2-block ceiling over the top half - Box builds it (microhop r01 geometry)
 'kb_pen': '158.5 70 160',         # AT the penned zombie (the combat pen doubles as the hitter pen - the zombie is the second actor)
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
   'row': 'POSITIVE CONTROL: Aimbot attacking from 4.5 blocks must SIGNAL combat (reach > 3.2). CONDITIONAL: combat SIGNAL is a dead path on d124 (control matrix 10-17, checklist 4.1; Luke ship-vs-fix decision OPEN) - on a d124-based jar expect BLIND CONTROL = INCONCLUSIVE + a d124 finding, not a grader failure',
   'falsifies': 'the reach check: silence on a jar with a live emit path means reach > 3.2 is unseen'},
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
 # STATUS: every row in this file is a DRAFT until its driver run + receipts prove the setup,
 # duration and attack effect; the scoreboard only records verdicts from receipt-backed runs.
 # Structured setup: 'setup_rcon'/'mid_rcon'/'teardown_rcon' are RCON command lists the Box runner
 # executes in order (receipts = captured command output); 'effect_check' is the server-confirmed
 # attack-effect verification. Rows whose setup/effect is not yet automated stay DRAFT - the fields
 # are the implementation target, and a run without the receipts is INCONCLUSIVE material only.
 # ground-spoof zero-FP battery rows (reviewer GO conditions on patch 1, relayed via parent 10-17 7:44pm)
 'nb-gc01-sneakedge': {'tp': G['sneak_edge'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': 0,
   'legs': [('hold2', 'w', 'Shift_L', 1), ('hold2', 's', 'Shift_L', 1)] * 150,  # 150 x 2s = 300s - the pre-committed 5-minute bound, bound IN the legs
   'row': 'GC01 (reviewer row 1): sneak-edge oscillation, worst-case partial overhang work. The legs themselves carry the bound: 150 cycles x 2s = 300s; a shorter run does not satisfy the row. DRAFT until the driver receipt (leg log + 300s wall-clock span in the capture) proves the duration. PRE-COMMITTED jitter-density bound: ZERO nofall SIGNAL lines over the whole row; a single flag kills the shipped window-5/flag-3 proposal and goes back to the reviewer with the gndclaim trace lines, no silent widening',
   'falsifies': 'the 3-of-5 window is a strict superset of the old 3-consecutive trigger - sustained off-edge jitter is exactly the marginal FP surface'},
 'nb-gc02-autostep': {'tp': G['autostep_lane'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('hold', 'w', 10), ('wait', 1), ('hold', 'w', 10)],
   'row': 'GC02 (reviewer: slab/stair auto-step stands as a third row on its own merits): vanilla auto-step keeps +gnd supported, so claims never enter the window unsupported; ZERO nofall SIGNAL',
   'falsifies': 'auto-step rise packets (dy ~0.6, fewer than 6 rising) must not arm or count the window'},
 'nb-gc05-jb-powder': {'tp': G['powder_pit'], 'needs': 'vanilla', 'expect': 'zero',
   'legs': [('hold', 'space', 20), ('wait', 2), ('hold', 'space', 20)],
   'setup_rcon': ['/effect give @p minecraft:jump_boost 60 0', '/clear @p minecraft:leather_boots'],
   'mid_rcon': ['/effect give @p minecraft:jump_boost 60 1', '/clear @p minecraft:leather_boots'],
   'teardown_rcon': ['/effect clear @p minecraft:jump_boost'],
   'row': 'GC05 (reviewer row 2): Jump Boost I then II powder struggle climb-out. Two phases, same legs each: setup_rcon (JB I + no-boots verify - /clear must report 0 removed), run legs; mid_rcon (JB II + re-verify), run legs again; teardown_rcon. DRAFT until the receipts (both effect results + both no-boots verifications) are captured. Acceptance: 0 nofall flags. REPORT the measured struggle dy values - never widen the 0.60-0.70 band silently (calibrated to ~0.63 vanilla struggle). The nofall exempt gate has NO powder entry (Movement.java:159-172, verified 10-17): this row is the only mitigation for in-powder unsupported claims; a flag here routes to rig/POWDER-EXEMPT-DRAFT.md',
   'falsifies': 'a legit JB struggle-exit landing outside the band, or in-powder +gnd claims reading unsupported (powder collision is entity-conditional), accumulates window evidence - this row proves whether the real client ever emits that pattern'},
 # boatfly (bound from source 10-17: Vehicle.java:29 - "boat climbing in open air" after 5 consecutive rising vehicle packets with no water/ground/bubble column, RISE_AT=5)
 'nb-boatfly-legit': {'tp': G['boat_lake'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('hold', 'w', 8), ('wait', 1), ('hold', 'w', 8)],
   'row': 'paddle the boat across the pool: zero boatfly SIGNAL',
   'falsifies': 'normal paddling and small wave bobbing must not count as open-air climbing'},
 'nb-boatfly-cheat': {'tp': G['boat_pad'], 'needs': 'cheat', 'expect': 'signal', 'module': 'BoatFly (rig-only, -Prig build)', 'checks': ('boatfly',), 'yaw': -90,
   'legs': [('module', None), ('hold', 'space', 6), ('module', None)],
   'setup_rcon': ['/give @p minecraft:oak_boat 1'],
   'effect_check': 'server-confirmed boat rise: /data get entity @e[type=minecraft:boat,sort=nearest,limit=1] Pos[1] before and after the hold - y must increase',
   'row': 'POSITIVE CONTROL: rig BoatFly climbing in open air must SIGNAL boatfly ("boat climbing in open air") after 5 consecutive rising vehicle packets with no lift (Vehicle.java: RISE_AT=5). setup_rcon gives the boat; the bot places and boards it at boat_pad BEFORE the module leg. Attack effect = effect_check (server-confirmed rise); without it INCONCLUSIVE, never EVADE. DRAFT until receipts',
   'falsifies': 'the vehicle rise counter: silence here means open-air boat climbing is unseen'},
 # nuker (matrix r04): same mining check as nb-mine, different detail strings
 'nb-nuker-cheat': {'tp': G['mine_wall'], 'needs': 'cheat', 'expect': 'signal', 'module': 'Nuker', 'checks': ('mining',), 'yaw': -90,
   'legs': [('module', None), ('wait', 8), ('module', None)],
   'row': 'POSITIVE CONTROL: Nuker on the wall must SIGNAL mining with a nuker string ("breaks blocks in every direction" / "block broken through another block" / "broke a block it is not looking at"); legit twin = nb-mine-legit',
   'falsifies': 'the direction/look-ray mining checks vs an omnidirectional nuker'},
 # GhostHand (matrix r03): wrong-face/behind placement
 'nb-ghosthand-cheat': {'tp': G['place_floor'], 'needs': 'cheat', 'expect': 'signal', 'module': 'GhostHand', 'checks': ('interact',), 'yaw': 90,
   'legs': [('module', None), ('click', '3', 10), ('module', None)],
   'effect_check': 'server-confirmed target block change: /execute if block at the target coords, before vs after the clicks - placement PACKETS alone are NOT proof',
   'row': 'POSITIVE CONTROL: GhostHand placing while facing AWAY (yaw 90, back to the floor) must SIGNAL interact ("clicked a block it is not looking at" / "clicked a block through another block"); legit twin = nb-interact-legit. Facing away, the clicks may place NOTHING: attack-effect proof = effect_check (server-confirmed target change ONLY); without it the row is INCONCLUSIVE, never EVADE. DRAFT until receipts',
   'falsifies': 'the look-ray interact checks vs behind-the-back placement'},
 # high jump (matrix r02 control)
 'nb-high-legit': {'tp': G['lane_start'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('tap', 'space'), ('wait', 1), ('tap', 'space'), ('wait', 1), ('tap', 'space')],
   'row': 'plain jumps on the flat lane: zero high SIGNAL (room is 1.45 + 0.6 per JB level, Movement.java:274-278)',
   'falsifies': 'a vanilla jump apex (~1.25) must stay under the high-jump room'},
 'nb-high-cheat': {'tp': G['lane_start'], 'needs': 'cheat', 'expect': 'signal', 'module': 'HighJump (+1.0)', 'checks': ('high',), 'yaw': -90,
   'legs': [('module', None), ('tap', 'space'), ('wait', 3), ('module', None)],
   'row': 'POSITIVE CONTROL: HighJump must SIGNAL high ("high jump rose %.2f") once y - airY0 > 1.45 with dy > 0.05, no exempt event within 4 s, not bouncing',
   'falsifies': 'the high-jump rise accumulator vs a boosted jump'},
 # timer (matrix r07)
 'nb-timer-legit': {'tp': G['lane_start'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('hold', 'w', 15)],
   'row': 'plain sprint 15 s: zero timer SIGNAL (balMs must stay under 1300; the stalled-connection bank allows 1.6 s of catch-up)',
   'falsifies': 'vanilla 20 pps pacing must not bank 1.3 s'},
 'nb-timer-cheat': {'tp': G['lane_start'], 'needs': 'cheat', 'expect': 'signal', 'module': 'Timer (x1.2)', 'checks': ('timer',), 'yaw': -90,
   'legs': [('module', None), ('hold', 'w', 15), ('module', None)],
   'row': 'POSITIVE CONTROL: Timer x1.2 must SIGNAL timer ("timer ahead %.0f ms") - at 1.2x the balance banks ~200 ms/s, crossing 1300 in ~7 s (matrix r07: run under latency)',
   'falsifies': 'the packet-balance clock vs a 20% game-speed hack'},
 # glide (matrix BF13): elytra equipped by Box via RCON before both rows
 'nb-glide-legit': {'tp': G['sneak_edge'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': 0,
   'legs': [('hold', 'w', 1), ('tap', 'space'), ('wait', 1), ('tap', 'space'), ('hold', 'w', 3)],
   'setup_rcon': ['/item replace entity @p armor.chest with minecraft:elytra'],
   'row': 'legit elytra hop-glide off the edge (setup_rcon equips the elytra, receipt captured; if the hop is too shallow to deploy, Box substitutes a 3-block pillar at these coords): zero glide SIGNAL. DRAFT until the equip receipt is captured',
   'falsifies': 'a real fall-start glide must not read as restarted gliding'},
 'nb-glide-cheat': {'tp': G['lane_start'], 'needs': 'cheat', 'expect': 'signal', 'module': 'ElytraFly', 'checks': ('glide',), 'yaw': -90,
   'legs': [('module', None), ('tap', 'space'), ('hold', 'w', 8), ('module', None)],
   'setup_rcon': ['/item replace entity @p armor.chest with minecraft:elytra'],
   'effect_check': 'flat glide without firework/fall-start: y holds or rises over the hold, read from the capture',
   'row': 'POSITIVE CONTROL: ElytraFly must SIGNAL glide ("restarted gliding..." / "flying at ..."). setup_rcon equips the elytra before the legs. Attack effect = effect_check; without it INCONCLUSIVE, never EVADE. DRAFT until receipts',
   'falsifies': 'the glide re-arm and no-air-drag checks vs a firework-free flat-glide module'},
 # auto-totem (matrix totem rows): executable setup below; the pop is /damage mid-row
 'nb-totem-legit': {'tp': G['lane_start'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90, 'expectMove': False,
   'legs': [('wait', 10)],
   'setup_rcon': ['/item replace entity @p weapon.offhand with minecraft:totem_of_undying'],
   'mid_rcon': ['/damage @p 20'],
   'row': 'one totem in the offhand, popped, NO refill: zero inventory SIGNAL (Totem.java:39 fires on the fast refill, not the pop). setup_rcon equips, mid_rcon pops; the pop receipt = the death-protection log line. DRAFT until receipts',
   'falsifies': 'a vanilla pop leaving the offhand empty must not read as auto-totem'},
 'nb-totem-cheat': {'tp': G['lane_start'], 'needs': 'cheat', 'expect': 'signal', 'module': 'AutoTotem (totems in hotbar)', 'checks': ('inventory',), 'yaw': -90, 'expectMove': False,
   'legs': [('module', None), ('wait', 10), ('module', None)],
   'setup_rcon': ['/give @p minecraft:totem_of_undying 8', '/item replace entity @p weapon.offhand with minecraft:totem_of_undying'],
   'mid_rcon': ['/damage @p 20'],
   'effect_check': 'server-confirmed refill: /data get entity @p Inventory shows the offhand re-filled after the pop',
   'row': 'POSITIVE CONTROL: AutoTotem: the offhand refill must SIGNAL inventory ("off hand filled <n> ms after a totem popped..."). setup_rcon stocks + equips, mid_rcon pops. Attack effect = effect_check; without an observed refill INCONCLUSIVE, never EVADE. DRAFT until receipts',
   'falsifies': 'the totem-refill timer vs an inventory-touching totem module'},
 # microhop (matrix r01): FP-side row only - meadow has NO microhop module (Speed has no hop modes),
   # and scripted driver hops are vanilla jumps, NOT a packet-level microhop. The cheat row waits on a
   # real attack (new meadow module or a packet tool); tracked in COVERAGE-RECHECK + the scoreboard.
 'nb-microhop-legit': {'tp': G['snow_ramp'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('hold2', 'w', 'space', 6), ('wait', 1), ('hold2', 'w', 'space', 6)],
   'row': 'sprint-jump up the snow ramp UNDER the 2-block ceiling: zero microhop SIGNAL (the lowCeiling + powder guards at Movement.java:300-305 exist for exactly this FP - Luke sprint-jump-under-2-block, 2026-10-11)',
   'falsifies': 'ceiling-clipped jumps and snow-layer steps must not count as microhop'},
 # antiknockback: the zombie is the second actor (hitter); Box keeps the bot alive via RCON (armor + Resistance II)
 'nb-kb-legit': {'tp': G['kb_pen'], 'needs': 'vanilla', 'expect': 'zero', 'yaw': -90,
   'legs': [('wait', 12)],
   'setup_rcon': ['/summon minecraft:zombie 158.5 70 160', '/item replace entity @p armor.chest with minecraft:iron_chestplate', '/effect give @p minecraft:resistance 60 1'],
   'row': 'stand at the zombie pen taking ~3 melee hits: vanilla knockback moves the bot ~0.5/hit - zero antiknockback SIGNAL. setup_rcon spawns the penned hitter (AI on) + armor/resistance. Baseline required: a measured vanilla knockback from the capture before this row reads as anything; DRAFT until receipts',
   'falsifies': 'normal knockback must clear the movement eval within 450 ms of each hit'},
 'nb-kb-cheat': {'tp': G['kb_pen'], 'needs': 'cheat', 'expect': 'signal', 'module': 'Velocity (horizontal 0.0)', 'checks': ('antiknockback',), 'yaw': -90, 'expectMove': False,
   'legs': [('module', None), ('wait', 12), ('module', None)],
   'setup_rcon': ['/summon minecraft:zombie 158.5 70 160', '/item replace entity @p armor.chest with minecraft:iron_chestplate', '/effect give @p minecraft:resistance 60 1'],
   'effect_check': 'measured knockback ~0 under the module vs the nb-kb-legit baseline, both from the capture',
   'row': 'POSITIVE CONTROL: Velocity horizontal=0.0 taking ~3 zombie melee hits: "no push after three hits" must SIGNAL antiknockback. Same setup_rcon as nb-kb-legit, open space behind the bot. Attack effect = effect_check; without BOTH measurements INCONCLUSIVE, never EVADE. DRAFT until receipts',
   'falsifies': 'the three-strike no-push counter vs a zero-knockback module'},
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
