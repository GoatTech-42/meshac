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
	private static final class H { final Ladder.Heat lh = new Ladder.Heat(), mon = new Ladder.Heat(); /* mon: heat of log-only signals, never feeds a removal */ int heat; long at; final ArrayDeque<String> trace = new ArrayDeque<>(); final java.util.LinkedHashMap<String, Integer> weight = new java.util.LinkedHashMap<>(); final Map<String, String> detail = new java.util.HashMap<>(); final Map<String, long[]> sigLog = new java.util.HashMap<>(); /* combat-only log/alert budget windows {windowStartNanos, used, suppressed} (0057); keys bounded by the fixed check vocabulary */ String name; /* 0059: stable grader identity on every suppression receipt */ }
	private static final Map<UUID, H> HEAT = new ConcurrentHashMap<>();

	/** Cheap per-event note kept in a ring so a case can carry the lead-up. */
	public static void note(ServerPlayer pl, String line) {
		synchronized (Config.POLICY_LOCK) { // trace lines feed case evidence; no HEAT access may interleave with a policy commit's wipe+publish
			H h = HEAT.computeIfAbsent(pl.getUUID(), k -> new H());
			synchronized (h) { h.trace.add(System.currentTimeMillis() % 1_000_000 + " " + line); if (h.trace.size() > 40) h.trace.poll(); }
		}
	}

	/** A check fired. Returns the step to take; REMOVE has already created the case and disconnected the player. */
	public static Step signal(ServerPlayer pl, String check, String detail) { return signal(pl, check, detail, 1); }
	/** Weight 2 is for hits: a landed attack is worth more heat than one odd move packet. */
	public static Step signal(ServerPlayer pl, String check, String detail, int weight) {
		synchronized (Config.POLICY_LOCK) { return signalLocked(pl, check, detail, weight); } // the decision and every effect dispatched INSIDE Verdict are atomic against a policy commit (0091). NARROW CLAIM (0092): the RETURNED step is acted on by the caller after the lock is released - outer cancel/setback/refusal actions are NOT covered
	}
	private static Step signalLocked(ServerPlayer pl, String check, String detail, int weight) {
		Config cfg = Config.current().cfg; // the published policy snapshot, read ONCE; it cannot change while the lock is held
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
			Ladder.Step ls = h.lh.add(check, weight, ms, cfg.ladderParams());
			h.heat = (int) Math.round(h.lh.heat); h.at = ms; heatNow = h.heat;
			if (ls != Ladder.Step.LOG) { h.weight.merge(check, weight, Integer::sum); h.detail.put(check, detail); } // an invalid-policy flag leaves NO enforcement evidence (weight/detail feed the case build-up)
			step = ls == Ladder.Step.REMOVE ? Step.REMOVE : ls == Ladder.Step.HOLD ? Step.HOLD : ls == Ladder.Step.LOG ? Step.LOG : Step.SETBACK;
			h.trace.add(ms % 1_000_000 + " SIGNAL " + check + " " + detail + " heat=" + h.heat + (ls == Ladder.Step.LOG ? " (invalid policy: logged, no action)" : ""));
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
		if (step == Step.REMOVE) remove(pl, why, ev, cfg); // still inside POLICY_LOCK: the removal cannot dispatch after a newer policy has committed
		return step;
	}

	/** 0059: ONE shape for every suppression receipt - name + uuid + check + count + reason, stable for graders. */
	private static final String SUPPRESSED_FMT = "[meshac] SIGNAL-SUPPRESSED {} {} {} count={} ({})";
	/** Durable receipt for counts the budget held back: flushes every pending bucket for one player. Never budgeted itself; cadence-bounded by its callers (window roll, tick, clear, disconnect). */
	private static void flushSuppressed(UUID id, H h, String why) {
		synchronized (h) {
			for (var e : h.sigLog.entrySet()) if (e.getValue()[2] > 0) { Meshac.LOG.warn(SUPPRESSED_FMT, h.name, id, e.getKey(), e.getValue()[2], why); e.getValue()[2] = 0; }
		}
	}
	/** Server-tick pump (0059): an idle-but-connected player never emits a combat signal, so window close cannot live only inside signalLogOnly. Bounded: at most one summary per expired window per player per tick. */
	public static void tickFlush(ServerPlayer pl) {
		H h = HEAT.get(pl.getUUID()); if (h == null) return;
		long now = System.nanoTime();
		synchronized (h) { for (var e : h.sigLog.entrySet()) { long[] b = e.getValue(); if (b[2] > 0 && now - b[0] >= 60_000_000_000L) { Meshac.LOG.warn(SUPPRESSED_FMT, h.name, pl.getUUID(), e.getKey(), b[2], "window closed (idle, tick flush)"); b[2] = 0; b[0] = now; b[1] = 0; } } } // 0063: roll + receipt atomic per bucket, inside the lock
	}
	/** Log-only signals: monitor-mode behaviour for a caller that must never enforce in ANY config.
	 * 0055 (parent decision 10-17, Luke standing autonomy): combat calls this BY CONSTRUCTION until the
	 * 100/300 ms FP replay + reviewer GO. Only the mon bucket moves (real mon heat for the rig); the removal
	 * ladder (lh, HOLD/REMOVE, cancel) never sees these signals. Always returns Step.LOG. */
	public static Step signalLogOnly(ServerPlayer pl, String check, String detail, int weight) {
		synchronized (Config.POLICY_LOCK) { // the mon-heat mutation and its log/alert effects cannot be split by a policy commit (reentrant when reached from signalLocked)
			Config cfg = Config.current().cfg; // published snapshot, read once; cannot change while the lock is held
			if (cfg.skips(pl, check)) return Step.LOG; // same exemptions as signal()
			H h = HEAT.computeIfAbsent(pl.getUUID(), k -> new H());
			h.name = pl.getGameProfile().name(); // 0059: stable identity for suppression receipts
			long ms = System.currentTimeMillis();
			int mh; boolean emit = true;
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
					if (b[2] > 0) Meshac.LOG.warn(SUPPRESSED_FMT, h.name, pl.getUUID(), check, b[2], "window closed"); // 0063: emitted INSIDE the lock, atomic with the roll - the 0059/0061 lock-release gap is closed
					b[0] = now; b[1] = 0; b[2] = 0;
				}
				if (b[1] >= 30) { b[2]++; emit = false; } // a post-roll window is at used=0, so a rolled signal always emits
				else { b[1]++; }
			}
		}
			if (!emit) return Step.LOG;
			Meshac.LOG.warn("[meshac] SIGNAL {} {} {} heat={} -> log", pl.getGameProfile().name(), check, detail, mh);
			alert(pl, check + " " + detail + " heat " + mh + " log");
			return Step.LOG;
		}
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
		H h = HEAT.get(pl.getUUID()); Config cf = Config.current().cfg; // consistent read of the published snapshot
		double eff = Cases.offences(pl.getUUID()); int rung = Ladder.rung(eff, cf.kicks(), false);
		String next = rung <= cf.kicks() ? "kick" : (cf.explicitLadder() ? cf.tempMinutes()[Math.min(rung - cf.kicks() - 1, cf.tempMinutes().length - 1)] : Ladder.tempMinutes(rung, cf.kicks(), cf.tempBase(), cf.tempGrowth(), cf.tempMax())) + " min tempban";
		return pl.getGameProfile().name() + ": heat " + (h == null ? "0.0" : String.format("%.1f", h.lh.now(System.currentTimeMillis(), cf.ladderParams()))) + ", offences " + String.format("%.2f", eff) + ", next removal: " + next + (cf.monitor() ? " (monitor mode, no removals)" : "");
	}

	private static void remove(ServerPlayer pl, String why, List<String> evidence, Config cf) { // cf is the decision's published snapshot; the caller holds POLICY_LOCK, so it is still current. Screens/Discord read Config.get() internally - identical under the lock
  if (!cf.enforce(why.split(" ")[0])) { Meshac.LOG.warn("[meshac] MONITOR {} would be removed: {}", pl.getGameProfile().name(), why); return; }
		String reason = "Unusual " + why.split(" ")[0] + " (" + why + ")";
		H hh = HEAT.get(pl.getUUID()); boolean tierA = hh != null && hh.lh.confirmedTierA(cf.ladderParams());
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

	public static void clearPendingActions() { // 0059: monitor on/off used to silently drop pending suppressed counts - flush first
		for (var e : HEAT.entrySet()) flushSuppressed(e.getKey(), e.getValue(), "pending-actions cleared (monitor toggle)");
		HEAT.clear();
	}

	public static void forget(UUID id) {
		H h = HEAT.remove(id);
		if (h != null) flushSuppressed(id, h, "disconnect flush"); // 0057: suppressed positives reach the log even when the player disconnects
	}
}
