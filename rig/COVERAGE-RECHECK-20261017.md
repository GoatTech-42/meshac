# Newbounds spec vs control matrix - coverage re-check (2026-10-17, patch 0014)

CONTROL-MATRIX-cheats-vs-checks rows re-checked against pb-controls.py + newbounds_controls.py
after patch 0014. Strings verified against the matrix doc and source where cited.

| matrix family | check | bound string | fixture row | status |
|---|---|---|---|---|
| fly | fly | hover/ascend strings | nb-fly-legit/cheat | COVERED |
| speed | speed | speed strings | nb-speed-legit/cheat | COVERED |
| combat (reach etc) | combat | computed hit strings | nb-combat-legit/cheat | COVERED - CONDITIONAL: SIGNAL is a dead path on d124 (matrix; Luke ship-vs-fix OPEN), blind control = INCONCLUSIVE |
| mining work rate | mining | work-rate strings | nb-mine-legit/cheat | COVERED |
| nuker / fast nuke (r04) | mining | "breaks blocks in every direction"; "block broken through another block"; "broke a block it is not looking at" | none | GAP - same check, different detail: nb-mine's NoBreakDelay/PacketMine trips the work-rate path, not the nuker strings. Meadow Nuker exists; row draftable |
| interact use rate | interact | uses/s strings | nb-interact-legit/cheat | COVERED |
| click behind / wrong-face (r03) | interact | "clicked a block it is not looking at"; "clicked a block through another block" | none | GAP - meadow GhostHand exists |
| scaffold / fast bridge | interact | "bridging at %.1f blocks a second"; "placed against air" | none | GAP - meadow scaffold rework still in design (HUD lane); AirPlace exists for the against-air string |
| inventory stealer | inventory | "more than <n> item moves in <ms> ms" | nb-inv-legit/cheat | COVERED |
| beat mover | inventory | "item moves on a steady beat of <gap> ms" | none | GAP (FP24 family; BF09/BF17 S3 OPEN_HOLE per matrix) |
| auto-totem | inventory | "off hand filled <n> ms after a totem popped..." (Totem.java:39) | none | GAP - meadow AutoTotem exists; matrix notes the bypass fixed in patch 9 |
| nofall / ground-spoof | nofall | "ground spoof"; "ground flag in mid-air" (RE-BIND at built battery jar per B2 hold) | GS01-GS03 + nb-gc01/02/05 | COVERED - GC01/GC05 are patch-1 GO conditions |
| intentional blink | blink | "held its movement packets back and released them in one burst" (:78) | bl-burst + BF08/BF14 grader pins (patch 0013) | COVERED |
| boat fly | boatfly | "boat climbing in open air" (Vehicle.java:29, RISE_AT=5) - was UNBOUND in the matrix, bound from source 10-17 | nb-boatfly-legit/cheat (patch 0014) | COVERED - needs meadow rig BoatFly (-Prig build) |
| air jump | air | "air jump dy %.3f" (FpGuards.airJumpFlag) | pb-e / pb-l | COVERED (pb family) |
| step | step | "step dy %.3f onGround" | pb-n / pb-q | COVERED (pb family) |
| powder struggle | jump | band 0.60-0.70 (FpGuards.powderJumpBand) | pb-e + GS03 + nb-gc05 | COVERED |
| microhop (r01) | microhop | "microhop dy %.3f" (bufHop>=3) | none | GAP - needs the snow-ramp + ceiling geometry from r01 |
| high jump (r02) | high | "high jump rose %.2f" | string bound via GS02 expectation | PARTIAL - no meadow HighJump fixture row; module exists, open-pad row draftable |
| timer (r07) | timer | "timer ahead %.0f ms" (balMs>1300) | none (grader binds 'timer') | GAP - meadow Timer exists; the control needs latency shaping |
| glide abuse (BF13) | glide | "restarted gliding..."; "flying at ..." | none | GAP - meadow ElytraFly exists |
| antiknockback | antiknockback | "no push after three hits" | none | GAP - meadow Velocity exists; needs a hitter (mob or second bot) |
| jesus / slippery | movement | "slippery ground: speed barely drops on a normal block" (:154) | none | GAP |
| levitation abuse | levitation | "levitation dy %.3f" (:177) | none | GAP |
| ladder fly | ladder | "ladder dy %.3f" (:181) | none | GAP |
| aimbot / not-looking | combat | "not looking at target" computed, never emitted | n/a | DEAD (matrix) - same d124 combat hole |

Grader note: all 23 checks bind by the structured check field (patch 7), so every GAP is a
missing row/geometry/module, not grader wiring. Rows for nuker, GhostHand, HighJump, Timer,
glide, AutoTotem are draftable today from existing meadow modules; microhop/antiknockback need
new geometry or a second actor.
