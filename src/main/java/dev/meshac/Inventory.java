package dev.meshac;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** AutoSteal, AutoArmor, AutoDrop, AutoTotem and friends: moving items in one blink, or on a metronome. */
public final class Inventory {
	private static final class S { long start, last, lastGap, hotStart; int clicks, even, hot; final Queue<Long> arrivals = new ConcurrentLinkedQueue<>(); }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MAX_IN_WINDOW = 10; // a fast drag across slots stays under this; a script empties a chest in one tick
	private static final long WINDOW_MS = 100;
	private static final long EVEN_MS = 8, SLOW_MS = 160; // a hand never repeats a click gap to within 8 ms five times running

	/** The click reaches the network thread: note when, before the server tick batches it. */
	public static void arrive(ServerPlayer pl) { STATE.computeIfAbsent(pl.getUUID(), k -> new S()).arrivals.add(System.currentTimeMillis()); }

	/** The same click, run on the server thread. Returns when it reached the network. */
	public static long click(ServerPlayer pl) {
		if (pl.isCreative() || pl.isSpectator()) return System.currentTimeMillis();
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		Long at = s.arrivals.poll();
		long now = at != null ? at : System.currentTimeMillis(), gap = now - s.last;
		s.last = now;
		if (now - s.start > WINDOW_MS) { s.start = now; s.clicks = 0; }
		if (++s.clicks > MAX_IN_WINDOW) { s.clicks = 0; Verdict.signal(pl, "inventory", "more than " + MAX_IN_WINDOW + " item moves in " + WINDOW_MS + " ms", 1); }
		if (gap < SLOW_MS && Math.abs(gap - s.lastGap) < EVEN_MS) { if (++s.even >= 5) { s.even = 0; Verdict.signal(pl, "inventory", "item moves on a steady beat of " + gap + " ms", 1); } } else s.even = 0;
		s.lastGap = gap;
		return now;
	}

	/** AutoSwitch: the held slot changes every tick. A free-spinning scroll wheel tops out well under 15 notches a second. */
	public static void hotbar(ServerPlayer pl) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.hotStart > 1000) { s.hotStart = now; s.hot = 0; }
		if (++s.hot > 30) { s.hot = 0; Verdict.signal(pl, "inventory", "hotbar slot changed over 30 times in a second", 1); }
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
