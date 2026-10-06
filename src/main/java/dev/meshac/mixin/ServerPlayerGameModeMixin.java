package dev.meshac.mixin;

import dev.meshac.Mining;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerGameMode.class)
public abstract class ServerPlayerGameModeMixin {
	@Shadow protected ServerPlayer player;

	@Inject(method = "destroyBlock", at = @At("HEAD"), cancellable = true)
	private void meshac$break(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (Mining.refuse(player, pos)) cir.setReturnValue(false);
	}
}
