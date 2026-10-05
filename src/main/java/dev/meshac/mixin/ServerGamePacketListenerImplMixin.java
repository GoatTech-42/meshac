package dev.meshac.mixin;

import dev.meshac.Movement;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerGamePacketListenerImplMixin {
	@Shadow public ServerPlayer player;
	@Shadow public abstract void teleport(double x, double y, double z, float yRot, float xRot);

	@Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
	private void meshac$move(ServerboundMovePlayerPacket p, CallbackInfo ci) {
		if (!player.level().getServer().isSameThread()) { Movement.arrive(player, p); return; } // vanilla re-queues this packet onto the main thread
		double[] back = Movement.check(player, p);
		if (back != null) {
			teleport(back[0], back[1], back[2], player.getYRot(), player.getXRot());
			if (back[3] == 1) ((net.minecraft.server.network.ServerCommonPacketListenerImpl) (Object) this).disconnect(net.minecraft.network.chat.Component.literal("meshac: impossible movement"));
			ci.cancel();
		}
	}
}
