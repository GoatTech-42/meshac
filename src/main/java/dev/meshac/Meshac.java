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
		Cases.load();
		CommandRegistrationCallback.EVENT.register((d, reg, env) -> MeshCommands.register(d));
		// A banned player who reconnects is shown the ban screen again with the time that is left.
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			Cases.Case c = Cases.activeBan(handler.player.getUUID());
			if (c != null) handler.disconnect(Screens.ban(c));
		});
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { Movement.forget(handler.player.getUUID()); Verdict.forget(handler.player.getUUID()); Combat.forget(handler.player.getUUID()); Vehicle.forget(handler.player.getUUID()); Mining.forget(handler.player.getUUID()); Interact.forget(handler.player.getUUID()); Inventory.forget(handler.player.getUUID()); Totem.forget(handler.player.getUUID()); });
		LOG.info("meshac loaded");
	}
}
