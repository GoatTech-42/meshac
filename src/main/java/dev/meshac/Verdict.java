package dev.meshac;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * One place decides what a signal costs. Heat is shared by every check, so a player who flies and then
 * kill-auras is one suspect, not two. Ladder: 1-2 setback (or the hit is denied), 3-4 hold, 5+ removal.
 * Removal is a kick the first time and an escalating tempban after that (see Config.tempbanMinutes).
 */
public final class Verdict {
	public enum Step { SETBACK, HOLD, REMOVE }
	private static final class H { int heat; long at; final ArrayDeque<String> trace = new ArrayDeque<>(); }
	private static final Map<UUID, H> HEAT = new ConcurrentHashMap<>();

	/** Cheap per-event note kept in a ring so a case can carry the lead-up. */
	public static void note(ServerPlayer pl, String line) {
		H h = HEAT.computeIfAbsent(pl.getUUID(), k -> new H());
		synchronized (h) { h.trace.add(System.currentTimeMillis() % 1_000_000 + " " + line); if (h.trace.size() > 40) h.trace.poll(); }
	}

	/** A check fired. Returns the step to take; REMOVE has already created the case and disconnected the player. */
	public static Step signal(ServerPlayer pl, String check, String detail) { return signal(pl, check, detail, 1); }
	/** Weight 2 is for hits: a landed attack is worth more heat than one odd move packet. */
	public static Step signal(ServerPlayer pl, String check, String detail, int weight) {
		H h = HEAT.computeIfAbsent(pl.getUUID(), k -> new H());
		long ms = System.currentTimeMillis();
		Step step;
		List<String> ev;
		synchronized (h) {
			h.heat = ms - h.at > 10_000 ? weight : h.heat + weight; // heat cools after 10 s clean
			h.at = ms;
			step = h.heat >= 5 ? Step.REMOVE : h.heat >= 3 ? Step.HOLD : Step.SETBACK;
			h.trace.add(ms % 1_000_000 + " SIGNAL " + check + " " + detail + " heat=" + h.heat);
			ev = new ArrayList<>(h.trace);
			if (step == Step.REMOVE) h.heat = 0;
		}
		Meshac.LOG.warn("[meshac] SIGNAL {} {} {} heat={} -> {}", pl.getGameProfile().name(), check, detail, h.heat, step.name().toLowerCase());
		if (step == Step.REMOVE) remove(pl, check + " " + detail, ev);
		return step;
	}

	private static void remove(ServerPlayer pl, String why, List<String> evidence) {
		String reason = "Unusual " + why.split(" ")[0] + " (" + why + ")";
		int prior = Cases.offences(pl.getUUID());
		int[] ladder = Config.get().tempbanMinutes;
		Cases.Case c;
		if (prior == 0) { // first removal is a kick
			c = Cases.add(pl.getGameProfile().name(), pl.getUUID(), "kick", reason, "meshac", 0, evidence);
			pl.connection.disconnect(Screens.kick(reason, c));
		} else if (prior - 1 < ladder.length) {
			c = Cases.add(pl.getGameProfile().name(), pl.getUUID(), "tempban", reason, "meshac", System.currentTimeMillis() + ladder[prior - 1] * 60_000L, evidence);
			pl.connection.disconnect(Screens.ban(c));
		} else {
			c = Cases.add(pl.getGameProfile().name(), pl.getUUID(), "ban", reason, "meshac", 0, evidence);
			pl.connection.disconnect(Screens.ban(c));
		}
		Meshac.LOG.warn("[meshac] CASE {} {} {} {}", c.id, c.action, c.player, reason);
		Discord.post(c);
	}

	public static void forget(UUID id) { HEAT.remove(id); }
}
