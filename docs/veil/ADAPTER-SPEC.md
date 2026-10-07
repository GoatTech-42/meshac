# Veil adapter: what the server agent must look up on the box (Mojang-mapped jars, per version)
I have no Minecraft jars here, so the Minecraft-facing glue is specified, not guessed. The pure logic (XrayCore, EspCore) is done and has a self test.

## A. Xray hook
Find where a chunk section is written into ClientboundLevelChunkWithLightPacket (look in ClientboundLevelChunkPacketData's constructor / extractChunkData, and LevelChunkSection.write). Plan: @Redirect or @ModifyArg on the call that writes each section's PalettedContainer, replace with a copy:
1. If the section palette contains no hidden state (check palette first, cheap), write the original.
2. Else copy the container, for each hidden position call XrayCore.shouldHide with a Blocks view over the chunk (neighbour lookups inside the section from the copy, across section borders from the chunk, across chunk borders from the loaded neighbour chunk or "transparent" if unloaded = fail open).
3. Replacement state: stone (overworld y>=0), deepslate (y<0), netherrack (nether), end stone (end). Fake ore (XrayCore.fakeOreAt, salt = hash of world seed) uses the dimension's ore variant of that stratum, only on buried plain stone/deepslate/netherrack positions.
4. Cache the encoded result per (chunk, chunk modification count); invalidate in the block-change hook below.
5. Block entities: drop BE data for hidden chests/spawners from the packet's blockEntitiesData list when the position was hidden.
Mixin class: dev.meshac.veil.mixin.ChunkPacketMixin (new entry in meshac.mixins.json). Per-version differences go into src/veil_a (1.21.11) and src/veil_b (26.x) like packets_a/b, chosen with a veil_src=... line in versions/*.properties and a sourceSets line in build.gradle.

## B. Reveal on exposure
Hook ServerLevel.sendBlockUpdated or LevelChunk.setBlockState (server side). After a block becomes transparent, for each of the 6 neighbours that is in the hidden list, send the real state to players tracking that chunk with ClientboundBlockUpdatePacket (+ block entity update for chests). Do it within the same tick. Explosions and pistons go through the same setBlockState so no extra hooks.

## C. Entity cull
Hook ChunkMap.TrackedEntity.updatePlayer(ServerPlayer) (or ServerEntity.addPairing/removePairing). At the point where it decides "visible to player", additionally ask Veil.allow(player, entity); if false and previously paired, call removePlayer(player) (sends ClientboundRemoveEntitiesPacket); when it flips back, the normal pairing path sends the spawn. Maintain EspCore state keyed by (player id, entity id) and run the LOS test every 4 ticks from a staggered queue, max 200 pairs per tick:
- LOS: for each point from EspCore.samplePoints, a block raycast from the viewer eye (and from eye + velocity*6 ticks, any pass counts) using level.clip(ClipContext with COLLIDER and fluid NONE) or a custom voxel walk using section caches. Visible if ANY point reaches without hitting a solid block. Transparent blocks (glass, leaves, fences, etc.) must not block: use the block's occlusion shape / isSolidRender, not "is not air".
- exempt: viewer.getVehicle() == e or passengers, bosses (EnderDragon, WitherBoss), e.isCurrentlyGlowing(), same team, viewer spectator/creative, leashed holder, "meshac.veil.bypass" permission, entity type not in the culled kinds, projectiles.
- Combat exemption: call EspCore.attacked(viewer, target, tick) from Combat.check.
- On player disconnect or entity removal, EspCore.forget.
- Do not cull when the player is in a vehicle with a fast fall, or during the first 40 ticks after join/teleport/dimension change (everything visible while the client loads).

## D. Config
In Config: public Veil veil = new Veil(); fields per design doc (xray.enabled, hiddenBlocks, fakeOreRatio default 0.15, esp.enabled, players, mobs, cullAfterChecks 3, nearRadius 8, checkEveryTicks 4, maxRaysPerTick 200) with lenient/default/strict presets (lenient: cullAfterChecks 5, nearRadius 16, ratio 0.05; strict: cullAfterChecks 2, nearRadius 6, ratio 0.25). /mesh veil status for ms and counts.

## Measured/unknown to report back
Hidden-list block ids differ in 1.21.11 vs 26.x only if renamed; the adapter should use tags/registry keys, not class names.
