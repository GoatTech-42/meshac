package dev.meshac.veil;

import net.minecraft.server.level.ServerPlayer;

/** Duck interface implemented by the TrackedEntity mixin. */
public interface TrackedEntityAccess {
	void meshac$remove(ServerPlayer p);
	void meshac$update(ServerPlayer p);
}
