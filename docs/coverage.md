# What meshac catches

Updated as tests run. Misses stay in the table.

## Real hacked client

Wurst v7.56 (official source, Minecraft 26.1.2) against the same test server twice: once with meshac, once without. Times are seconds after the hack was switched on. "Signal" is the first thing meshac noticed, "hold" is when it froze the player and put them back, "kick" is when it ended the session.

| Hack | Signal | Hold | Kick | Vanilla alone |
|---|---|---|---|---|
| Flight | 0.2 | 0.5 | 3.6 | Kicks for floating after about 4 s |
| SpeedHack | 1.5 | 3.8 | 8.8 | Nothing |
| HighJump | 0.2 | 1.2 | 4.6 | Nothing |
| Glide | 0.9 | 1.6 | 4.8 | Nothing |
| Timer | 0.9 | 1.7 | 4.8 | Nothing |
| AutoSprint (honest play) | none | | | Nothing |
| BunnyHop | none | | | Nothing |

BunnyHop is a low auto-jump at normal speed. It looks like a player holding jump, so there is nothing to catch. NoFall and Step were not exercised: the test walk is flat and never falls.

One run per hack, about 12 seconds each. Treat the timings as rough.

## Scripted bots

Packet-level bots on the same rig, before the real client was set up.

| Cheat | How it is checked | Result |
|---|---|---|
| Speed | Horizontal distance per move against a cap that depends on ground or air and on the speed attribute. Big overshoots count triple. | Caught in a few packets |
| Fly and hover | The next vertical move is predicted from gravity. Anything above it counts, big climbs count triple. | Caught |
| High jump | Leaving the ground higher than a jump or a step | Caught on the first jump |
| NoFall | Claims to be on the ground with only air below | Caught in 3 packets |
| Timer | Move packets are timestamped when they arrive on the network thread, and each is worth 50 ms. Running ahead of the clock flags it. | Caught. Extra packets after a teleport are ignored. |

## Not covered yet

- Combat (reach, aura, criticals), building (scaffold, fast place), inventory, exploits, bots and join floods.
- Blocks and items that change movement: ice, slime, honey, bubble columns, wind charges, elytra, riptide. These can cause false signals until tested.
- Meteor, other clients, Via-translated clients, lag spikes, other Minecraft versions.
- Phase, Jesus, Spider, Step, NoFall with a real client.
