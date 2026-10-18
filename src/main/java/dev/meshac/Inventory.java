package dev.meshac;

import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/** AutoSteal, AutoArmor, AutoDrop, AutoTotem and friends: moving items in one blink, or on a metronome. */
public final class Inventory {
	private static final class S { long last, hotStart; int hot; final BeatCounter beat = new BeatCounter(); final InventoryBurst burst = new InventoryBurst(); final Queue<Long> arrivals = new ConcurrentLinkedQueue<>(); }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();

	/** The click reaches the network thread: note when, before the server tick batches it. */
	public static void arrive(ServerPlayer pl) { STATE.computeIfAbsent(pl.getUUID(), k -> new S()).arrivals.add(System.currentTimeMillis()); }

	/** The same click, run on the server thread. Returns when it reached the network. */
	public static long click(ServerPlayer pl, String clickKind, int item) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		Long at = s.arrivals.poll(); // always consume the arrival that belongs to this click, skipped or not, or the queue grows for the whole session
		if (Config.get().skips(pl, "inventory")) { s.burst.reset(); s.beat.reset(); s.last = 0; return at != null ? at : System.currentTimeMillis(); } // and start clean when the check comes back
		long now = at != null ? at : System.currentTimeMillis(), gap = now - s.last;
		InventoryBurst.Kind kind = InventoryBurst.kind(clickKind);
		if (kind == InventoryBurst.Kind.QUICK_CRAFT) return now; // drag release: start, one packet per slot, end. Not a move on its own, and it must not break up a beat or a burst count
		s.last = now;
		if (s.burst.observe(now, kind, item, Ticks.bunched(pl))) { Verdict.signal(pl, "inventory", "more than " + InventoryBurst.MAX_IN_WINDOW + " item moves in " + InventoryBurst.WINDOW_MS + " ms", 1); }
		if (s.beat.observe(gap, now)) Verdict.signal(pl, "inventory", "item moves on a steady beat of " + gap + " ms", 1); // 5 gaps equal to within 8 ms; see BeatCounter for the cap on a long beat
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
