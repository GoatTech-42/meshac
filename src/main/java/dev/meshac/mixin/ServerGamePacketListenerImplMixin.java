package dev.meshac.mixin;

import dev.meshac.Movement;
import dev.meshac.Combat;
import dev.meshac.Interact;
import dev.meshac.Inventory;
import dev.meshac.Totem;
import dev.meshac.Trace;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import dev.meshac.Vehicle;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
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
			ci.cancel();
		}
	}

	@Inject(method = "handleMoveVehicle", at = @At("HEAD"), cancellable = true)
	private void meshac$vehicle(ServerboundMoveVehiclePacket p, CallbackInfo ci) {
		if (player.level().getServer().isSameThread() && Vehicle.check(player, p)) ci.cancel();
	}

	@Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
	private void meshac$swap(ServerboundSetCarriedItemPacket p, CallbackInfo ci) {
		if (player.level().getServer().isSameThread()) { Combat.swapped(player, p.getSlot()); Inventory.hotbar(player); Trace.t(player, "hotbar slot=" + p.getSlot()); }
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
	private void meshac$place(ServerboundUseItemOnPacket p, CallbackInfo ci) {
		if (player.level().getServer().isSameThread()) {
			boolean refused = Interact.place(player, p);
			Trace.t(player, "use-on" + (refused ? " REFUSED" : "") + " pos=" + p.getHitResult().getBlockPos() + " item=" + player.getMainHandItem().getItem());
			if (refused) {
				net.minecraft.core.BlockPos at = p.getHitResult().getBlockPos();
				player.connection.ackBlockChangesUpTo(p.getSequence());
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.level(), at));
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.level(), at.relative(p.getHitResult().getDirection())));
				player.containerMenu.sendAllDataToRemote();
				ci.cancel();
			}
		}
	}

	@Inject(method = "handleUseItem", at = @At("HEAD"))
	private void meshac$use(ServerboundUseItemPacket p, CallbackInfo ci) {
		if (player.level().getServer().isSameThread()) { Interact.use(player); Trace.t(player, "use item=" + player.getMainHandItem().getItem()); }
	}

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
	private void meshac$release(ServerboundPlayerActionPacket p, CallbackInfo ci) {
		if (player.level().getServer().isSameThread()) Trace.t(player, "action " + p.getAction());
		if (p.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND && player.level().getServer().isSameThread() && !player.isCreative() && Totem.refuse(player, System.currentTimeMillis(), 45, true)) { ci.cancel(); player.containerMenu.sendAllDataToRemote(); return; }
		if (p.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && player.getUseItem().is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE) && player.level().getServer().isSameThread()) Interact.release(player);
	}

	@Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
	private void meshac$click(ServerboundContainerClickPacket p, CallbackInfo ci) {
		if (!player.level().getServer().isSameThread()) { Inventory.arrive(player); return; }
		long at = Inventory.click(player);
		Trace.t(player, "click slot=" + p.slotNum() + " button=" + p.buttonNum() + " arrived=" + at);
		boolean offhand = p.containerId() == 0 && (p.slotNum() == 45 || p.buttonNum() == 40);
		if (!player.isCreative() && Totem.refuse(player, at, p.slotNum(), offhand)) { ci.cancel(); player.containerMenu.sendAllDataToRemote(); }
	}
}
