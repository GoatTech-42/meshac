package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Client tick counter. The client sends one tick-end packet per client tick. Counted on the main thread, in packet order, so a lag burst that
 * delivers 6 ticks of packets in one millisecond still reads as 6 client ticks apart. Rules that need "how many client ticks passed between A and B"
 * use this instead of wall-clock gaps. Clients that do not send tick-end packets (old versions through Via) report active() == false and those rules stay off.
 */
public final class Ticks {
	private static final Map<UUID, long[]> S = new ConcurrentHashMap<>(); // {count, lastSeenMs}
	public static void inc(ServerPlayer pl) { long[] s = S.computeIfAbsent(pl.getUUID(), k -> new long[2]); s[0]++; s[1] = System.currentTimeMillis(); }
	public static long n(ServerPlayer pl) { long[] s = S.get(pl.getUUID()); return s == null ? 0 : s[0]; }
	/** True when tick-end packets arrived recently. A stalled link also reads false, which is the safe direction (rules off). */
	public static boolean active(ServerPlayer pl) { long[] s = S.get(pl.getUUID()); return s != null && s[0] > 40 && System.currentTimeMillis() - s[1] < 500; }
	public static void forget(UUID id) { S.remove(id); }
}
