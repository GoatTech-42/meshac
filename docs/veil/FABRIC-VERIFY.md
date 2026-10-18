# Fabric API names used by the veil work, checked per version

Checked with javap on the jars already in the box's Gradle cache (read only, no build). The umbrella pom of each Fabric API version lists the module versions below.

| Minecraft | Fabric API | entity-events | lifecycle-events | networking-api | command-api-v2 |
|---|---|---|---|---|---|
| 1.21.11 | 0.141.6+1.21.11 | 3.1.1 | 2.6.15 | 5.1.6 | 2.4.7 |
| 26.1.2 | 0.155.3+26.1.2 | 5.0.2 | 4.1.1 | 6.3.2 | 3.0.5 |
| 26.2 | 0.152.1 and 0.161.0 | 5.0.5 | 4.1.3 and 4.1.4 | 6.3.3 and 6.3.4 | 3.1.0 |
| 26.3 | 0.160.6 and 0.161.0 | 6.0.4 | 4.1.9 | 6.3.8 | 3.1.2 |

Note: 1.21.11 jars show Intermediary names (class_3222) because that Fabric API is not remapped to Mojang names. The source names are the same, Loom remaps them.

## What this branch uses
| API | Signature | Same on all four? |
|---|---|---|
| `ServerPlayerEvents.AFTER_RESPAWN` | `afterRespawn(ServerPlayer old, ServerPlayer new, boolean alive)` | Yes. Found in 3.1.1, 5.0.2, 5.0.5 and 6.0.4. |
| `ServerPlayConnectionEvents.JOIN` | `onPlayReady(ServerGamePacketListenerImpl, PacketSender, MinecraftServer)` | Yes (5.1.6, 6.3.2 to 6.3.8). Already in use. |
| `ServerTickEvents.END_SERVER_TICK` | `onEndTick(MinecraftServer)` | Yes. Already in use. |
| `CommandRegistrationCallback.EVENT` | `register(CommandDispatcher<CommandSourceStack>, CommandBuildContext, Commands.CommandSelection)` | Yes. Already in use. |

## Names that differ between versions (not used, avoid)
| Thing | 1.21.11 | 26.x |
|---|---|---|
| Player changes world | `ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD` with `afterChangeWorld(ServerPlayer, ServerLevel, ServerLevel)` | class removed. `ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL` with `afterChangeLevel(ServerPlayer, ServerLevel, ServerLevel)` (5.0.2, 5.0.5, 6.0.4) |
| Chunk load | `ServerChunkEvents.Load.onChunkLoad(ServerLevel, LevelChunk)` | `onChunkLoad(ServerLevel, LevelChunk, boolean newlyGenerated)` |
| Chunk unload | `onChunkUnload(ServerLevel, LevelChunk)` | same |

My first version of the respawn change used `AFTER_PLAYER_CHANGE_WORLD`. It would not have compiled on any 26.x. The no-cull window for dimension changes and respawns is now started from the tick loop (`ViewerEpoch`), so no version split is needed.

## Not verified here (game names, needs the Box compile)
These carry `VERIFY` comments in the source and were not checked on the jars: `BlockState.canOcclude`, `ServerLevel.getSeed`, `LevelChunk.getMinY/getMaxY/getMinSectionY`, `LevelChunkSection.maybeHas`, `ChunkMap.getPlayers(ChunkPos, boolean)`, `Leashable`, `Entity.isCurrentlyGlowing`, `hasPassenger(Entity)`. The tracked-set names (`ChunkMap.entityMap`, `TrackedEntity.entity`) were checked with javap on 26.2 only.
