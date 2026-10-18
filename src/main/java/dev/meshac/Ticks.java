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
	public static void inc(ServerPlayer pl) { long[] s = S.computeIfAbsent(pl.getUUID(), k -> new long[5]); long now = System.currentTimeMillis();
		// 4 tick-end packets inside 5 ms means the link delivered a backlog: view and click packets are bunched too
		s[3] = now - s[1] < 5 ? s[3] + 1 : 0; if (s[3] >= 3) s[4] = now + 3000;
		s[0]++; s[1] = now; }
	/** True for 3 s after a packet backlog landed. Rules that compare a click with the view at server time stay off then. */
	public static boolean bunched(ServerPlayer pl) { long[] s = S.get(pl.getUUID()); return s != null && System.currentTimeMillis() < s[4]; }
	public static long n(ServerPlayer pl) { long[] s = S.get(pl.getUUID()); return s == null ? 0 : s[0]; }
	/** True when tick-end packets arrived recently. A stalled link also reads false, which is the safe direction (rules off). */
	public static boolean active(ServerPlayer pl) { long[] s = S.get(pl.getUUID()); return s != null && s[0] > 40 && System.currentTimeMillis() - s[1] < 500; }
	public static void forget(UUID id) { S.remove(id); }
}
