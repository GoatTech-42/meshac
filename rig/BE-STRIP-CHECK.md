# Rig check: hidden chests and spawners in the chunk packet

Rig server only. Never ilhsmc. Source only so far: nothing in this change has been compiled or run. Branch floodgate-linked-skip-9g-20261016, commit "veil: drop hidden chest/spawner entries ..." and the counters commit after it.

What changed: a buried chest, ender chest or spawner that the veil turns into stone no longer has its block entity entry (position and type) in the chunk packet. A visible one keeps it. Reveal still sends the block update and then the block entity update.

Set `veilXray` true, `veilEsp` false, `veilBedrock` false, then `/mesh reload`. The log must say xray=on. Run everything below with that and watch `/mesh veil`: it now ends its first line with `chest/spawner entries stripped N, size mismatches M`. Write down both numbers before each step.

## Arena
Flat stone area, at least 20 blocks of solid stone in every direction around the test spot, Y well below any cave. Use the operator console, not a client, to build it. Put these in the same chunk (same x/z chunk coordinates):
1. A buried chest at least 3 blocks inside the stone: `/setblock <x> <y> <z> minecraft:chest`
2. A buried ender chest, same way, 3+ blocks from the chest.
3. A buried spawner, same way: `/setblock <x> <y> <z> minecraft:spawner`
4. A visible chest: put it on the surface of an open pocket (carve a 3x3x3 room with `/fill ... air` and place a chest on the floor).
5. A buried chest in a different chunk, for step 7.
Write the six coordinates down. A chest in a double pair is a separate case, see step 8.

## 1. Strip counts (no client needed)
- [ ] Log out everyone, restart the chunk by walking a Java client to the arena from 200 blocks away so the chunk is sent fresh.
- [ ] `/mesh veil`: stripped went up by 3 for the first chunk (chest, ender chest, spawner) and not by 4. The visible chest must not be counted. Size mismatches stays at its old value. If the counter went up by 4, the visible chest was stripped: stop and report.
- [ ] Walk away and come back: the chunk is sent again and the counter goes up again. That is expected.

## 2. Java client, no hack
- [ ] Walk to the visible chest and open it. It works, has its contents, no flicker.
- [ ] Tunnel to the buried chest with a pickaxe. As the last stone block breaks the chest appears. Open it. Contents are there.
- [ ] Same for the ender chest and the spawner. The spawner shows its spinning mob and spawns on the normal rules.
- [ ] Break a buried chest from the side so the reveal happens by a neighbour change. It must show up as a chest and open.

## 3. Java client with a hack client (Wurst or Meteor storage or chest ESP)
- [ ] Chest ESP shows the visible chest only. The buried chest, ender chest and spawner are not boxed.
- [ ] After the tunnel opens a side of the buried chest, the ESP shows it.

## 4. Java client log check
- [ ] Client log has no errors about block entities, positions or "invalid block entity" when loading the arena chunk.

## 5. Bedrock through Geyser (if a client is available)
Do this twice: once with an unlinked Bedrock account (name starts with the Floodgate prefix, a dot by default) and once with a linked one. Note which is which.
- [ ] Enter the arena. No Geyser errors in the server log about block entities or chunks.
- [ ] The visible chest opens and has its contents.
- [ ] Dig to the buried chest like a normal player. It appears when exposed and opens.
- [ ] Spawner: same, with the mob inside.
- [ ] Geyser kept its chunk cache straight: break the block next to the buried chest, then break the chest itself. Nothing stays behind as a ghost block.
- [ ] Hook lines: the server log has `bedrock: floodgate api found ...` and, for the linked account only, `bedrock: Floodgate player with a normal UUID (linked account) treated as Bedrock`. The unlinked account makes no such line.
- [ ] For both accounts `/mesh status <name>` shows no combat, interact, inventory or mining signals after five minutes in the arena. Movement signals can appear (Bedrock keeps the movement checks).

## 6. Java and Bedrock names
- [ ] A Java client named `.Test` (offline mode rig only) is checked like any Java player: its dot name gets no skip. `/mesh status .Test` shows normal signals for a deliberate fast-click or similar.
- [ ] Exempting a Bedrock player by name: for the unlinked account put `.<name>` (prefix, underscores for spaces) in `exemptPlayers`, `/mesh reload`. It is exempt, the bare name without the prefix does nothing. For the linked account the server shows the Java account name with no prefix: write that one. Check the player list for what the server actually shows before you write either.

## 7. Second chunk and reload
- [ ] The buried chest in the other chunk is stripped when that chunk is sent (counter +1) and works on reveal.
- [ ] `/mesh reload` with veilXray false: walk away and back. The counter does not rise and the buried chest is back in the packet (a hack client can see it again). That confirms the switch.
- [ ] Turn veilXray on again, reload, relog: stripping comes back.

## 8. Edge cases to look at, not to pass or fail
- [ ] A double chest with one half buried and one half exposed. Look at the visible half from both sides, then expose the other. Write down what Java and Bedrock show.
- [ ] Break a visible chest while a Bedrock client watches. No leftover chest model.
- [ ] Join a Bedrock client while a Floodgate reload is happening if the rig can reload Floodgate. The player should end up skipped within a few seconds.

## Report back
Counter values before and after each step, any Geyser or client error text, a screenshot of anything odd in step 8, and the Floodgate and Geyser versions. If size mismatches is ever above zero, say when it happened.

## Stop conditions
A buried chest that cannot be opened after exposure, a chest that appears empty, a spawner without its mob, any client disconnect or crash, or a Geyser error naming a block entity. Turn veilXray off and send the log.

## Candidate9p integration update
The constructor strip now uses only per-packet masks recorded from successful private copies and completed section writes, not Veil.mods recomputation. A null/failed copy records no mask. The filtered list is built privately and its reference is swapped once using a Mutable shadow; a filter failure leaves the original list intact. Native checks must still confirm Mixin application, ordering and client-visible reveal behavior.
