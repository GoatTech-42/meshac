# Geyser and the chunk block entity strip: what the source says

Read before the rig run, so the rig is not the first test. Nothing here was run.

## Which Geyser
ilhsmc runs Geyser-Fabric 2.11.3 (staged builds b1249 to b1254 are on the Box). Geyser has no release tags on GitHub, so a tag read was not possible. Evidence comes from two places:
- The Box checkout /home/luke/geyser-build/Geyser (branch live-build, head 35b1736, version line 2.12.0-SNAPSHOT, upstream base e215fa5 plus local commits). The chunk translator file has no local changes: its last commit is the upstream "Relocate various Java-registry classes" change (e57dfd6).
- javap on the Geyser-Fabric-2.11.3-b1254 jar in /home/luke/geyser-staging. JavaLevelChunkWithLightTranslator.class and JavaBlockEntityDataTranslator.class have the same byte sizes in b1249 and b1254 (18971 and 9723), so those two did not change between those builds. The other staged builds were not compared.

## What a chunk with a missing block entity entry does
JavaLevelChunkWithLightTranslator (source lines 268 to 296, bytecode confirmed in b1254, calls to Block.blockEntityType at offset 1325):
- Geyser walks the packet's block entity list. For each entry it reads the block state at that position from the chunk data it just decoded and only translates the entry when `type == blockState.block().blockEntityType()`. The source comment says: the server can send block entity data for blocks that are not those blocks, and a Java client ignores them.
- So an entry left in the list at a stone position (what the veil sent before this change) was already dropped by Geyser. An entry that is simply absent is also fine: Geyser builds Bedrock block entities only from the list it is given (plus a few block types it makes itself from the block state: piston, flower pot, cauldron, lectern, see BedrockChunkWantsBlockEntityTag). Chests, ender chests and spawners are not in that group, so a chunk without their entries gives Bedrock no tag for them, which is right while they are stone.
- A null or unknown type is skipped, not thrown.

## The reveal path
1. Java BlockUpdate packet: JavaBlockUpdateTranslator calls WorldCache.updateServerCorrectBlockState, which calls ChunkUtils.updateBlock. That sends the Bedrock block update and writes the new state into Geyser's chunk cache (`session.getChunkCache().updateBlock`).
2. Java BlockEntityData packet: JavaBlockEntityDataTranslator reads the state at the position from the cache and returns without doing anything unless the block's entity type matches the packet's type. Because step 1 already put the real state in the cache, the order the veil uses (block update first, then the entity packet) is the right one. The reverse order would be dropped silently.
3. Which block entities get an entity packet on reveal. In the 26.3 server jar BlockEntity.getUpdatePacket returns null by default, ChestBlockEntity and EnderChestBlockEntity do not override it, SpawnerBlockEntity does. So on reveal:
   - Spawner: Bedrock gets the block update and then the spawner entity data.
   - Chest and ender chest: Bedrock gets only the block update. This is the same sequence a Java server produces when a player places a chest, which Geyser handles every day. Geyser's ChestBlock.updateBlock adds a block entity only for double chests (CHEST_TYPE not single). A single chest on Bedrock with no entity tag is the placed-chest case. Not tested here: that is a rig item (open it, check contents and lid).
   This was already the behavior before the strip: a revealed chest never had its list entry reach Bedrock, because Geyser dropped it at the stone position. The strip does not change what a Bedrock client gets for a hidden chest.

## What is not covered
- Geyser builds other than b1249 to b1254 on the Box.
- Whether a Bedrock client renders a revealed single chest without an entity tag the same as a placed one. Source says it is the placed-chest path. The rig check has it.
- Double chests: the veil treats a chest as see-through (it does not occlude), so two chests side by side count as exposed to each other and are never hidden. A buried double chest is therefore a leak but not an inconsistent view. Read from the veil code, not run.

## Bedrock parity: linked and unlinked names
Every Bedrock decision in Meshac and the veil goes through one function, BedrockIds.isBedrock(uuid), through Meshac.skip and Veil.exempt. Names are never read by it. A grep of src for getMostSignificantBits, isBedrock, skip, the dot prefix and getGameProfile found:
- Chunk packets (xray, fake ore, hidden chests, and now the block entity strip): built once per chunk and shared, never looks at the viewer. A linked Bedrock player, an unlinked one and a Java player get the same chunk. Parity by construction.
- Anti-ESP: the viewer's Bedrock answer decides whether everything is shown (veilBedrock false). The target's Bedrock status is not used. So linked or unlinked as viewer gets the same result once BedrockIds has the answer, and as a target they are treated the same as a Java player. The answer for a linked viewer can lag a few seconds if Floodgate is not ready, and anti-ESP has a join grace window for that, so the veil errs toward showing.
- Combat, interact, inventory, mining, totem: skip for both. Movement: both keep the movement checks with the looser Bedrock budget.
- Name-based settings are the one place linked and unlinked differ: Floodgate gives an unlinked player a prefixed name (a dot by default, spaces as underscores) and a linked player the name of the Java account (FloodgatePlayerImpl.getCorrectUsername, Floodgate master source). So `exemptPlayers` and `exemptChecks` need ".My_Name" for the unlinked player and "TheirJavaName" for the linked one. That is a configuration detail, not a veil decision. The rig check has a step for it.
Sources: Floodgate FloodgatePlayerImpl.java and SimpleFloodgateApi.java on GeyserMC/Floodgate master (raw.githubusercontent.com), Geyser sources from the Box checkout above.
