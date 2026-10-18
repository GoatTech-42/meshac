package dev.meshac;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Meshac implements ModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("meshac");

	/** Bedrock identity uses zero-high-half UUID or the optional Floodgate API for linked accounts. Names are never an identity signal. Existing movement/check-specific policies still decide coverage. */
	public static boolean skip(net.minecraft.server.level.ServerPlayer pl) {
		return BedrockIds.DEFAULT.isBedrock(pl.getUUID());
	}

	/** Bedrock movement types not yet proven clean with a Bedrock client: gliding, vehicles, climbing, levitation, slow falling, riptide. Movement checks stay off for a Bedrock player while one of these applies. */
	public static boolean bedrockUnproven(net.minecraft.server.level.ServerPlayer pl) {
		return skip(pl) && (pl.isFallFlying() || pl.isPassenger() || pl.onClimbable() || pl.isAutoSpinAttack() || pl.hasEffect(net.minecraft.world.effect.MobEffects.LEVITATION) || pl.hasEffect(net.minecraft.world.effect.MobEffects.SLOW_FALLING));
	}

	@Override
	public void onInitialize() {
		BedrockIds.DEFAULT.onLog(m -> LOG.info("bedrock: {}", m));
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(Combat::recorderServer);
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(server -> Combat.recorderServer(null));
		Config.load();
		Config.get().warnUnknownChecks();
		Config.applyVeil();
		Cases.load();
		CommandRegistrationCallback.EVENT.register((d, reg, env) -> MeshCommands.register(d));
		// A banned player who reconnects is shown the ban screen again with the time that is left.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> { BedrockIds.DEFAULT.forget(handler.player.getUUID());
			dev.meshac.veil.Veil.grace(handler.player, server.getTickCount());
			Cases.Case c = Cases.activeBan(handler.player.getUUID());
			if (c != null) handler.disconnect(Screens.ban(c)); else Fingerprints.check(handler.player);
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> { for (var pl : new java.util.ArrayList<>(server.getPlayerList().getPlayers())) { if (!bedrockUnproven(pl)) Movement.tick(pl); if (skip(pl)) continue; Combat.tick(pl, server.getTickCount()); Fingerprints.check(pl); } dev.meshac.veil.Veil.tick(server); });
		// A respawned player is a new entity with a new id: drop the old one's pairs. The no-cull window for the new id and for dimension changes starts in Veil.tick (ViewerEpoch), because the level-change event has a different name in 1.21.11 and 26.x. AFTER_RESPAWN(ServerPlayer, ServerPlayer, boolean) is the same in every version (checked in the cached jars).
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> { dev.meshac.veil.Veil.forgetPlayer(oldP); });
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(Fish::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { BedrockIds.DEFAULT.forget(handler.player.getUUID()); Fingerprints.forget(handler.player.getUUID()); Movement.forget(handler.player.getUUID()); Verdict.forget(handler.player.getUUID()); dev.meshac.veil.Veil.forgetPlayer(handler.player); Combat.forget(handler.player.getUUID()); Vehicle.forget(handler.player.getUUID()); Mining.forget(handler.player.getUUID()); Interact.forget(handler.player.getUUID()); Inventory.forget(handler.player.getUUID()); Totem.forget(handler.player.getUUID()); Glide.forget(handler.player.getUUID()); Ticks.forget(handler.player.getUUID()); Chatter.forget(handler.player.getUUID()); Fish.forget(handler.player.getUUID()); });
		CombatLog.init();
		LOG.info("meshac loaded");
	}
}
