# Rig cells: the veil strips only what it should, and the egg rule is not tied to monitor mode

For the rig agent. Rig server only, never ilhsmc. Nothing here has been compiled or run. Config is `config/meshac.json`, then `/mesh reload`. Read `/mesh veil` (first line carries the strip counters) and the log line `veil: xray=... esp=...` after every reload. With `veilEnabled` false that line now ends with `(veilEnabled is false: the veil is off whatever veilXray and veilEsp say)`.

Cell format: set up, do, expect. Run each cell with monitor on (`/mesh monitor on`) and again with it off (`/mesh monitor off`) unless a cell says otherwise. The result must be the same both ways, that is the independence check.

## A. Arena with ground truth
Flat stone, 20+ blocks solid around the test chunk, all in one chunk, at a height where the rock is plain stone (Y above 0 in the overworld). Build from the console. Write down the counts. What gets masked is decided by HideRule (see its comment): a block is only shown as stone when stone's dig time is at least half of the real block's, so the mining check cannot trip. Vanilla hardness: stone 1.5, deepslate 3.0, ores 3.0 (deepslate ores 4.5), chest and trapped chest 2.5, spawner 5.0, ender chest 22.5, ancient debris 30.
- Masked in stone, buried 3+ blocks in: 2 chests, 1 trapped chest, 3 coal ore, 2 iron ore, 1 diamond ore.
- Hidden-list blocks that must NOT be masked in stone, on purpose: 1 ender chest, 1 spawner. They stay visible to xray. Their block entity entries must stay in the packet.
- Must stay as sent: a chest on the floor of a lit 3x3x3 room, an ore exposed on the wall of that room, an ore touching a cave, and these buried 3+ blocks in stone (block entities that are not in the hidden set): barrel, furnace, hopper, sign, shulker box, bed, banner, lectern, brewing stand, enchanting table, jukebox, skull.
- Counter expectation in plain stone: after a fresh chunk send the `chest/spawner entries stripped` number rises by exactly 3 (2 chests and the trapped chest). Ores add nothing there. The ender chest, the spawner and everything in the second list add nothing.
- Deepslate variant (Y below 0, shown block is deepslate, hardness 3.0): the spawner is masked too, so the same arena gives +4. The ender chest still adds nothing.

## B. veilEnabled
1. `veilEnabled` unset, `veilXray` true: log says xray=on. Cell A passes (hidden stays hidden, counter +3).
2. `veilEnabled` false, `veilXray` true, `veilEsp` true, reload: log says xray=off esp=off plus the veilEnabled note. Walk away 200 blocks, come back: the counter does not rise, a Java hack client sees the buried chests and ore again, no fake ore.
3. `veilEnabled` true again, reload, relog: hidden again, counter +3.
4. `veilEnabled` false, then run `/mesh veil xray true`: expect "Anti-xray off (veilEnabled is false...)"; the veil must do nothing (the plan builder checks the master switch). Look for any fake ore or hidden chest. If something is hidden, that is a bug, report it. A message saying "Anti-xray on" with the master false is a failure. Also try `/mesh veil esp true`: it must refuse activation with the same master-key explanation.
5. Existing chunks after turning the veil off: a client that was standing in the arena keeps the veiled chunks until it reloads them. Record what the client sees (stone where the chest is) before and after relog. Expected: stale until relog, then correct. Mine the stone where a hidden chest was without relogging and write down what happens (the server breaks the chest). This is why the README tells people to relog.
6. Monitor independence: repeat 1 to 3 with monitor on and off. Same results.
7. `veilEnabled` false with `veilBedrock` true and a Bedrock viewer: nothing hidden from them, nothing stripped.

## C. Only the right things are stripped
8. Cell A fresh chunk send: counter +3 exactly. Any other number: write down the arena layout and the number.
9. Hack client, chest ESP and ore ESP: shows the visible chest, the exposed ores and all of the second list (they are not hidden, so ESP boxes them: this proves the veil did not touch them). Shows none of the masked list (2 chests, trapped chest, ores) and does show the ender chest and spawner, which are not masked in stone.
10. Open each item in the second list (barrel, furnace, hopper, lectern, jukebox, shulker box, brewing stand, enchanting table, bed, sign, banner, skull) after digging to it. All behave like vanilla, nothing is missing.
11. Reveal both ways: dig to a masked chest from the side (neighbour reveal) and from above. It appears, opens, has its contents. The ender chest and spawner need no reveal in stone (they were never hidden): check they open and the mob shows. In the deepslate variant do the reveal for the spawner too.
12. Size mismatches in `/mesh veil` stays 0 through the whole run. If it rises, write down when.
13. `/mesh status <name>` for the digging player: no signals from mining a hidden ore. With monitor off too.

## D. Bedrock through Geyser (if a client is available)
Unlinked account and linked account, once each.
14. Cells 8, 11 and 12 with a Bedrock client: same counter, chest and spawner appear and work, no Geyser errors about block entities.
15. `veilBedrock` false: Bedrock viewer sees every player at all times (anti-esp does not apply to them). `veilBedrock` true and `veilEsp` true: a player behind a wall disappears and comes back at the corner. Same for linked and unlinked.
16. Same chunk, three clients (Java, unlinked Bedrock, linked Bedrock): all three see the same hidden and unhidden blocks. Any difference is a bug.

## E. enderchestEggRule
Make a creative test player and a survival one. Give each: a dragon egg, a shulker box holding an egg, a bundle holding an egg, a bundle holding a shulker box holding an egg, a plain shulker box, a plain bundle.
17. Default (key unset): in an ender chest, none of the four egg holders can be placed. Try left click, right click, shift-click from the inventory, number key hotkey, and drag. The plain shulker box and plain bundle go in. After every try, the item is back where it came from, nothing is lost or doubled, and relog shows the same inventory.
18. Normal chest, barrel and shulker box accept the egg as before.
19. An egg that is already in an ender chest (put it there with the rule off) stays, and can be taken out with the rule on.
20. `/mesh status <name>` after all those tries: no signals, no heat, no case. The refusal is not a detector hit.
21. Monitor on and monitor off: identical refusals.
22. `enderchestEggRule` false, reload: all four egg holders go into the ender chest. True again, reload: refused again, no restart.
23. Bedrock client, same tries as 17 (drag and tap to move). Geyser has no knowledge of this rule, so the Bedrock client may briefly show the item in the ender chest before the server undoes it. Pass: the item ends up back in the inventory or cursor, no loss, no duplicate, relog matches. Write down whether and how long the flicker shows. Do it for the unlinked and linked account.
24. Creative mode Bedrock or Java: the same refusal. Note whether the creative menu "pick" path lets an egg in.

## Report
Per cell: pass or fail, counter values, anything odd. For D and 23 add the Geyser build and Floodgate version. Stop conditions: any lost or duplicated egg or shulker contents, a chest that cannot be opened after reveal, a client disconnect, or a veil that stays on after `veilEnabled` false and a relog.
