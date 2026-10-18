# Rig capture request: fixtures for the newly-bound checks (fly / speed / combat / mining / interact / inventory)

Companion to RIG-CAPTURE-REQUEST-POWDER-BOUNCE.md. These rows exercise the checks the grader
bound in the TEXT2CHECK extension (rig/signal_checks.py, patch 0007) that had no synthetic
fixtures yet. Spec only - Box rep runs it; rows are in pb-controls leg language
(rig/newbounds_controls.py, machine-checked by rig/test-newbounds_controls.py).

## World setup (one flat world, all coords Y=70 pads unless noted)

- flat lane: 40-block line of solid full blocks, x 100..140 at z 100, surface y 70 (speed/fly rows).
- combat pen: a zombie (no AI if possible; a named zombie in a 1x1 pen) at 160 70 160.
  Player spots: LEGIT at 158.5 70 160 (2.5 blocks), CHEAT at 155.5 70 160 (4.5 blocks - beyond vanilla reach 3.0 + 0.2 margin, Combat.java:123).
- mine wall: 3 wide x 10 high stone wall at x 180, z 180..183, base y 70.
  Player holds a plain diamond pickaxe (NO efficiency; setup receipt photos the held item).
- place floor: 20x20 stone floor at 200 69 200; player at 210.5 70 210.5 with a stack of stone in hand.
- inv chest: double chest at 220 70 220 filled with 54 distinct stacks; player at 220.5 70 221.5.

## Leg-vocabulary extensions (Box: same xdotool driver, two new verbs)

- ('click', button, n): n clicks of mouse button ('1' left, '3' right), human pace ~4/s unless noted.
- ('holdmouse', button, seconds): hold a mouse button (mining).
('module', None) means: press the rep-confirmed keybind for THE MODULE NAMED IN THE ROW
(one rep session per module, or rebind between rows; the setup receipt names the binding).

## Rows (S table in rig/newbounds_controls.py is authoritative; this is the reading view)

| row | needs | module | expect | checks | what |
|---|---|---|---|---|---|
| nb-fly-legit | vanilla | - | zero | - | sprint-jump the lane: no fly SIGNAL (F6 in the old triage) |
| nb-fly-cheat | cheat | Fly (0011 abilities mode) | signal | fly | hover+ascend 5 s must SIGNAL fly (hover/ascend) |
| nb-speed-legit | vanilla | - | zero | - | plain sprint down the lane: no speed SIGNAL |
| nb-speed-cheat | cheat | Speed | signal | speed | Speed on the lane must SIGNAL speed |
| nb-combat-legit | vanilla | - | zero | - | attack the zombie at 2.5 blocks, ~4 clicks/s: no combat SIGNAL (reach limit 3.0+0.2) |
| nb-combat-cheat | cheat | Aimbot | signal | combat | attack from 4.5 blocks with Aimbot must SIGNAL combat (reach) |
| nb-mine-legit | vanilla | - | zero | - | mine 5 stone with an unenchanted diamond pick: no mining SIGNAL |
| nb-mine-cheat | cheat | NoBreakDelay + PacketMine | signal | mining | must SIGNAL mining (work rate) |
| nb-interact-legit | vanilla | - | zero | - | place 12 blocks at ~4/s: no interact SIGNAL (limit 16/s, Interact.java:15) |
| nb-interact-cheat | cheat | FastPlace | signal | interact | FastPlace must SIGNAL interact (uses/s) |
| nb-inv-legit | vanilla | - | zero | - | move 3 stacks chest<->inventory at human pace: no inventory SIGNAL (limit 10/100 ms, InventoryBurst.java:13) |
| nb-inv-cheat | cheat | ChestStealer | signal | inventory | ChestStealer on the full chest must SIGNAL inventory (burst) |

Vanilla rows need the module-off pixel confirmation (same OFF-state receipt as PB rows).
Zero rows carry no 'checks' (any bound SIGNAL fails them, per pb_grade).

## Falsification claims (what silence/noise on each row kills)

- nb-fly-cheat silence: fly check dead on this jar (the fly lane is unmonitored).
- nb-speed-cheat silence: speed check dead.
- nb-combat-cheat silence: reach check dead at >3.2 (Combat.java:123).
- nb-combat-legit noise: the 750 ms (eye,box) rewind window FPs on a static target.
- nb-mine-cheat silence: MiningRate dead vs NoBreakDelay/PacketMine.
- nb-interact-cheat silence: the 16/s use counter dead vs FastPlace.
- nb-inv-cheat silence: InventoryBurst dead vs ChestStealer.
- Any vanilla-row noise: names the check whose FP gate needs work (grader FAILs the row).

## Receipts per session

Same as PB: setup receipt (coords + module binding + held item photo), tick health, mod liveness
('mesh cases'), per-row server.log slice, end.png. Grade with pb_grade.py per session dir;
the 'checks' field in each result scopes positive rows to their check (off-target = INVALID).
