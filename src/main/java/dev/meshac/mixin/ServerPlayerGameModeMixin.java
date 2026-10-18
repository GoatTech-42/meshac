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
		if (!player.level().getServer().isSameThread()) return; // 0065: mining checks only on the server thread - an off-thread invocation loses detection (fail-safe), never races Verdict. No server.execute here: a delayed refusal would change the synchronous cancel semantics (0064 reviewer).
		if (!dev.meshac.Meshac.skip(player) && Mining.refuse(player, pos)) cir.setReturnValue(false);
	}
}
