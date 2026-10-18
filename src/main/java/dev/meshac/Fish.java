package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * AutoFish: reels in the tick after the bite sound arrives. The server knows when the bite started (the hook's nibble timer goes above 0) and when the reel packet came.
 * Honest reaction to the splash sound is 140 ms at the very best and usually 250+. The hack reels at round trip + at most 2 ticks. Round trip is taken off the measured gap.
 * Lag assumption: reaction = gap - latency, where latency is the keepalive round trip (up to 300 ms tested). The bite tick is sampled at the end of the server tick, so the
 * gap reads up to 50 ms long, never short. Threshold 100 ms leaves 40 ms under the best honest reaction. Four bites in a row under it; one slower reel resets the streak.
 */
public final class Fish {
	private static final class S { boolean biting; long biteMs; int fast; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final long FAST_MS = 100; private static final int STREAK = 4;

	/** END_SERVER_TICK. */
	public static void tick(MinecraftServer srv) {
		for (ServerPlayer pl : srv.getPlayerList().getPlayers()) {
			var hook = pl.fishing; // VERIFY: public FishingHook fishing on Player (Mojang names)
			S s = STATE.get(pl.getUUID());
			if (hook == null) { if (s != null) s.biting = false; continue; }
			if (s == null) s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
			boolean nib = ((dev.meshac.mixin.FishingHookAccess) hook).meshac$nibble() > 0;
			if (nib && !s.biting) { s.biting = true; s.biteMs = System.currentTimeMillis(); } else if (!nib) s.biting = false;
		}
	}

	/** A use-item packet: the rod is cast or reeled. Called before the use is processed, so pl.fishing is still the hook being reeled. */
	public static void use(ServerPlayer pl) {
		if (pl.fishing == null || pl.isCreative()) return;
		S s = STATE.get(pl.getUUID());
		if (s == null || !s.biting) return;
		long reaction = System.currentTimeMillis() - s.biteMs - Math.max(0, Math.min(300, pl.connection.latency())); // VERIFY latency() on ServerGamePacketListenerImpl
		s.biting = false;
		if (reaction < FAST_MS) { if (++s.fast >= STREAK) { s.fast = 0; Verdict.signal(pl, "interact", "reeled in " + Math.max(0, reaction) + " ms after the bite, " + STREAK + " times in a row", 1); } }
		else s.fast = 0;
	}
	public static void forget(UUID id) { STATE.remove(id); }
}
