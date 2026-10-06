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
 * kill-auras is one suspect, not two. Heat steps: below holdAt a setback (or the hit is denied), then hold, then removal at removeAt.
 * Removal climbs a ladder kept in the case store, so it survives rejoins: kicks first, then tempbans, then a permanent ban if the config allows it.
 */
public final class Verdict {
	public enum Step { SETBACK, HOLD, REMOVE }
	private static final class H { int heat; long at; final ArrayDeque<String> trace = new ArrayDeque<>(); final java.util.LinkedHashMap<String, Integer> weight = new java.util.LinkedHashMap<>(); final Map<String, String> detail = new java.util.HashMap<>(); }
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
		String why;
		synchronized (h) {
			if (ms - h.at > Config.get().heatCoolMs()) { h.weight.clear(); h.detail.clear(); }
			h.heat = ms - h.at > Config.get().heatCoolMs() ? weight : h.heat + weight;
			h.weight.merge(check, weight, Integer::sum); h.detail.put(check, detail); // heat cools after the configured clean time
			h.at = ms;
			step = h.heat >= Config.get().removeAt() ? Step.REMOVE : h.heat >= Config.get().holdAt() ? Step.HOLD : Step.SETBACK;
			h.trace.add(ms % 1_000_000 + " SIGNAL " + check + " " + detail + " heat=" + h.heat);
			ev = new ArrayList<>(h.trace);
			// The case names the check that carried most of the heat, not whichever one happened to land last: a held player hovering after a no-fall flag is a no-fall case.
			String top = check; int best = -1;
			for (var e : h.weight.entrySet()) if (e.getValue() > best) { best = e.getValue(); top = e.getKey(); }
			why = top + " " + h.detail.get(top);
			if (step == Step.REMOVE) { h.heat = 0; h.weight.clear(); h.detail.clear(); }
		}
		Meshac.LOG.warn("[meshac] SIGNAL {} {} {} heat={} -> {}", pl.getGameProfile().name(), check, detail, h.heat, step.name().toLowerCase());
		if (step == Step.HOLD && h.heat - weight < Config.get().holdAt()) Discord.hold(pl.getGameProfile().name(), check, detail);
		if (step == Step.REMOVE) remove(pl, why, ev);
		return step;
	}

	private static void remove(ServerPlayer pl, String why, List<String> evidence) {
		String reason = "Unusual " + why.split(" ")[0] + " (" + why + ")";
		Config cf = Config.get();
		int prior = Cases.offences(pl.getUUID()), idx = prior - cf.kicks();
		int[] ladder = cf.tempMinutes();
		String name = pl.getGameProfile().name();
		Cases.Case c;
		if (idx < 0) {
			c = Cases.add(name, pl.getUUID(), "kick", reason, "meshac", 0, evidence);
			pl.connection.disconnect(Screens.kick(reason, c));
		} else if (idx < ladder.length || !cf.perma()) {
			c = Cases.add(name, pl.getUUID(), "tempban", reason, "meshac", System.currentTimeMillis() + ladder[Math.min(idx, ladder.length - 1)] * 60_000L, evidence);
			pl.connection.disconnect(Screens.ban(c));
		} else {
			c = Cases.add(name, pl.getUUID(), "ban", reason, "meshac", 0, evidence);
			pl.connection.disconnect(Screens.ban(c));
		}
		Meshac.LOG.warn("[meshac] CASE {} {} {} {}", c.id, c.action, c.player, reason);
		Discord.post(c);
	}

	public static void forget(UUID id) { HEAT.remove(id); }
}
