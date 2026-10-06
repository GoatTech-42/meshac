package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Scaffold, AirPlace, AutoBuild, Throw: using blocks and items faster, or somewhere else, than a hand does. */
public final class Interact {
	private static final class S { long windowAt; int uses; long lastUse; int burst; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MAX_PER_SECOND = 16; // fast bridging is about 8 a second

	/** A use-item packet (throw, eat, bow). */
	public static void use(ServerPlayer pl) { rate(pl); }

	/** A use-item-on-block packet (placing). */
	public static void place(ServerPlayer pl, ServerboundUseItemOnPacket p) {
		if (pl.isCreative() || pl.isSpectator()) return;
		AABB box = new AABB(p.getHitResult().getBlockPos()).inflate(0.3);
		Vec3 eye = pl.getEyePosition();
		if (box.clip(eye, eye.add(pl.getLookAngle().scale(8))).isEmpty()) Verdict.signal(pl, "interact", "clicked a block it is not looking at", 1);
		rate(pl);
	}

	private static void rate(ServerPlayer pl) {
		if (pl.isCreative() || pl.isSpectator()) return;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.windowAt > 1000) { s.windowAt = now; s.uses = 0; }
		if (now - s.lastUse < 20) s.burst++; else s.burst = 0; // several uses inside one tick
		s.lastUse = now;
		if (s.burst >= 3) { s.burst = 0; Verdict.signal(pl, "interact", "several uses in one tick", 1); }
		else if (++s.uses > MAX_PER_SECOND) { s.uses = 0; Verdict.signal(pl, "interact", "more than " + MAX_PER_SECOND + " uses in a second", 1); }
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
