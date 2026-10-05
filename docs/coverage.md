# Coverage matrix
Honest status. "Bot" = scripted mineflayer packet sender on the isolated rig (MC 26.1.2). Real hacked clients (Meteor, Wurst) come later.

| Cheat | Check | Detected (bot) | Speed to flag | False positives seen | Known weak spots |
|---|---|---|---|---|---|
| Speed (packet) | horizontal cap scaled by speed attribute, buffer 6 | yes (8-14 signals) | ~6 packets | 0 in 15 s sprint-jump legit run | cap is generous (0.75 b/t), so small speedups pass; ice, slime, soul speed, vehicles not modeled |
| Fly (hover/ascend) | vanilla gravity prediction, buffer 6 | yes (7 signals) | ~6 packets | 0 | blocks that change gravity (cobweb, honey, bubble columns, powder snow) are not modeled yet |
| High jump | ground dy > 0.62 | yes (18 signals, logged as fly or jump) | 1-2 packets | 0 | step-assist under 0.62 passes |
| NoFall (ground spoof) | claimed onGround with only air below, buffer 3 | yes (bot) | 3 packets | not measured separately | boats, scaffolding edge cases, chunk borders untested |
| Timer | packet-time balance, stamped on the network thread, flag at 450 ms ahead | yes (bot at 2.5x) | ~20 packets | fixed: teleport confirm packets were counted as speedup, now skipped in grace. 20 Hz bots: 0 to 1 stray signals in a 10 s run, still being measured | lag bursts bank at most 300 ms |
| Jesus, Phase, Reach, KillAura, Scaffold, Timer, Velocity, XRay, ... | not built | no | - | - | milestone 3+ |

Open issue: the legit bot sometimes hovers in the air before chunks load and trips the fly check (4 signals in one run, 0 in others). A real client behaves differently; to be re-tested with a real client.

Not yet tested: real hacked clients, other latencies, lag spikes, 1.21.11, Via-translated clients.

## Real client results (Wurst v7.56 for MC 26.1.2, built from the official repo, run headless on the isolated rig)
| Date | Hack | Signals | Outcome |
|---|---|---|---|
| 2026-10-05 | Flight | 9 (8 speed, 1 fly) in the first seconds | meshac signalled and set back, then vanilla kicked the client ("Flying is not enabled"). meshac's setback alone did not stop the flight yet. |
| pending | speedhack, nofall, timer, highjump, step, jesus, killaura | - | needs a fresh client session per hack (the flight run ended the session) |

## Beats vanilla (real Wurst v7.56 client, MC 26.1.2, flat world, same server with and without meshac)
Times are seconds from toggling the hack. "signal" = first meshac signal, "hold" = movement frozen and player put back, "kick" = meshac ends the session.

| Hack | meshac signal | meshac hold | meshac kick | Vanilla alone | Beats vanilla |
|---|---|---|---|---|---|
| Flight | 0.2 s | 0.5 s | 3.6 s | kicked for floating too long, after about 4 s airborne ("Flying is not enabled") | yes: held in half a second, vanilla only reacts after floating |
| SpeedHack | 1.5 s | 3.8 s | 8.8 s | nothing | yes |
| HighJump | 0.2 s | 1.2 s | 4.6 s | nothing | yes |
| Glide | 0.9 s | 1.6 s | 4.8 s | nothing | yes |
| Timer | 0.9 s | 1.7 s | 4.8 s | nothing | yes (caught mostly as hover from duplicate packets, plus the timer balance) |
| AutoSprint (control, legit) | none | - | - | nothing | no false positive |
| BunnyHop | none | - | - | nothing | n/a: low autojump with vanilla-speed movement, same as a player holding jump |
| NoFall, Step | not exercised: the flat test walk never falls or climbs | | | | pending |

Caveats: one run per hack, 12 s of movement each. Meteor not tested yet (needs a 26.2 rig). Special-block cases (ice, slime, bubble columns, wind charges, elytra) are not modeled and may false-positive; they need their own tests before any real server uses meshac.
Raw outputs: batch-v3-meshac.out, batch-v3-vanilla.out (kept on the build box).
