package dev.meshac.mixin;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkMap.class)
public abstract class ChunkMapMixin implements dev.meshac.veil.ChunkMapAccess {
	@Shadow @org.spongepowered.asm.mixin.Final private Int2ObjectMap<Object> entityMap; // VERIFY name and type (Int2ObjectMap<ChunkMap.TrackedEntity>); use @Accessor if generic mismatch
	public Int2ObjectMap<Object> meshac$entityMap() { return entityMap; }
}
