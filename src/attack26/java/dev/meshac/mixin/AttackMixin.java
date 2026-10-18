package dev.meshac.mixin;

import dev.meshac.Combat;
import dev.meshac.Verdict;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Melee checks for Minecraft 26.1 and later, where attacks have their own packet. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AttackMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handleAttack", at = @At("HEAD")) // 0055: not cancellable - combat never denies the hit
	private void meshac$attack(ServerboundAttackPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (!player.level().getServer().isSameThread()) return;
		Entity e = player.level().getEntity(p.entityId());
		if (e == null) return;
		String hit = Combat.check(player, e);
		if (hit == null) return;
		Verdict.signalLogOnly(player, "combat", hit, 2); // 0055: combat log-only BY CONSTRUCTION in every config; never feeds the removal ladder (gate: 100/300 ms FP replay + reviewer GO)
	}

	@Inject(method = "handleInteract", at = @At("HEAD"), cancellable = true)
	private void meshac$interact(net.minecraft.network.protocol.game.ServerboundInteractPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (player.level().getServer().isSameThread() && dev.meshac.Interact.entity(player, p.entityId())) ci.cancel();
	}
}
