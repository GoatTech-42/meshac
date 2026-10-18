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
	private static final class H { final Ladder.Heat lh = new Ladder.Heat(), mon = new Ladder.Heat(); /* mon: heat of log-only signals, never feeds a removal */ int heat; long at; final ArrayDeque<String> trace = new ArrayDeque<>(); final java.util.LinkedHashMap<String, Integer> weight = new java.util.LinkedHashMap<>(); final Map<String, String> detail = new java.util.HashMap<>(); final Map<String, long[]> sigLog = new java.util.HashMap<>(); /* combat-only log/alert budget windows {windowStartNanos, used, suppressed} (0057); keys bounded by the fixed check vocabulary */ }
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
		if (cfg.monitor()) return signalLogOnly(pl, check, detail, weight); // monitor mode: detector signals never enforce, including legacy egregious entries
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

	/** Log-only signals: monitor-mode behaviour for a caller that must never enforce in ANY config.
	 * 0055 (parent decision 10-17, Luke standing autonomy): combat calls this BY CONSTRUCTION until the
	 * 100/300 ms FP replay + reviewer GO. Only the mon bucket moves (real mon heat for the rig); the removal
	 * ladder (lh, HOLD/REMOVE, cancel) never sees these signals. Always returns Step.LOG. */
	public static Step signalLogOnly(ServerPlayer pl, String check, String detail, int weight) {
		Config cfg = Config.get();
		if (cfg.skips(pl, check)) return Step.LOG; // same exemptions as signal()
		H h = HEAT.computeIfAbsent(pl.getUUID(), k -> new H());
		long ms = System.currentTimeMillis();
		int mh; long sup = 0; long winClose = 0; boolean emit = true;
		synchronized (h) {
			h.mon.add(check, weight, ms, cfg.ladderParams()); mh = (int) Math.round(h.mon.heat);
			h.trace.add(ms % 1_000_000 + " SIGNAL " + check + " " + detail + " heat=" + mh + " (log-only)"); if (h.trace.size() > 40) h.trace.poll();
			// 0057: the budget is COMBAT-ONLY - every other check family keeps the unthrottled monitor contract
			// (FP coverage + positive controls unchanged). 30 lines per 60 s per player, combat (PROPOSAL value,
			// reviewer-tunable); heat + trace still record every signal; only the line/alert is capped.
			if ("combat".equals(check)) {
				long now = System.nanoTime(); // monotonic: a wall-clock rollback must never reopen the budget
				long[] b = h.sigLog.computeIfAbsent(check, k -> new long[3]);
				if (b[0] == 0 || now - b[0] >= 60_000_000_000L) { // forward roll only: now < b[0] counts within the window, never reopens
					if (b[2] > 0) winClose = b[2]; // durable window-close summary: suppressed positives always reach the log
					b[0] = now; b[1] = 0; b[2] = 0;
				}
				if (b[1] >= 30) { b[2]++; emit = false; }
				else { b[1]++; sup = b[2]; b[2] = 0; }
			}
		}
		if (winClose > 0) Meshac.LOG.warn("[meshac] SIGNAL-SUPPRESSED {} {} count={} (window closed)", pl.getGameProfile().name(), check, winClose);
		if (!emit) return Step.LOG;
		Meshac.LOG.warn("[meshac] SIGNAL {} {} {} heat={} -> log{}", pl.getGameProfile().name(), check, detail, mh, sup > 0 ? " (suppressed " + sup + ")" : "");
		alert(pl, check + " " + detail + " heat " + mh + " log");
		return Step.LOG;
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

	public static void forget(UUID id) {
		H h = HEAT.remove(id);
		if (h != null) { // 0057: suppressed log-only positives must reach a durable log even when the player disconnects
			synchronized (h) {
				for (var e : h.sigLog.entrySet()) if (e.getValue()[2] > 0) Meshac.LOG.warn("[meshac] SIGNAL-SUPPRESSED {} {} count={} (disconnect flush)", id, e.getKey(), e.getValue()[2]);
			}
		}
	}
}
