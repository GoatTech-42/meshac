# Chunkveil edge cases

What was read in the adapter, what each case does today, and what is tested. Pure tests are in `sim/VeilEdge2Test.java`, `sim/VeilEdgeTest.java` and `sim/ViewerPolicyTest.java`. Game behaviour still needs the rig (RIG-QUICKSTART.md).

## Chunk borders
- A neighbour in an unloaded chunk counts as see-through, so an ore on the border is shown. It fails open. Tested.
- A change at a border block invalidates the cached plan of the chunk next door and re-sends to the players tracking that chunk. This was already right.
- Known leak, designed but not wired (docs/veil/BORDER-LEAKS.md): if a block next to a border is changed without an update (no-update setblock, WorldEdit), the cached plan of the other chunk is not told. The freshness check only compares a chunk's own blocks. Result: a real ore stays hidden although it is now next to air, until that chunk is planned again. The player-visible effect is a missing ore, not an extra one. Rare.
- A chunk planned while its neighbour was unloaded shows its border ore for good, since nothing re-plans it when the neighbour loads. A tiny leak, in the safe direction.

## Piston and explosion updates
- Pistons, explosions, players and fluids all go through setBlock, which calls our reveal step. That path is right for blocks that become see-through.
- Found: an exposed FAKE ore was never resent. Reveal only fixed real hidden ores. A player who dug next to a fake would keep seeing an ore that breaks into stone, and a cheater learns which ones are fake. Fixed: reveal now also resends stone positions that carry a fake roll (`XrayCore.resendOnReveal`, same roll as the plan, tested for 5000 positions). Resending real stone where there was no fake changes nothing on the client.
- Found: every setBlock invalidated 32 cache entries and scanned 6 neighbours, even for crops growing, lamps, doors and redstone. A new filter (`ChangeFilter`) skips all of it unless the old or new block is a hidden-list block or stone, or the see-through state changed. Pulsing machines no longer cost this on every pulse.
- Not covered: block changes sent without the client-update flag do not reach reveal at all. Vanilla sends those only for internal updates, so this is expected.

## Respawn, dimension change, teleport
- The no-cull window after join was only started on join. Respawn and dimension change did not start one although the comments say they should. Added: the tick loop now starts the same 40 tick window the first time it sees a viewer id (join, or the new entity after a respawn) and whenever a viewer is in a different level than last time (`ViewerEpoch`). No Fabric event is used for that, because the level-change event is named differently in 1.21.11 and 26.x (see FABRIC-VERIFY.md).
- A respawn gives a new entity id. The old id's hidden pairs and window stayed until the 1200 tick sweep, so the "hidden pairs" number could look high. Respawn now forgets the old id. The window table is also swept, it only ever grew before.
- A same-dimension teleport does not start a window. Entities in range are then re-checked every 4 ticks, and a visible one is shown after one pass (200 ms at worst). Left as is.
- A tick counter that goes backwards (a second server in the same JVM) counts as an expired window. Tested.

## Geyser and Bedrock
- Detected by a zero UUID high half only. A dot name prefix is never trusted. Tested in ViewerPolicyTest.
- Anti-ESP never culls for a Bedrock viewer unless `veilBedrock` is true.
- Anti-xray cannot be skipped per player (chunks are shared), so Bedrock players get fake ore and hidden chests. The reveal packets above are ordinary block updates that Geyser translates. With the fix, an exposed fake is now corrected for them as well.
- Unknown until a Bedrock client runs on the rig: whether Geyser keeps a stale fake in its own chunk cache when the Java server sends the real block, and how a hidden chest's block entity data is translated. If a Bedrock player sees an ore that does not break as an ore, or a chest that will not open after digging to it, that is where to look first.

## Staff audit
`/mesh case` and `/mesh status` now write an audit line with who looked and at whom (`case.view`, `status.view`). They show evidence about players.

## Covered by sim tests (VeilSituationsTest, ExemptRule)
Exemptions (self, mounts, shared boat or horse, boss, glowing, projectile, leash, spectator or creative viewer, Bedrock viewer, team, join grace, mobs), a viewer switching to spectator and back, a mount while hidden, dimension change and return, relog, a hidden target that dies or logs out, Bedrock viewer with veilBedrock on and off, Bedrock player as target, every chunk border face with an open or unloaded neighbour.

## Only the rig can prove
- That Geyser rebuilds the entity and chunk state of a Bedrock viewer correctly after hide and show, after the fake ore resend, and after a dimension change. The policy side is tested, the Geyser side is not.
- What the real client does with the block resend mid-dig (keeps progress or restarts), and the vanilla destroy-progress path with real tools and effects.
- That a mixin removal of a tracked entity looks the same to the client as an out-of-range removal: no ghost, no frozen model, name tag gone, and the entity comes back with its current state (equipment, pose, passengers) on show.
- Passenger handling in the tracker: a hidden mount with a visible rider, and a hidden rider on a visible boat.
- Real chunk loading order at a border: that exposure is rebuilt when the neighbour finishes loading, and no ore stays visible or hidden wrongly after /tp across a border.
- Teleport and respawn timing against the 40 tick grace (the sim has no tick source).
- Real ray results: fences, gates, walls, stairs, slabs, leaves, glass, ice, vines with real VoxelShapes.
- Plugin and mod interaction: vanish, other entity-tracking changes, Floodgate.
- Time cost on a loaded server (PROFILING-PLAN).
- Mining, movement and combat heat with the veil on (VEIL-ON-HEAT-CHECKLIST).
