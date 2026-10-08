# Meteor coverage matrix (rig-run, 26.2, fp4+ build, default module settings)

Written Oct 8 12:15 AM. Source: rig/h/meteor-sweep1.sh, results in rig/h/results-meteor-sweep1.csv. Every module toggled alone on a real Meteor client against the isolated rig, one scenario each. All 8 legit controls (walk, fall, water, wall, step, ladder, web, soulsand) gave 0 signals.
Meaning: CAUGHT = signals and the move was held or the player kicked. FLAGGED = signals but the hack was not stopped in the scenario time. 0 signals means MISS only when the hack visibly acted. INCONCLUSIVE = the scenario did not exercise the hack, so 0 signals says nothing. Single scenario, default settings: this is not proof of full coverage.

## Movement
| module | result | note |
|---|---|---|
| flight | CAUGHT | kicked at 4.4 s (both scenarios) |
| step | CAUGHT | kicked 6.3 s |
| spider | CAUGHT | kicked 6.7 s |
| elytra-fly | CAUGHT (Vanilla, Packet) | Packet via Glide.startFly. Pitch40/Bounce not exercised |
| elytra-boost | UNPROVEN | check coded, boost never confirmed firing on the rig |
| blink | CAUGHT for pulses 0.75 s+, MISS at 0.4 s | lag control 0 signals |
| slippy | CAUGHT (1.0) | 0.6/0.98/1.1 not run |
| jesus | CAUGHT (held) | 9 signals, no kick in window |
| fast-climb | CAUGHT (held) | 7 signals |
| no-slow | CAUGHT (held) | web and soul sand, 5 signals each |
| no-fall | FLAGGED | 3 signals, not stopped in window |
| long-jump | FLAGGED weakly | 1 signal, distance same as control, hack may not have acted |
| speed | default (vanilla-speed 5.6) = legit sprint speed, not a hack. vanilla-speed 8: 1 signal, not stopped (weak). 12: CAUGHT, held. 10: caught earlier. Strafe 1.6: 0 signals, 61 blocks, but a legit sprint-jump control covers 63.5 blocks in the same scenario, so this speed is inside legal movement and cannot be flagged without false flags (same for vanilla-speed 8 and below). Only speed ABOVE sprint-jump is detectable: 12 caught |
| timer | CAUGHT (multiplier 2: kicked 10.5 s) |
| air-jump, high-jump | INCONCLUSIVE | scenario holds jump on ground; needs a tailored scenario |
| velocity | INCONCLUSIVE | TNT scenario, player ended same place as without hack |
| entity-control | INCONCLUSIVE | boat scenario, no effect seen |
| sprint, auto-jump, gui-move, parkour, safe-walk | 0 signals, same result as legit | client conveniences (inputs the server cannot tell from a player) |
| not run | | click-tp, anti-void, auto-walk, auto-wasp, anchor, reverse-step, trident-boost |

## Player / World
| module | result | note |
|---|---|---|
| nuker | CAUGHT (held) | 8 signals |
| instant-rebreak | FLAGGED | 3 signals |
| scaffold | FLAGGED | 4 signals at 7.9 s, not kicked in window |
| air-place | FLAGGED | 6 signals |
| speed-mine | INCONCLUSIVE | 0 signals, break time near vanilla |
| break-delay, auto-tool | 0 signals | legit-looking client features |
| fast-use | INCONCLUSIVE | ate 9 of 19 items in 8 s, no control run for rate |
| reach | INCONCLUSIVE | no target hit in scenario |
| ghost-hand | CAUGHT | kicked, legit chest 0 signals |
| packet-mine | CAUGHT (blocked) | legit hold-mining 0 signals |
| not run | | the other player/world modules (auto-fish, auto-eat, liquid-filler, etc.) |

## Combat
| module | result | note |
|---|---|---|
| auto-clicker | CAUGHT | kicked 3.2 s |
| kill-aura | CAUGHT | first sweep was a bad run (default targets players only, damage came from the harness clicking). With entities=zombie and no manual clicks: 6 signals, kicked 2.5 s. Control 0 |
| criticals, hitboxes | INCONCLUSIVE | no hits landed |
| not run | | crystal-aura, anchor-aura, bed-aura, auto-totem, bow-aimbot, surround, etc. |

## Render (36) and most Misc (15)
Not server-detectable: they change only what the client draws or the chat UI. Checked by source reading only, not rig-run. Exceptions to keep an eye on: xray / wall-hack / tracers / esp (reveal hidden info; Veil layer addresses ore hiding, OFF by default and not in any release jar), packet-canceller, packet-logger, server-spoof (client-side).

## Open
Speed hacks at or below legit sprint-jump speed are not detectable by speed alone, inconclusive modules above, all of this on 26.2 only, plus Wurst unrun modules in docs/coverage.md.

## Sweep 3 (1:13 AM, combat, default settings, existing scenarios)
All 0 signals and none of them visibly acted, so all INCONCLUSIVE (not misses, not passes): velocity (floor blown out, no horizontal launch), crystal-aura and anchor-aura (target counts unchanged, needs obsidian and real target setup), auto-totem (player at 3 hp ended 0 hp, popped/died check not read), bow-aimbot, bow-spam (no hit on target), auto-weapon, auto-armor, hole-filler, auto-trap, surround (need their own scenarios), criticals (Packet mode without kill-aura, no hit landed; needs a kill-aura plus criticals scenario with a hit).
Files: rig/h/meteor-sweep3.sh, results-meteor-sweep3.csv.

## Sweep 4 (1:30 AM)
- crystal-aura: CAUGHT. Obsidian floor, zombie 5.7 blocks away, entities=zombie, range 5: it hit the zombie (1000 to 935.9 hp), 5 signals at 2.0 s, kicked 2.4 s. Control (hack off): 0 signals, no damage.
- bow-aimbot: INCONCLUSIVE, no arrow hit the zombie; 0 signals.
Files: rig/h/meteor-sweep4.sh, results-meteor-sweep4.csv.

## Sweep 5 (1:50 AM)
- kill-aura alone (zombie): 5 signals at 2.6 s, held at 2.8 s, still connected at the end (earlier run: kicked 2.5 s). Hack-off control 0 signals.
- kill-aura + criticals (mode Packet): 4 signals, first at 2.0 s, kicked 3.2 s; top category "microhop" (the criticals packet hop). CAUGHT. Criticals alone is not separable from the aura run.
- anchor-aura: still INCONCLUSIVE (target count unchanged; needs a respawn-anchor setup that the module accepts).
- windjump legit control: the wind charge did not launch the player (y stayed -60), so the control is INCONCLUSIVE. Wind-charge legit control still owed.
