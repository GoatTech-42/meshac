package dev.meshac;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;

/** Combat log protection. Dealing or taking damage from another player tags both for tagSeconds; leaving while tagged kills the player and logs a case. PvE never tags. */
public final class CombatLog {
	private static final Map<UUID, Long> tagged = new ConcurrentHashMap<>();

	static void init() {
		ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
			if (!Config.get().combatLog()) return;
			if (!(entity instanceof ServerPlayer victim) || !(source.getEntity() instanceof ServerPlayer attacker) || victim == attacker) return;
			tag(victim); tag(attacker);
		});
		ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if (entity instanceof ServerPlayer p) tagged.remove(p.getUUID()); });
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer p = handler.player; Long until = tagged.remove(p.getUUID());
			if (until == null || System.currentTimeMillis() > until || !Config.get().combatLog() || !server.isRunning()) return;
			if (!p.isAlive() || p.isCreative() || p.isSpectator()) return;
			Cases.add(p.getGameProfile().name(), p.getUUID(), "combatlog", "Disconnected during combat", "meshac-combat", 0, List.of("left while combat-tagged; killed"));
			Meshac.LOG.info("[meshac] combat log: {} left during combat and was killed", p.getGameProfile().name());
			p.kill((net.minecraft.server.level.ServerLevel) p.level());
			server.getPlayerList().broadcastSystemMessage(net.minecraft.network.chat.Component.literal(p.getGameProfile().name() + " combat logged and was killed."), false);
		});
	}

	private static void tag(ServerPlayer p) {
		if (p.isCreative() || p.isSpectator()) return;
		tagged.put(p.getUUID(), System.currentTimeMillis() + Config.get().combatLogSeconds() * 1000L);
	}
}
