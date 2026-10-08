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
| elytra-boost | CAUGHT (below 45 b/s) | rocket-less energy check, kicked 10.9 s; controls clean (rockets, dives, wind charge, lag proxy, crossbow). Above 45 b/s not covered |
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
| velocity | CAUGHT for melee knockback (5 signals, kicked 11.6 s, antiknockback); MISS for explosion knockback | zombie hits with knockback 0: caught, legit control 0 signals (pushed 12 blocks). TNT with explosions 0: player did not move, 0 signals (the check excludes explosions on purpose, to protect anchor/bed/wind legit play) |
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

## Wind-charge legit control (2:10 AM, rig 26.2, no hack)
Real client throws wind charges at its feet (pitch 90, space held, 32 charges, 12 s). Sampled y rose from -60 to -48.5 (12 blocks up) with several launches, 0 signals, connected. Earlier summon-based attempts did not move the player and were dropped. Files: rig/h/meteor-sweep7.sh.

## Other versions (2:32 AM)
The dist jars for 1.21.11, 26.1 and 26.3 each load on a dedicated server (log "meshac loaded", server Done, no meshac errors; the only errors are offline Mojang auth lookups). NOT rig-tested with a client: the Meteor build here is 26.2 only, so detection results above hold for 26.2 only. Mixin targets compile on all 4 versions.

## More legit controls (3:08 AM, rig 26.2, no hack, real client)
- PacketMine controls: hold-mining obsidian with a diamond pickaxe (broke), and the same with Haste II (broke): 0 signals, connected. Water and lag mining not run.
- GhostHand controls: right-click a chest behind glass (twice) and behind a bottom oak slab: 0 signals, connected. The scenario did not check that the chest opened, so it shows no false flag only. Fence not run.
Files: rig/h/meteor-sweep8.sh, results-meteor-controls2.csv.

## Sweep 15 (5:17 AM): BowAimbot, AnchorAura
- bow-aimbot: MISS. Zombie 12 blocks away, 6 drawn shots: legit manual bow did 45 damage, Bow Aimbot 49 damage; 0 signals both. It aims like a good player; nothing in the shot packets separates it. Known miss unless a rotation-snap check is added (risk of false flags on legit flicks).
- anchor-aura: NOT TESTABLE on this rig. The module only targets real players (target-priority setting, no entity filter) and the rig has one client; the fake-player trick gave no placement (anchors 16 to 16). Needs a second real player. Bed-aura only works in the Nether/End and was not set up.
- velocity: caught for melee knockback, known miss for explosion knockback (decided: leave, protects legit explosion play).

## Anchor / Bed Aura (sweep 16): not tested
Meteor AnchorAura only targets real players, so a second client is needed as the victim. Two Meteor containers on one Gradle home collide on the cache lock; a copied cache (gh2) hit the same lock error. No anchor-aura result either way. Treat as untested, not caught. Bed Aura needs Nether/End setup and was not attempted. BowAimbot stays a known miss: a rotation-snap check would risk flagging legitimate bow flicks (legit 45 dmg vs aimbot 49 dmg, 0 signals).

## Sweep 17/18: AutoTotem, Reach, AirJump
- AutoTotem: CAUGHT (1 signal, weight 5.2, inventory check). Controls: human-speed F-key totem swaps (0.4 s and 0.12 s after pop, 3 runs) gave 0 signals.
- Reach (extra-entity-reach 3, target 5 blocks out): CAUGHT, 4 combat signals, kicked. Caveat: the target health did not change (server rejected the hit), so the flag came from the attack attempt. Legit reach control: ordinary melee runs at normal range gave 0 signals earlier.
- AirJump: INCONCLUSIVE, the airjump scenario is not defined in scenarios.sh (only in meteor-air.sh), no data.

## Sweep 19: AirJump, HighJump
- AirJump (space tapped mid-air): CAUGHT, 7 air signals (weight 6.7 and 8.6 peaks); control with the hack off: 0 signals. Player took 2 damage (setbacks).
- HighJump (jump-multiplier 4, walk+jump): CAUGHT, 6 jump signals; control: 0 signals. Player ended at 0 health in the run (fall or setback damage), noted.
