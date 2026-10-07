package dev.meshac.mixin;

import dev.meshac.veil.Veil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reveal buried ores the moment a block next to them is removed. UNCOMPILED. */
@Mixin(ServerLevel.class)
public abstract class ServerLevelVeilMixin {
	@Inject(method = "sendBlockUpdated", at = @At("TAIL"))
	private void meshac$reveal(BlockPos pos, BlockState old, BlockState now, int flags, CallbackInfo ci) { Veil.reveal((ServerLevel) (Object) this, pos, now); }
}
