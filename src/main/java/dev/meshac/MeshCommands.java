package dev.meshac;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /mesh for staff: cases, case, pardon, unban, ban, kick, status, watch, monitor, veil, check, reload. */
public final class MeshCommands {
	private static final CallerCommandBudget CAPTURE_DUMPS = new CallerCommandBudget();
	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("mesh").requires(Perm.admin())
			.then(Commands.literal("cases").executes(c -> list(c.getSource(), Cases.recent(10)))
				.then(Commands.argument("player", StringArgumentType.word()).executes(c -> list(c.getSource(), Cases.of(StringArgumentType.getString(c, "player"))))))
			.then(Commands.literal("case").then(Commands.argument("id", StringArgumentType.word()).executes(c -> show(c.getSource(), StringArgumentType.getString(c, "id")))))
			.then(Commands.literal("pardon").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
				boolean ok = Cases.pardon(StringArgumentType.getString(c, "id")); audit(c.getSource(), "pardon", StringArgumentType.getString(c, "id") + (ok ? " ok" : " not found"));
				c.getSource().sendSuccess(() -> Component.literal(ok ? "Pardoned." : "No such case."), true); return ok ? 1 : 0; })))
			.then(Commands.literal("unban").then(Commands.argument("player", StringArgumentType.word()).executes(c -> {
				int n = Cases.unban(StringArgumentType.getString(c, "player")); audit(c.getSource(), "unban", StringArgumentType.getString(c, "player") + " lifted " + n);
				c.getSource().sendSuccess(() -> Component.literal(n > 0 ? "Lifted " + n + " ban(s)." : "No active ban."), true); return n; })))
			.then(Commands.literal("ban").then(Commands.argument("player", StringArgumentType.word())
				.then(Commands.argument("duration", StringArgumentType.word()).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> ban(c.getSource(),
					StringArgumentType.getString(c, "player"), StringArgumentType.getString(c, "duration"), StringArgumentType.getString(c, "reason")))))))
			.then(Commands.literal("status").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
				ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"); String s = Verdict.status(t); audit(c.getSource(), "status.view", t.getGameProfile().name());
				c.getSource().sendSuccess(() -> Component.literal(s), false); return 1; })))
			.then(Commands.literal("capture").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player())
				.then(Commands.literal("start").executes(c -> capture(c.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"), "start", 0)))
				.then(Commands.literal("clear").executes(c -> capture(c.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"), "clear", 0)))
				.then(Commands.literal("dump").then(Commands.argument("page", IntegerArgumentType.integer(1, 32)).executes(c -> capture(c.getSource(), net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"), "dump", IntegerArgumentType.getInteger(c, "page") - 1))))))
			.then(Commands.literal("watch").executes(c -> {
				ServerPlayer me = c.getSource().getPlayer(); if (me == null) { c.getSource().sendFailure(Component.literal("Players only.")); return 0; }
				boolean on = Verdict.WATCH.add(me.getUUID()); if (!on) Verdict.WATCH.remove(me.getUUID());
				c.getSource().sendSuccess(() -> Component.literal(on ? "Watching flags." : "Stopped watching."), false); return 1; }))
			.then(Commands.literal("monitor").executes(c -> { c.getSource().sendSuccess(() -> Component.literal("Monitor mode is " + (Config.get().monitor() ? "on" : "off") + "."), false); return 1; })
				.then(Commands.literal("on").executes(c -> { boolean was; synchronized (Config.POLICY_LOCK) { was = Config.get().monitor(); Config.get().monitorOnly = true; Movement.clearPendingActions(); Verdict.clearPendingActions(); } /* the toggle is a policy commit: same atomicity contract as a reload - no decision straddles it (0091) */ audit(c.getSource(), "monitor", AdminLog.change(was ? "on" : "off", "on")); c.getSource().sendSuccess(() -> Component.literal("Monitor mode on: detector flags log only; no detector actions."), true); return 1; }))
				.then(Commands.literal("off").executes(c -> { boolean was; synchronized (Config.POLICY_LOCK) { was = Config.get().monitor(); Config.get().monitorOnly = false; } audit(c.getSource(), "monitor", AdminLog.change(was ? "on" : "off", "off")); c.getSource().sendSuccess(() -> Component.literal("Monitor mode off."), true); return 1; })))
			.then(Commands.literal("kick").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> {
				ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"); String why = StringArgumentType.getString(c, "reason");
				Cases.Case cs = Cases.add(t.getGameProfile().name(), t.getUUID(), "kick", why, c.getSource().getTextName(), 0, List.of("Staff kick")); audit(c.getSource(), "kick", t.getGameProfile().name() + " case " + cs.id + " reason " + why); t.connection.disconnect(Screens.kick(why, cs));
				c.getSource().sendSuccess(() -> Component.literal("Kicked " + t.getGameProfile().name() + ", case " + cs.id), true); return 1; }))))
			.then(Commands.literal("veil")
				.executes(c -> { c.getSource().sendSuccess(() -> Component.literal(veilLine()), false); if (Config.get().veilEnabled() && dev.meshac.veil.Veil.espOn) c.getSource().sendSuccess(() -> Component.literal(dev.meshac.veil.Veil.statusLine()), false); return 1; })
				.then(Commands.literal("xray").then(Commands.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> {
					boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "on"); boolean was = Config.get().veilEnabled() && dev.meshac.veil.Veil.xrayOn; boolean effective = Config.get().veilEnabled() && on; dev.meshac.veil.Veil.xrayOn = effective; dev.meshac.veil.Veil.clear(); audit(c.getSource(), "veil.xray", AdminLog.change(was ? "on" : "off", effective ? "on" : "off"));
					c.getSource().sendSuccess(() -> Component.literal("Anti-xray " + (effective ? "on" : "off") + (on && !effective ? " (veilEnabled is false; enable it in config and reload first)" : "") + " until reload or restart (config veilXray is unchanged). Players see it on the next chunk they load."), true); return 1; })))
				.then(Commands.literal("esp").then(Commands.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> {
					boolean want = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "on"); boolean was = dev.meshac.veil.Veil.espOn; String bad = dev.meshac.veil.Veil.setEsp(want); audit(c.getSource(), "veil.esp", AdminLog.change(was ? "on" : "off", dev.meshac.veil.EspSwitch.audit(want, bad)));
					c.getSource().sendSuccess(() -> Component.literal(dev.meshac.veil.EspSwitch.message(want, bad)), true); return 1; })))
				.then(Commands.literal("off").executes(c -> { String was = "xray " + (dev.meshac.veil.Veil.xrayOn ? "on" : "off") + " esp " + (dev.meshac.veil.Veil.espOn ? "on" : "off"); dev.meshac.veil.Veil.xrayOn = false; dev.meshac.veil.Veil.espOn = false; dev.meshac.veil.Veil.clear(); audit(c.getSource(), "veil.off", AdminLog.change(was, "xray off esp off"));
					c.getSource().sendSuccess(() -> Component.literal("Chunkveil fully off until reload or restart."), true); return 1; })))
			.then(Commands.literal("check")
				.executes(c -> { var l = Config.get().disabledChecks; c.getSource().sendSuccess(() -> Component.literal("Disabled checks: " + (l == null || l.isEmpty() ? "none" : String.join(", ", l))), false); return 1; })
				.then(Commands.argument("name", StringArgumentType.word()).then(Commands.literal("off").executes(c -> setCheck(c.getSource(), StringArgumentType.getString(c, "name"), false)))
					.then(Commands.literal("on").executes(c -> setCheck(c.getSource(), StringArgumentType.getString(c, "name"), true)))))
			.then(Commands.literal("reload").executes(c -> { AdminLog.ReloadResult read = Config.load(); Config.applyVeil(); Cases.load(); audit(c.getSource(), "reload", AdminLog.reloadDetail(read, Config.get().monitor(), Config.get().disabledChecks == null ? 0 : Config.get().disabledChecks.size(), new dev.meshac.veil.VeilSettings(Config.get().veilEnabled, Config.get().veilXray, Config.get().veilEsp, Config.get().veilBedrock).describe())); Config.get().warnUnknownChecks(); c.getSource().sendSuccess(() -> Component.literal(read.feedback()), true); return read.applied() ? 1 : 0; })));
	}


	/** Staff-only command inherited from the /mesh root. Replies only to the caller, never broadcasts. */
	private static int capture(CommandSourceStack source, ServerPlayer target, String action, int page) {
		if (!source.getServer().isSameThread()) throw new IllegalStateException("Capture command requires server thread");
		long now = System.nanoTime();
		if (action.equals("dump")) {
			ServerPlayer caller = source.getPlayer();
			String key = caller == null ? "console-rcon-shared" : caller.getUUID().toString();
			if (!CAPTURE_DUMPS.allow(key, now)) { source.sendFailure(Component.literal("Capture dump limit: four pages per caller per minute. Console/RCON share a budget.")); return 0; }
		}
		if (action.equals("start")) {
			boolean ok = Combat.RECORDER.start(target.getUUID(), now);
			audit(source, "capture.start", target.getGameProfile().name() + " " + (ok ? "started" : "refused (four already retained)")); // a start REPLACES any previous capture: destructive, so even a refusal is logged
			source.sendSuccess(() -> Component.literal(ok ? "Combat capture started: 120 seconds, 512 hits, RAM only. Restart replaces this player's previous capture. No verdicts." : "Four captures already retained. Clear one first."), false);
			return ok ? 1 : 0;
		}
		if (action.equals("clear")) {
			Combat.RECORDER.clear(target.getUUID());
			audit(source, "capture.clear", target.getGameProfile().name());
			source.sendSuccess(() -> Component.literal("Combat capture cleared."), false); return 1;
		}
		CombatRecorder.Page result = Combat.RECORDER.page(target.getUUID(), page, now);
		audit(source, "capture.dump", target.getGameProfile().name() + " page " + (page + 1)); // a dump DISCLOSES retained combat evidence to the caller: log who read it. A budget-refused dump changes and discloses nothing, so it is not audited.
		source.sendSuccess(() -> Component.literal("Combat capture exists=" + result.exists() + " expired=" + result.expired() + " total=" + result.total() + " overwritten=" + result.overwritten() + " retained=" + result.retained() + " page=" + (page + 1) + "/" + result.pages() + ". Descriptive evidence, not a verdict. Disconnect clears it."), false);
		for (CombatRecorder.Hit hit : result.hits()) source.sendSuccess(() -> Component.literal(hit.toString()), false);
		return result.hits().size();
	}

	/** Persistent one-line record of a staff change, in the server log. Feedback to ops is not enough: a gamerule can silence it. */
	private static void audit(CommandSourceStack s, String action, String detail) { Meshac.LOG.info(AdminLog.line(s.getTextName(), action, detail)); }
	private static String veilLine() { return "chunkveil (stale plan rebuilds " + dev.meshac.veil.Veil.staleRebuilds + ", neighbour " + dev.meshac.veil.Veil.neighbourRebuilds + ", border " + dev.meshac.veil.Veil.borderRebuilds + ", chest/spawner entries stripped " + dev.meshac.veil.Veil.beStripped + ", size mismatches " + dev.meshac.veil.Veil.beSizeMismatch + "): anti-xray " + (Config.get().veilEnabled() && dev.meshac.veil.Veil.xrayOn ? "ON" : "off") + ", anti-ESP " + (Config.get().veilEnabled() && dev.meshac.veil.Veil.espOn ? "ON" : "off") + ", master veilEnabled=" + Config.get().veilEnabled() + ", config veilXray=" + Config.get().veilXray + " veilEsp=" + Config.get().veilEsp; }
	/** Runtime switch for one sub-check (same list as config disabledChecks). Not written to disk; /mesh reload restores the config's list. */
	private static int setCheck(CommandSourceStack s, String name, boolean on) {
		if (!Config.knownCheck(name)) { s.sendFailure(Component.literal("No check called " + name + ". Known: " + String.join(", ", new java.util.TreeSet<>(Config.KNOWN_CHECKS)))); return 0; }
		var l = Config.get().disabledChecks; if (l == null) { l = new java.util.ArrayList<>(); Config.get().disabledChecks = l; }
		final boolean wasOn = l.stream().noneMatch(d -> d != null && d.equalsIgnoreCase(name));
		l.removeIf(d -> d != null && d.equalsIgnoreCase(name));
		if (!on) l.add(name);
		audit(s, "check." + name, AdminLog.change(wasOn ? "enabled" : "disabled", on ? "enabled" : "disabled"));
		s.sendSuccess(() -> Component.literal("Check " + name + (on ? " enabled." : " disabled until reload or restart: no flags, no heat, no setbacks or refusals from it for anyone.") + ""), true); return 1;
	}
	private static int list(CommandSourceStack s, List<Cases.Case> cs) {
		if (cs.isEmpty()) { s.sendSuccess(() -> Component.literal("No cases."), false); return 0; }
		for (Cases.Case c : cs) s.sendSuccess(() -> Component.literal(c.id + "  " + c.action + "  " + c.player + "  " + c.reason + (c.pardoned ? "  (pardoned)" : c.lifted ? "  (lifted)" : "")), false);
		return cs.size();
	}
	private static int show(CommandSourceStack s, String id) {
		Cases.Case c = Cases.find(id);
		audit(s, "case.view", id + (c == null ? " not found" : " for " + c.player));
		if (c == null) { s.sendSuccess(() -> Component.literal("No such case."), false); return 0; }
		s.sendSuccess(() -> Component.literal(c.id + " " + c.action + " " + c.player + "\n" + c.reason + "\nEvidence:\n" + String.join("\n", c.evidence)), false);
		return 1;
	}
	/** Staff ban: minutes 0 = permanent. Works on offline players only if they have joined before (name lookup via the profile cache). */
	/** Duration: 0 = permanent; plain number = minutes; or number plus s, m, h/hr, d, w (case-insensitive). Returns millis, or -1 if invalid. */
	static long parseDuration(String t) {
		java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{1,9})\\s*(s|sec|secs|m|min|mins|h|hr|hrs|d|day|days|w|wk|wks)?").matcher(t.trim().toLowerCase(java.util.Locale.ROOT));
		if (!m.matches()) return -1;
		long n = Long.parseLong(m.group(1)); String u = m.group(2) == null ? "m" : m.group(2);
		long unit = switch (u.charAt(0)) { case 's' -> 1000L; case 'h' -> 3_600_000L; case 'd' -> 86_400_000L; case 'w' -> 604_800_000L; default -> 60_000L; };
		return n * unit;
	}
	private static int ban(CommandSourceStack s, String name, String duration, String reason) {
		long ms = parseDuration(duration);
		if (ms < 0) { s.sendFailure(Component.literal("Usage: /mesh ban <player> <duration> <reason>. Duration: 0 = permanent, or 30s, 10m, 3hr, 1d, 2w (plain number = minutes).")); return 0; }
		int minutes = ms == 0 ? 0 : 1;
		ServerPlayer p = s.getServer().getPlayerList().getPlayerByName(name);
		if (p == null) return banOffline(s, name, reason, ms, minutes);
		Cases.Case c = Cases.add(p.getGameProfile().name(), p.getUUID(), minutes == 0 ? "ban" : "tempban", reason, s.getTextName(), ms == 0 ? 0 : System.currentTimeMillis() + ms, List.of("staff action"));
		audit(s, c.action, c.player + " case " + c.id + (ms == 0 ? " permanent" : " " + duration) + " reason " + reason);
		p.connection.disconnect(Screens.ban(c));
		Discord.post(c);
		s.sendSuccess(() -> Component.literal("Banned " + name + ", case " + c.id), true);
		return 1;
	}

	/**
	 * Offline ban. Resolves name to UUID from the server's own name cache (players who have joined), else from this plugin's case history. Never invents a UUID:
	 * an offline-mode UUID would be wrong on an online-mode server and the ban would silently never match.
	 * Lookup is SeenNames (usercache.json read-only, then case history); no Minecraft resolver API, so no per-version differences.
	 * Join enforcement already runs Cases.activeBan(uuid) in Meshac's JOIN handler, so a case with the right UUID is enough.
	 */
	private static int banOffline(CommandSourceStack s, String name, String reason, long ms, int minutes) {
		java.util.UUID id = null; String real = name;
		// Read-only: vanilla nameToIdCache().get(name) invents and stores an offline UUID for unknown names, so it is not used here.
		java.util.List<SeenNames.Past> past = new java.util.ArrayList<>();
		for (Cases.Case k : Cases.of(name)) past.add(new SeenNames.Past(k.player, k.uuid));
		SeenNames.Hit hit = SeenNames.find(net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("usercache.json"), past, name);
		if (hit != null) { id = hit.id(); real = hit.name(); }
		if (id == null) { s.sendFailure(Component.literal("Never seen " + name + " on this server (not in the name cache or case history), so there is no UUID to ban. Ask them to join once, or check the spelling.")); return 0; }
		if (Cases.activeBan(id) != null) { s.sendFailure(Component.literal(real + " already has an active ban.")); return 0; }
		final String shown = real;
		Cases.Case c = Cases.add(shown, id, minutes == 0 ? "ban" : "tempban", reason, s.getTextName(), ms == 0 ? 0 : System.currentTimeMillis() + ms, List.of("staff action (offline)"));
		audit(s, c.action, shown + " case " + c.id + " offline" + (ms == 0 ? " permanent" : "") + " reason " + reason);
		Discord.post(c);
		s.sendSuccess(() -> Component.literal("Banned " + shown + " (offline), case " + c.id + ". They see the ban screen on their next join."), true);
		return 1;
	}
}
