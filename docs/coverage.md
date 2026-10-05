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
