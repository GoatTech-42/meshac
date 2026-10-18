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
	public enum Step { SETBACK, HOLD, REMOVE, LOG }
	private static final class H { final Ladder.Heat lh = new Ladder.Heat(), mon = new Ladder.Heat(); /* mon: heat of log-only signals, never feeds a removal */ int heat; long at; final ArrayDeque<String> trace = new ArrayDeque<>(); final java.util.LinkedHashMap<String, Integer> weight = new java.util.LinkedHashMap<>(); final Map<String, String> detail = new java.util.HashMap<>(); }
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
		Config cfg = Config.get();
		if (cfg.skips(pl, check)) return Step.LOG; // exempt player, exempt check, disabled check, creative, spectator: no heat, no action, from any caller
		H h = HEAT.computeIfAbsent(pl.getUUID(), k -> new H());
		long ms = System.currentTimeMillis();
		if (cfg.monitor()) { // monitor mode: detector signals never enforce, including legacy egregious entries
			int mh; synchronized (h) { h.mon.add(check, weight, ms, cfg.ladderParams()); mh = (int) Math.round(h.mon.heat); h.trace.add(ms % 1_000_000 + " SIGNAL " + check + " " + detail + " heat=" + mh + " (log-only)"); if (h.trace.size() > 40) h.trace.poll(); }
			Meshac.LOG.warn("[meshac] SIGNAL {} {} {} heat={} -> log", pl.getGameProfile().name(), check, detail, mh);
			alert(pl, check + " " + detail + " heat " + mh + " log");
			return Step.LOG;
		}
		int heatNow;
		Step step;
		List<String> ev;
		String why;
		synchronized (h) {
			if (h.lh.now(ms, cfg.ladderParams()) < 0.25) { h.weight.clear(); h.detail.clear(); } // cooled off: the case must name the check of THIS build-up, not one from an hour ago
			Ladder.Step ls = h.lh.add(check, weight, ms, Config.get().ladderParams());
			h.heat = (int) Math.round(h.lh.heat); h.at = ms; heatNow = h.heat;
			h.weight.merge(check, weight, Integer::sum); h.detail.put(check, detail);
			step = ls == Ladder.Step.REMOVE ? Step.REMOVE : ls == Ladder.Step.HOLD ? Step.HOLD : Step.SETBACK;
			h.trace.add(ms % 1_000_000 + " SIGNAL " + check + " " + detail + " heat=" + h.heat);
			ev = new ArrayList<>(h.trace);
			// The case names the check that carried most of the heat, not whichever one happened to land last: a held player hovering after a no-fall flag is a no-fall case.
			String top = check; int best = -1;
			for (var e : h.weight.entrySet()) if (e.getValue() > best) { best = e.getValue(); top = e.getKey(); }
			why = top + " " + h.detail.get(top);
			if (step == Step.REMOVE) { h.heat = 0; h.lh.reset(); h.mon.reset(); h.weight.clear(); h.detail.clear(); }
		}
		Meshac.LOG.warn("[meshac] SIGNAL {} {} {} heat={} -> {}", pl.getGameProfile().name(), check, detail, heatNow, step.name().toLowerCase());
		alert(pl, check + " " + detail + " heat " + heatNow + " " + step.name().toLowerCase());
		if (step == Step.HOLD && heatNow - weight < cfg.holdAtI()) Discord.hold(pl.getGameProfile().name(), check, detail);
		if (step == Step.REMOVE) remove(pl, why, ev);
		return step;
	}

	/** Staff who ran /mesh watch see every flag as it happens. */
	public static final java.util.Set<UUID> WATCH = ConcurrentHashMap.newKeySet();
	private static void alert(ServerPlayer pl, String text) {
		if (WATCH.isEmpty()) return;
		var srv = pl.level().getServer();
		for (UUID u : WATCH) { ServerPlayer w = srv.getPlayerList().getPlayer(u); if (w != null) w.sendSystemMessage(net.minecraft.network.chat.Component.literal("[meshac] " + pl.getGameProfile().name() + " " + text)); }
	}
	/** One line for /mesh status. */
	public static String status(ServerPlayer pl) {
		H h = HEAT.get(pl.getUUID()); Config cf = Config.get();
		double eff = Cases.offences(pl.getUUID()); int rung = Ladder.rung(eff, cf.kicks(), false);
		String next = rung <= cf.kicks() ? "kick" : (cf.explicitLadder() ? cf.tempMinutes()[Math.min(rung - cf.kicks() - 1, cf.tempMinutes().length - 1)] : Ladder.tempMinutes(rung, cf.kicks(), cf.tempBase(), cf.tempGrowth(), cf.tempMax())) + " min tempban";
		return pl.getGameProfile().name() + ": heat " + (h == null ? "0.0" : String.format("%.1f", h.lh.now(System.currentTimeMillis(), cf.ladderParams()))) + ", offences " + String.format("%.2f", eff) + ", next removal: " + next + (cf.monitor() ? " (monitor mode, no removals)" : "");
	}

	private static void remove(ServerPlayer pl, String why, List<String> evidence) {
  if (!Config.get().enforce(why.split(" ")[0])) { Meshac.LOG.warn("[meshac] MONITOR {} would be removed: {}", pl.getGameProfile().name(), why); return; }
		String reason = "Unusual " + why.split(" ")[0] + " (" + why + ")";
		Config cf = Config.get();
		H hh = HEAT.get(pl.getUUID()); boolean tierA = hh != null && hh.lh.confirmedTierA();
		double eff = Cases.offences(pl.getUUID());
		int rung = Ladder.rung(eff, cf.kicks(), tierA && cf.skipRungOnTierA());
		String name = pl.getGameProfile().name();
		Cases.Case c;
		if (rung <= cf.kicks()) {
			c = Cases.add(name, pl.getUUID(), "kick", reason, "meshac", 0, evidence);
			pl.connection.disconnect(Screens.kick(reason, c));
		} else if (!Ladder.permanent(cf.perma(), tierA || !cf.permanentNeedsTierA(), rung, cf.kicks() + 4)) {
			long mins = cf.explicitLadder() ? cf.tempMinutes()[Math.min(rung - cf.kicks() - 1, cf.tempMinutes().length - 1)] : Ladder.tempMinutes(rung, cf.kicks(), cf.tempBase(), cf.tempGrowth(), cf.tempMax());
			c = Cases.add(name, pl.getUUID(), "tempban", reason, "meshac", System.currentTimeMillis() + mins * 60_000L, evidence);
			pl.connection.disconnect(Screens.ban(c));
		} else {
			c = Cases.add(name, pl.getUUID(), "ban", reason, "meshac", 0, evidence);
			pl.connection.disconnect(Screens.ban(c));
		}
		Meshac.LOG.warn("[meshac] CASE {} {} {} {}", c.id, c.action, c.player, reason);
		Discord.post(c);
	}

	public static void clearPendingActions() { HEAT.clear(); }

	public static void forget(UUID id) { HEAT.remove(id); }
}
