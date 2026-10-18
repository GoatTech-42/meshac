package dev.meshac;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * MassTpa, ForceOP and other command sweeps: the same command root over and over with a different argument each time, on a metronome.
 * MassTpa sends "/tpa <player>" for every player every N ticks (default 20); ForceOP sends "/login <password>" at a fixed delay.
 * A hand, a paste or a command block macro is irregular: 8 commands in 20 s with the same root, all different arguments, and gaps that
 * differ by under 60 ms from each other is not a hand. Only a signal, nothing is refused (commands are not ours to block).
 * Lag assumption: gaps are measured between arrivals on the main thread, and a burst compresses gaps to ~0 which makes the spread LARGE vs the mean
 * (a burst has a few 0 gaps and some long ones), so bursts do not look metronomic.
 */
public final class Chatter {
	private static final class S { final ArrayDeque<long[]> q = new ArrayDeque<>(); String root = ""; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MIN_COMMANDS = 8;
	private static final long WINDOW_MS = 20000, MAX_SPREAD_MS = 60;

	/** Runs on the network thread (vanilla handles chat commands off the main thread, so arrival times here are the real ones). Returns the signal text, or null; the caller signals on the main thread. */
	public static String command(ServerPlayer pl, String line) {
		String t = line.startsWith("/") ? line.substring(1) : line;
		int sp = t.indexOf(' ');
		String root = (sp < 0 ? t : t.substring(0, sp)).toLowerCase();
		long arg = sp < 0 ? 0 : t.substring(sp + 1).trim().hashCode(); // only a hash of the argument is kept, passwords are never stored
		long now = System.currentTimeMillis();
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		if (!root.equals(s.root)) { s.q.clear(); s.root = root; }
		while (!s.q.isEmpty() && now - s.q.peekFirst()[0] > WINDOW_MS) s.q.pollFirst();
		s.q.addLast(new long[] {now, arg});
		if (s.q.size() < MIN_COMMANDS) return null;
		java.util.HashSet<Long> args = new java.util.HashSet<>(); long prev = 0, min = Long.MAX_VALUE, max = 0; boolean first = true;
		for (long[] e : s.q) { args.add(e[1]); if (!first) { long g = e[0] - prev; min = Math.min(min, g); max = Math.max(max, g); } prev = e[0]; first = false; }
		if (args.size() == s.q.size() && max - min <= MAX_SPREAD_MS && max >= 150) { // gaps under 150 ms are a burst, not a cadence
			s.q.clear();
			return "ran /" + root + " " + MIN_COMMANDS + " times with different arguments on a steady beat of about " + max + " ms";
		}
		return null;
	}
	public static void forget(UUID id) { STATE.remove(id); }
}
