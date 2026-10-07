package dev.meshac.mixin;

import dev.meshac.veil.Veil;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Anti-ESP: vanilla re-pairs on chunk-section changes; keep culled entities unpaired. UNCOMPILED.
 * Field name `entity` in ChunkMap$TrackedEntity and `entityMap` in ChunkMap are assumptions (VERIFY, use the mapped names).
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedEntityMixin implements dev.meshac.veil.TrackedEntityAccess {
	@Shadow @org.spongepowered.asm.mixin.Final net.minecraft.world.entity.Entity entity;
	@Shadow public abstract void updatePlayer(ServerPlayer p);
	@Shadow public abstract void removePlayer(ServerPlayer p);

	@Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
	private void meshac$cull(ServerPlayer p, CallbackInfo ci) {
		if (Veil.isHidden(p, entity)) { removePlayer(p); ci.cancel(); }
	}
	public void meshac$remove(ServerPlayer p) { removePlayer(p); }
	public void meshac$update(ServerPlayer p) { updatePlayer(p); }
}
