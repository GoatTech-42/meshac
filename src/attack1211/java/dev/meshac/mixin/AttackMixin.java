package dev.meshac.mixin;

import dev.meshac.Combat;
import dev.meshac.Verdict;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Melee checks for Minecraft 1.21.x, where an attack is an interact packet with the attack action. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class AttackMixin {
	@Shadow public ServerPlayer player;

	@Inject(method = "handleInteract", at = @At("HEAD")) // 0055: not cancellable - combat never denies the hit
	private void meshac$attack(ServerboundInteractPacket p, CallbackInfo ci) {
		if (dev.meshac.Meshac.skip(player)) return;
		if (!player.level().getServer().isSameThread()) return;
		boolean[] attack = {false};
		p.dispatch(new ServerboundInteractPacket.Handler() {
			public void onInteraction(InteractionHand h) {}
			public void onInteraction(InteractionHand h, Vec3 v) {}
			public void onAttack() { attack[0] = true; }
		});
		if (!attack[0]) return;
		Entity e = p.getTarget(player.level());
		if (e == null) return;
		String hit = Combat.check(player, e);
		if (hit == null) return;
		Verdict.signalLogOnly(player, "combat", hit, 2); // 0055: combat log-only BY CONSTRUCTION in every config; never feeds the removal ladder (gate: 100/300 ms FP replay + reviewer GO)
	}
}
