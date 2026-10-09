package dev.meshac.mixin;

import dev.meshac.Glide;
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
	@Shadow private net.minecraft.world.phys.Vec3 awaitingPositionFromClient;
	@Shadow public abstract void teleport(double x, double y, double z, float yRot, float xRot);

	@Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
	private void meshac$move(ServerboundMovePlayerPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (!player.level().getServer().isSameThread()) { Movement.arrive(player, p); return; } // vanilla re-queues this packet onto the main thread
		Movement.awaiting(player, awaitingPositionFromClient != null);
		double[] back = Glide.check(player, p);
		if (back == null) back = Movement.check(player, p);
		if (back != null) {
			teleport(back[0], back[1], back[2], player.getYRot(), player.getXRot());
			ci.cancel();
		}
	}

	@Inject(method = "handleMoveVehicle", at = @At("HEAD"), cancellable = true)
	private void meshac$vehicle(ServerboundMoveVehiclePacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (player.level().getServer().isSameThread() && Vehicle.check(player, p)) ci.cancel();
	}

	@Inject(method = "handleSetCarriedItem", at = @At("HEAD"))
	private void meshac$swap(ServerboundSetCarriedItemPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (player.level().getServer().isSameThread()) { Combat.swapped(player, p.getSlot()); Interact.swap(player); Inventory.hotbar(player); Trace.t(player, "hotbar slot=" + p.getSlot()); }
	}

	@Inject(method = "handleUseItemOn", at = @At("HEAD"), cancellable = true)
	private void meshac$place(ServerboundUseItemOnPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (player.level().getServer().isSameThread()) {
			boolean refused = Interact.place(player, p);
			Trace.t(player, "use-on" + (refused ? " REFUSED" : "") + " pos=" + dev.meshac.Packets.hit(p).getBlockPos() + " item=" + player.getMainHandItem().getItem());
			if (refused) {
				net.minecraft.core.BlockPos at = dev.meshac.Packets.hit(p).getBlockPos();
				player.connection.ackBlockChangesUpTo(dev.meshac.Packets.seq(p));
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.level(), at));
				player.connection.send(new net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket(player.level(), at.relative(dev.meshac.Packets.hit(p).getDirection())));
				player.containerMenu.sendAllDataToRemote();
				ci.cancel();
			}
		}
	}

	@Inject(method = "handleSignUpdate", at = @At("HEAD"), cancellable = true)
	private void meshac$sign(net.minecraft.network.protocol.game.ServerboundSignUpdatePacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		// the vanilla handler does not hop to the main thread before this point, so the hook runs on the network thread
		String[] lines = dev.meshac.Packets.lines(p);
		long gap = Interact.signGap(player, lines);
		Trace.t(player, "sign text" + (gap >= 0 ? " REFUSED gap=" + gap : "") + " lines=" + String.join("|", lines));
		if (gap >= 0) {
			ci.cancel();
			net.minecraft.server.level.ServerPlayer pl = player;
			pl.level().getServer().execute(() -> dev.meshac.Verdict.signal(pl, "interact", "sign text filled in " + gap + " ms after the editor opened", 1));
		}
	}

	@Inject(method = "handleUseItem", at = @At("HEAD"), cancellable = true)
	private void meshac$use(ServerboundUseItemPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (player.level().getServer().isSameThread()) {
			dev.meshac.Fish.use(player);
			boolean refused = Interact.use(player);
			Trace.t(player, "use item=" + player.getMainHandItem().getItem() + (refused ? " REFUSED" : ""));
			if (refused) { player.connection.ackBlockChangesUpTo(dev.meshac.Packets.seq(p)); player.containerMenu.sendAllDataToRemote(); ci.cancel(); }
		}
	}

	// VERIFY packet class and accessor: ServerboundChatCommandPacket.command() in 1.21.11 and 26.x (signed commands use ServerboundChatCommandSignedPacket, hook it the same way).
	@Inject(method = "handleChatCommand", at = @At("HEAD"))
	private void meshac$chatCommand(net.minecraft.network.protocol.game.ServerboundChatCommandPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		// vanilla handles chat commands without hopping to the main thread first, so this hook only ever runs on the network thread
		String msg = dev.meshac.Chatter.command(player, p.command());
		if (msg != null) { net.minecraft.server.level.ServerPlayer pl = player; pl.level().getServer().execute(() -> dev.meshac.Verdict.signal(pl, "chat", msg, 1)); }
	}

	@Inject(method = "handleClientTickEnd", at = @At("HEAD"))
	private void meshac$tickEnd(net.minecraft.network.protocol.game.ServerboundClientTickEndPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (!player.level().getServer().isSameThread()) Movement.tickEnd(player); else dev.meshac.Ticks.inc(player);
	}

	@Inject(method = "handlePlayerCommand", at = @At("HEAD"))
	private void meshac$command(net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (p.getAction() == net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket.Action.START_FALL_FLYING && player.level().getServer().isSameThread()) Glide.startFly(player);
	}

	@Inject(method = "handlePlayerAction", at = @At("HEAD"), cancellable = true)
	private void meshac$release(ServerboundPlayerActionPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (player.level().getServer().isSameThread()) Trace.t(player, "action " + p.getAction());
		if (player.level().getServer().isSameThread() && !player.isCreative()) {
			if (p.getAction() == ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK) dev.meshac.Mining.digStart(player, p.getPos());
			else if (p.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK) dev.meshac.Mining.digStop(player, p.getPos());
			else if (p.getAction() == ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK) dev.meshac.Mining.digAbort(player);
		}
		if (p.getAction() == ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND && player.level().getServer().isSameThread() && !player.isCreative() && Totem.refuse(player, System.currentTimeMillis(), 45, true)) { ci.cancel(); player.containerMenu.sendAllDataToRemote(); return; }
		if (p.getAction() == ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM && player.getUseItem().is(net.minecraft.tags.ItemTags.BOW_ENCHANTABLE) && player.level().getServer().isSameThread()) Interact.release(player);
	}

	@Inject(method = "handleContainerClick", at = @At("HEAD"), cancellable = true)
	private void meshac$click(ServerboundContainerClickPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (!player.level().getServer().isSameThread()) { Inventory.arrive(player); return; }
		long at = Inventory.click(player);
		Trace.t(player, "click slot=" + p.slotNum() + " button=" + p.buttonNum() + " arrived=" + at);
		boolean offhand = p.containerId() == 0 && (p.slotNum() == 45 || p.buttonNum() == 40);
		if (!player.isCreative() && Totem.refuse(player, at, p.slotNum(), offhand)) { ci.cancel(); player.containerMenu.sendAllDataToRemote(); }
	}
}
