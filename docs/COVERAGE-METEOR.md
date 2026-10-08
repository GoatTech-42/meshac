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
| speed | MISS at default settings | 0 signals, covered 50.9 blocks vs 35.3 control in 8 s. Needs a fix (earlier vanilla-speed 10 was caught) |
| air-jump, high-jump | INCONCLUSIVE | scenario holds jump on ground; needs a tailored scenario |
| velocity | INCONCLUSIVE | TNT scenario, player ended same place as without hack |
| timer | INCONCLUSIVE | default multiplier is 1.0, no effect; needs a setting |
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
| kill-aura | MISS at default settings | 0 signals while hitting a tank zombie (55 damage dealt). Needs the older aura scenarios rerun to confirm |
| criticals, hitboxes | INCONCLUSIVE | no hits landed |
| not run | | crystal-aura, anchor-aura, bed-aura, auto-totem, bow-aimbot, surround, etc. |

## Render (36) and most Misc (15)
Not server-detectable: they change only what the client draws or the chat UI. Checked by source reading only, not rig-run. Exceptions to keep an eye on: xray / wall-hack / tracers / esp (reveal hidden info; Veil layer addresses ore hiding, OFF by default and not in any release jar), packet-canceller, packet-logger, server-spoof (client-side).

## Open
speed default MISS, kill-aura default MISS, inconclusive modules above, all of this on 26.2 only, plus Wurst unrun modules in docs/coverage.md.
