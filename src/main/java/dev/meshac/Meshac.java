package dev.meshac;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Meshac implements ModInitializer {
	public static final Logger LOG = LoggerFactory.getLogger("meshac");

	@Override
	public void onInitialize() {
		Config.load();
		Config.applyVeil();
		Cases.load();
		CommandRegistrationCallback.EVENT.register((d, reg, env) -> MeshCommands.register(d));
		// A banned player who reconnects is shown the ban screen again with the time that is left.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			dev.meshac.veil.Veil.grace(handler.player, server.getTickCount());
			Cases.Case c = Cases.activeBan(handler.player.getUUID());
			if (c != null) handler.disconnect(Screens.ban(c)); else Fingerprints.check(handler.player);
		});
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> { for (var pl : new java.util.ArrayList<>(server.getPlayerList().getPlayers())) { Movement.tick(pl); Fingerprints.check(pl); } dev.meshac.veil.Veil.tick(server); });
		net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(Fish::tick);
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { Fingerprints.forget(handler.player.getUUID()); Movement.forget(handler.player.getUUID()); Verdict.forget(handler.player.getUUID()); dev.meshac.veil.Veil.forgetPlayer(handler.player); Combat.forget(handler.player.getUUID()); Vehicle.forget(handler.player.getUUID()); Mining.forget(handler.player.getUUID()); Interact.forget(handler.player.getUUID()); Inventory.forget(handler.player.getUUID()); Totem.forget(handler.player.getUUID()); Glide.forget(handler.player.getUUID()); Ticks.forget(handler.player.getUUID()); Chatter.forget(handler.player.getUUID()); Fish.forget(handler.player.getUUID()); });
		LOG.info("meshac loaded");
	}
}
