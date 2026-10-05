# Coverage matrix
Honest status. "Bot" = scripted mineflayer packet sender on the isolated rig (MC 26.1.2). Real hacked clients (Meteor, Wurst) come later.

| Cheat | Check | Detected (bot) | Speed to flag | False positives seen | Known weak spots |
|---|---|---|---|---|---|
| Speed (packet) | horizontal cap scaled by speed attribute, buffer 6 | yes (8-14 flags) | ~6 packets | 0 in 15 s sprint-jump legit run | cap is generous (0.75 b/t), so small speedups pass; ice, slime, soul speed, vehicles not modeled |
| Fly (hover/ascend) | vanilla gravity prediction, buffer 6 | yes (7 flags) | ~6 packets | 0 | blocks that change gravity (cobweb, honey, bubble columns, powder snow) are not modeled yet |
| High jump | ground dy > 0.62 | yes (18 flags, logged as fly or jump) | 1-2 packets | 0 | step-assist under 0.62 passes |
| NoFall (ground spoof) | claimed onGround with only air below, buffer 3 | yes (bot) | 3 packets | not measured separately | boats, scaffolding edge cases, chunk borders untested |
| Timer | packet-time balance, stamped on the network thread, flag at 450 ms ahead | yes (bot at 2.5x) | ~20 packets | OPEN: 20 Hz scripted bots also trip it under a 1-CPU cap; needs a drift-free test bot before trusting | lag bursts bank at most 300 ms |
| Jesus, Phase, Reach, KillAura, Scaffold, Timer, Velocity, XRay, ... | not built | no | - | - | milestone 3+ |

Open issue: the legit bot sometimes hovers in the air before chunks load and trips the fly check (4 flags in one run, 0 in others). A real client behaves differently; to be re-tested with a real client.

Not yet tested: real hacked clients, other latencies, lag spikes, 1.21.11, Via-translated clients.
