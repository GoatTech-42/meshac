# Veil rig quickstart: one Java client and one Bedrock client

Rig server only. Never ilhsmc. Two real clients: a Java one (Wurst or Meteor if you want the hack side) and a Bedrock one through Geyser. The scenario names (X1, E1 and so on) are in CLIENT-TEST-SPEC.md. This list only says what each client should see for each setting. Tick a box only after you saw it.

Check the log first. After every start, `/mesh reload` or `/mesh veil ...` the log should say `veil: xray=... esp=... bedrockViewersCulled=...`. If a config key you typed shows "is ignored", fix the spelling and reload before going on.

Use the flat-stone arena from the spec: a buried ore field, a cave pocket, a buried chest, and a second player behind a wall.

## 0. Everything off (`veilXray` false, `veilEsp` false)
- [ ] Java client: with xray on, sees every buried ore and the buried chest. Sees the player behind the wall with ESP on.
- [ ] Bedrock client: normal view, no hack client needed. Buried chest and ore are real. Player behind the wall is listed.
- [ ] Log line says xray=off esp=off.

## 1. Anti-xray only (`veilXray` true, `veilEsp` false)
Relog both clients, or walk far enough to reload the chunks.
- [ ] Java, xray on: only exposed ores show. Buried chest not shown. Some extra fake ore in stone.
- [ ] Java, no hack: dig into a buried ore. It shows when the wall block breaks.
- [ ] Bedrock: the same fake ore and hidden chest apply, because chunks are shared. Look for: stone where a chest should be, ore that is missing next to an open cave, ore that flickers or stays after mining, a chest that cannot be opened after digging to it.
- [ ] Bedrock: mine the buried ore like a normal player. It must be real and drop normally.
- [ ] Both: the player behind the wall is still visible (ESP is off).

## 2. Anti-ESP only (`veilXray` false, `veilEsp` true, `veilBedrock` false)
- [ ] Java, ESP or tracers on: the player behind the wall at 20 blocks is gone within a quarter second. In the open, always there.
- [ ] Java, no hack: walk to a corner with the other player just behind it. They must be visible before or when they come around.
- [ ] Bedrock viewer: sees the player behind the wall. This is the fail-open default, so seeing everything is correct here.
- [ ] Bedrock player as the target, Java viewer behind a wall: the Bedrock player is hidden from the Java viewer, then shows when the wall is gone. Check the Bedrock player does not appear stuck or duplicated for others.
- [ ] Spectator or creative Java viewer: sees everyone.

## 3. Both on, Bedrock culled (`veilBedrock` true)
Only after section 2 passed.
- [ ] Log says bedrockViewersCulled=yes.
- [ ] Bedrock viewer: the player behind the wall disappears and comes back at the corner like for Java. Watch for entities that do not come back, a frozen player model, or the name tag staying in place.
- [ ] Bedrock viewer: teleport, respawn and change dimension. Everyone nearby shows up within a couple of seconds.
- [ ] Any ghost, stuck entity or missing player on Bedrock: set `veilBedrock` back to false, `/mesh reload`, and write down what you saw. That is a blocker for turning it on.

## 4. Switch off live
- [ ] `/mesh veil off` while both clients stand still behind walls. Hidden players appear within a tick on Java, and on Bedrock if section 3 was on.
- [ ] `/mesh veil` prints the stale plan rebuild count. It should be a small number that does not climb while you stand still.
- [ ] Chunks received before the switch keep their fake ore until reloaded. Relog both clients to confirm the real ore shows again.

## 5. Config checks
- [ ] Put `"veilESP": true` in the file and reload: a warning names the key and the right spelling, and the veil stays off.
- [ ] Remove all veil keys: the line says xray=off esp=off.
- [ ] Break the JSON on purpose and reload: the log says the config could not be read and the veil stays as it was. Fix it afterwards.

## 6. Cross-check with the mining check (veil on, enforcement in monitor mode)
The veil does not add heat or issue bans itself, but it changes what a client sees while digging, and the mining check judges digging by the server's block. Run these with `/mesh monitor on` and watch the flags and heat (`/mesh status <player>`, `/mesh watch`). The signal to watch is the mining one: "stopped digging after N ticks, progress P" (`Mining.digStop`).
The reason: with anti-xray the client times its dig by the block it sees, the server by the real block. A fake ore is harder than the base block under it, so the client takes longer and nothing fires. Vanilla hardness, read from the game jars for 1.21.11, 26.1.2, 26.2 and 26.3 (all four agree): stone 1.5, coal, iron, gold, diamond and quartz ore 3.0, netherrack 0.4, deepslate 3.0, deepslate ores 4.5, end stone 3.0, chest 2.5, spawner 5.0, ancient debris 30.
A hidden real block looks like the base block to the client, so the client can stop sooner than the server expects. The check flags a stop with less than half the real progress done, so the veil only masks a block when the shown block is at least half as hard (HideRule). Ancient debris, ender chests, spawners in stone and ores in the nether are therefore left unmasked. Rows 6.3 and 6.4 still apply to everything that is masked.
- [ ] 6.0 Ancient debris, a spawner in stone and a nether ore show up in an x-ray client, and digging them raises no mining signal. Deepslate spawners, chests and ores are hidden.
- [ ] 6.1 Dig each kind of fake ore (coal, iron, gold, quartz in the nether, the deepslate versions) to the end with a matching pick. Java and Bedrock. No mining signal, heat stays 0.
- [ ] 6.2 Dig a fake ore and let go after 3 ticks on purpose. The signal should be the same as letting go after 3 ticks on plain stone with the veil off. Compare the two runs line by line.
- [ ] 6.3 Tunnel toward a buried real ore, break the block next to it, and carry on into the ore at once. The ore must appear in the same tick the neighbour breaks. No mining signal, same result as with the veil off.
- [ ] 6.4 While you dig a fake ore, have a second player (or TNT) remove a block next to it. The server now resends real stone to you in the middle of the dig. The client restarts the dig, the server may see a stop. Heat must stay 0 and nothing may be flagged as early stopping.
- [ ] 6.5 Switch `/mesh veil xray false` in the middle of a dig on a fake ore, and `true` again. Same expectation.
- [ ] 6.6 Same digs with 100 ms and 300 ms of added ping.
- [ ] 6.7 Fast mining with an efficiency pick and haste on stone beside fake ore: no mining signal, compared with the veil off.
- [ ] 6.8 Quick movement and combat for 10 minutes with the veil on (strafing, bridging, a few PvP rounds, elytra, a boat). Heat from the movement and combat checks must match the same session with the veil off, within noise. Write both numbers down.
If any row flags or adds heat: leave the veil off in production, copy the lines and send us the row number.

## Write down
Client and version, Geyser version, which section, what you saw against what the box says. Screenshots of anything wrong.
