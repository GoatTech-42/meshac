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

## Added in the second pass (Wurst 26.1.2, one run each)

Same rig and same reading of the columns as above. Every hack here was also run once with nothing switched on, and the legit suite (22 normal and skilled play scenarios) stayed at zero flags afterwards. Only the 26.1.2 jar has been through a real client so far.

| Hack | What happened | Check |
|---|---|---|
| BoatFly | Kicked in about 5 s | A boat that rises 5 packets in a row in open air is refused |
| AttributeSwap | Held at 10.8 s | Swap to another item type and hit inside 120 ms, 3 times in 10 s |
| AntiKnockback | Caught | Hit, then under 0.3 block of push in 450 ms with open space, 3 times in a row. Real pushes measured 0.7 to 1.1 blocks |
| SpeedNuker, Kaboom | Kicked in 2.2 s (Kaboom partly, through-block rule) | Block broken through another block, or more than 8 breaks a second |
| AutoBuild, InstaBuild | Kicked in 4 to 6 s | Click on a block that is not in the look ray, or more than 16 uses a second |
| ScaffoldWalk | Held at 5.9 s | Ten placements in a row under the feet at over 4 blocks a second |
| FastPlace | Kicked in 6.1 s | Same use rate rule, run with the mouse held |
| Throw | Kicked in 3.2 s | Several uses in one tick |
| AutoSoup | Kicked in 0.9 s | Same use rate rule |
| AutoFarm, Tillaura, BonemealAura | Kicked | Clicks on blocks it is not looking at, block break rate |
| TreeBot | Kicked | Block break rate |
| FastBreak | No speedup got through (4 blocks in 10 s with or without it) | Vanilla needs 70 percent break progress, and the 8 a second cap sits on top |
| ItemGenerator, TrollPotion, KillPotion | 0 items gained in survival | The server refuses creative inventory packets from survival players. Not traced at packet level |

### Partly caught

- **BowAimbot**: a view jump of over 25 degrees in the 1.5 s before release, 3 times in 30 s, gives a signal. In six shots that was 2 signals and a setback, no hold, and the arrows still landed. Not counted as caught.
- **AutoSteal**: moves a chest at a steady 100 ms beat. The steady-beat rule gave 1 signal on 12 stacks and a setback. Not counted as caught.
- **AutoEat**: 1 signal from the use rate, harmless hack.

### Not detectable from the server

- **AutoLeave**: the client closes its own connection.
- **AutoRespawn, AutoReconnect**: client screens.
- **AutoTotem, AutoArmor**: worked (totem in the off hand, all four armor pieces on) with zero signals. They are a few clicks a hand can also make. A reaction-time check on totems is not built.

### Still unproven or unrun

AutoPotion (never healed), FeedAura (no cow fed), AutoSign (scenario never placed a sign), NoClip, CreativeFlight, AutoDrop, AutoSwitch, Restock, AimAssist, AutoSword, and the modules that have not been run yet. Other Minecraft versions: all four jars build and boot with meshac loaded, but no client has joined on 1.21.11, 26.2 or 26.3.
