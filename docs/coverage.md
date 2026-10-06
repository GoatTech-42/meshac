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
- **AutoArmor**: worked (all four armor pieces on) with zero signals. A few clicks a hand can also make; not detectable on its own.

### AutoTotem (caught, refused)

Wurst AutoTotem refills the off hand with two inventory clicks in the same tick. The server refuses a fill that lands 10 ms or less after another click, or 10 ms or less after the pop, reverts it and signals. A fill 10 to 25 ms after the pop is a strike: the first is only noted, the second is refused, the third reaches kick level. A normal-speed fill resets the streak. The first fill is refused even with the hack Delay slider at 2 or 3 (fills 182 to 287 ms after the pop), because the two clicks still arrive 0 ms apart. Controls (no hack, F key at 0.4 s and 0.12 s) gave zero signals. Measured on the 26.1.2 rig only so far.

Thresholds and why: simple visual reaction time is about 200 to 250 ms for most people and about 100 ms at the verified best (https://www.frontiersin.org/journals/human-neuroscience/articles/10.3389/fnhum.2015.00131/full, https://humanbenchmark.now/reaction-time-faq/what-is-the-fastest-human-reaction-time-ever-recorded). Anticipating the pop can beat that (https://journals.sagepub.com/doi/10.1080/17470215008416582); our own player has done a sub-25 ms swap once, never five in a row. TotemGuard, an open-source detector, checks click-time difference, standard deviation, low outliers and re-totem sequence (https://github.com/Bram1903/TotemGuard), which is where the strike idea comes from.

### ExtraElytra (caught)

Gliding only trades height for speed: v squared over 64 plus height cannot rise without a firework rocket or a hit. Wurst ExtraElytra reached about 80 blocks a second after a 44 block drop. Two samples in a row more than 10 blocks of energy above the lowest point are a signal; rockets and hits pause the check. Caught and kicked at 8 s; the vanilla glide control gave zero signals. 26.1.2 rig only so far.

### AnchorAura (caught, refused)

Place, charge and detonate happen in one tick. Clicks at a block the player is not looking at are cancelled and the block is resynced, so the blast does not happen; the player is kicked after repeats. Rig, 26.1.2 only. Clean-world rerun and macro timing variants still to do.

### Nuker (partial: stopped, not removed)

Blocks broken that the crosshair is not on are refused, and four breaks more than 40 degrees apart inside 600 ms (in 3 s) are refused with a signal. Wurst Nuker faces each block first, so only the turn-rate rule sees it. Rig: 1 signal, one block broken, not kicked. Controls (mining straight ahead, no hack) gave zero signals.

### Other unproven or partial rows (11 AM batch)

AutoEat 1 signal at 0.9 s and AutoSteal 1 signal at 4 s (both partial, no kick). AirPlace, AutoSign, FeedAura: 0 signals but the scenario did not show the hack acting, so these are unproven, not passes. AutoPotion scenario fixed, rerun pending. AutoSword and AimAssist: 0 signals, still misses.

### AirPlace (caught, stopped)

Wurst AirPlace sends a block placement against thin air and vanilla accepts it. A hand can only click a face of a block that exists, so a placement against an air block is refused and counts as a signal. Before: one block placed in the open air. After: nothing placed, 5 signals, kicked at 5.6 s. Control without the hack places nothing. Rig, 26.1.2 only.

### FeedAura (caught, stopped)

Wurst FeedAura picks a random animal every tick and feeds it, so it hits a new animal 50 ms after the last. A hand needs time to turn to the next animal. An interaction with a different entity less than 250 ms after the previous one is refused, and a second one inside three seconds is a signal. Before: all four cows fed (wheat 32 to 28), 0 signals. After: 5 signals, kicked, the player is out before the herd is fed. Control is clean. Rig, 26.1.2 only (the 1.21.11 jar has no entity-interact hook yet).

### AutoSign (unproven)

AutoSign copies the text of the first sign onto every later one. meshac refuses sign text that arrives under a second after the click (a hand needs longer to type), but the rig client never sent a sign-update packet, so the check has not been seen firing. The scenario needs the sign editor to open on the real client. Not counted as caught.

### NoClip (caught, stopped)

Wurst NoClip needs the body partly inside a block and then teleports it up to 21 blocks through the wall. Three rules: any move packet over 1.5 blocks is refused and set back when the body, slid along the path in half-block steps, overlaps a solid block (vanilla only tests where the packet ends, so a thin wall did not stop it); a move of more than 10 blocks is refused whatever lies between; and ten packets in a row with the body inside solid blocks are a signal. None of them applies while the server itself has ordered a teleport. Before the rule the hack walked 42 blocks through stone with no signal, because the jump arrived straight after suffocation damage and every movement check skipped it. After: 1 signal, the jump was refused (42 blocks became 1). Control without the hack is clean. A 3-block-thick wall scenario: 3.7-block move through it refused, control clean. Legit knockback (TNT and wind charges at the feet, wall beside) stays at 0 signals. Rig, 26.1.2 only.

### CreativeFlight (caught)

Double-tap jump scenario: 4 signals, kicked. Control clean.

### BunnyHop (not server-detectable)

Travels the same distance as a player holding jump while walking (35.7 against 35.9 blocks in 8 s).

### AutoFish (not caught yet)

Runs fine: 2 fish in 45 s, zero signals. The tell is the time from bite to reel; a hook mixin could measure it. Not built.

### AutoSwitch (caught)

More than 15 hotbar slot changes in one second. Signal at 0.1 s, kicked at 2.3 s in the rig.

### AutoDrop (not detectable)

Drops items with the normal drop action, which the server cannot tell apart from a player doing it.


### Still unproven or unrun

AutoPotion (never healed), FeedAura (no cow fed), AutoSign (scenario never placed a sign), NoClip, CreativeFlight, Restock, AimAssist, AutoSword, and the modules that have not been run yet. Other Minecraft versions: all four jars build and boot with meshac loaded, but no client has joined on 1.21.11, 26.2 or 26.3.

## Client-only modules

Read in the Wurst v7.56 source: none of these calls anything that sends a packet or touches the connection (searched each file for packet sends, the connection, game mode interaction, chat and command sends). They only change what the player sees. The server has nothing to detect, so they are listed as not server-detectable. Where the module reveals hidden things (ESP, radar, tracers, cave and base finders, Freecam, Search, TrueSight), the answer is on the server side by hiding the information, which is a separate piece of work.

AntiBlind, AntiWobble, BarrierESP, BaseFinder, CameraDistance, CameraNoClip, CaveFinder, ChestESP, Freecam, Fullbright, HealthTags, ItemESP, LSD, MobESP, MobSpawnESP, NameProtect, NameTags, NewChunks, NoBackground, NoFireOverlay, NoFog, NoHurtcam, NoOverlay, NoPumpkin, NoShieldOverlay, NoVignette, NoWeather, OpenWaterESP, PlayerESP, PortalESP, ProphuntESP, Radar, RainbowUI, RemoteView, Search, Trajectories, TrueSight.

Still to run with a scenario: AnchorAura, CrystalAura, AutoFish, AutoLibrarian, ExtraElytra, AntiSpam, AutoComplete, FancyChat, ForceOP, InfiniChat, MassTPA.
