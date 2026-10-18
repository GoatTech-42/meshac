# Red-team scoreboard SKELETON (format: REDTEAM-SCOREBOARD-FORMAT.md; fill at battery time)

Jar: <sha256, patch level> | meadow build: <sha> | rig: <id> | run date: <date>

| check | meadow attack (module + config) | bound string (source) | legit twin | meshac result | verdict | evidence |
|---|---|---|---|---|---|---|
| nofall | GroundSpoof (interleaved; exact config bound at run time per battery nit 3) | "ground spoof"; "ground flag in mid-air" (RE-BIND from the built battery jar - B2 hold) | nb-gc01-sneakedge, nb-gc02-autostep, nb-gc05-jb-powder | | | |
| fly | Fly (abilities mode) | hover/ascend strings | nb-fly-legit | | | |
| speed | Speed | speed strings | nb-speed-legit | | | |
| combat | Aimbot @ 4.5 blocks | "reach %.2f" etc - LIVE on d124 under monitor mode (log-only, real mon heat); the 10-17 dead-path claim was retracted (0048) | nb-combat-legit | | | |
| mining | NoBreakDelay + PacketMine | work-rate strings | nb-mine-legit | | | |
| mining | Nuker | "breaks blocks in every direction" / "through another block" / "not looking at" | nb-mine-legit | | | |
| interact | FastPlace | uses/s strings | nb-interact-legit | | | |
| interact | GhostHand (facing away) | "clicked a block it is not looking at" / "through another block" | nb-interact-legit | | | |
| interact | scaffold/fast bridge | "bridging at %.1f blocks a second" / "placed against air" | (row GAP - scaffold rework in design) | | | |
| inventory | ChestStealer | "more than <n> item moves in <ms> ms" | nb-inv-legit | | | |
| inventory | AutoTotem (RCON pop) | "off hand filled <n> ms after a totem popped..." | nb-totem-legit | | | |
| inventory | beat mover | "item moves on a steady beat of <gap> ms" | (row GAP - FP24 family) | | | |
| blink | Blink | "held its movement packets back and released them in one burst" | BF08/BF14 legit bunching (grader pins, patch 0013) | | | |
| boatfly | BoatFly (rig-only, -Prig) | "boat climbing in open air" (RISE_AT=5) | nb-boatfly-legit | | | |
| high | HighJump (+1.0) | "high jump rose %.2f" | nb-high-legit | | | |
| timer | Timer (x1.2, under latency) | "timer ahead %.0f ms" | nb-timer-legit | | | |
| glide | ElytraFly | "restarted gliding..." / "flying at ..." | nb-glide-legit | | | |
| air | AirJump | "air jump dy %.3f" | pb-e | | | |
| step | Step | "step dy %.3f onGround" | pb-n | | | |
| jump | powder jump > 0.70 | "jump dy %.3f" (bufJump) | GS03 legit + nb-gc05 | | | |
| microhop | (row GAP - needs r01 snow-ramp geometry) | "microhop dy %.3f" | | | | |
| antiknockback | Velocity (row GAP - needs a hitter) | "no push after three hits" | | | | |
| movement | (row GAP) | "slippery ground..." | | | | |
| levitation | (row GAP) | "levitation dy %.3f" | | | | |
| ladder | (row GAP) | "ladder dy %.3f" | | | | |

## Tally (fill last)

MESHAC __ / MEADOW __ (EVADE __ / FP __) / SPLIT __ / INCONCLUSIVE __

## Footer checks before any enforcement-adjacent reading

- [ ] GC01 verdict recorded (zero nofall SIGNAL over the 5-minute oscillation)
- [ ] GC05 verdict recorded (0 nofall flags; measured dy values attached, band untouched)
- [ ] combat rows scored only against the emit-fixed build with checklist 10b-C items 1-3 PASS
- [ ] every MEADOW-FP half has its trace lines attached (gndclaim / would-have-passed / block-id triple)
