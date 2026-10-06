package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * AutoTotem: the off hand is filled inside a window no hand can hit, either right after a totem pops or right after
 * a click on another slot. Calibrated with Luke, a crystal PvP player: he has beaten 25 ms once by anticipating a pop,
 * never twice running; 10 ms or less is not a human. So 10 ms or less is refused at once, and 10 to 25 ms is a strike:
 * the first is only noted, the second in a row is refused and signalled, the third reaches the kick level. A fill at
 * normal speed ends the streak, and one stray fast packet on a laggy line never acts on its own.
 */
public final class Totem {
	private static final long IMPOSSIBLE_MS = 10, TOO_FAST_MS = 25;
	private static final long STRIKE_MS = 60_000;
	private static final Map<UUID, long[]> STATE = new ConcurrentHashMap<>(); // { popAt, lastClickAt, lastSlot, strikes, lastStrikeAt }

	/** The server just used up a totem for this player. */
	public static void popped(ServerPlayer pl) { state(pl)[0] = System.currentTimeMillis(); Trace.t(pl, "TOTEM-POP"); }

	/** True when this click is a repeat of an impossible off-hand fill: the click is refused. */
	public static boolean refuse(ServerPlayer pl, long arrivedAt, int slot, boolean toOffhand) {
		long[] s = state(pl);
		long gap = arrivedAt - s[1], since = arrivedAt - s[0];
		boolean pair = toOffhand && s[2] != 45 && gap >= 0 && gap < TOO_FAST_MS;
		boolean react = toOffhand && since >= 0 && since < TOO_FAST_MS;
		s[1] = arrivedAt; s[2] = slot;
		if (toOffhand) Trace.t(pl, "offhand-fill sincePop=" + since + "ms sinceLastClick=" + gap + "ms strikes=" + s[3] + " fast(pair=" + pair + " react=" + react + ")");
		if (!pair && !react) { if (toOffhand) s[3] = 0; return false; } // a normal-speed fill breaks the streak
		long fastest = Math.min(react ? since : TOO_FAST_MS, pair ? gap : TOO_FAST_MS);
		if (arrivedAt - s[4] > STRIKE_MS) s[3] = 0;
		s[4] = arrivedAt;
		int strikes = (int) ++s[3];
		if (fastest > IMPOSSIBLE_MS && strikes < 2) return false;
		Verdict.signal(pl, "inventory", "off hand filled " + fastest + " ms after " + (react && since <= gap || !pair ? "a totem popped" : "another slot was clicked") + (strikes > 1 ? ", " + strikes + " in a row" : ""), strikes >= 3 ? 3 : 2);
		return true;
	}

	private static long[] state(ServerPlayer pl) { return STATE.computeIfAbsent(pl.getUUID(), k -> new long[] { -1000000, 0, -1, 0, 0 }); }

	public static void forget(UUID id) { STATE.remove(id); }
}
