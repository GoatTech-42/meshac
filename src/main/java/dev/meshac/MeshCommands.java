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
	public static void register(CommandDispatcher<CommandSourceStack> d) {
		d.register(Commands.literal("mesh").requires(Perm.admin())
			.then(Commands.literal("cases").executes(c -> list(c.getSource(), Cases.recent(10)))
				.then(Commands.argument("player", StringArgumentType.word()).executes(c -> list(c.getSource(), Cases.of(StringArgumentType.getString(c, "player"))))))
			.then(Commands.literal("case").then(Commands.argument("id", StringArgumentType.word()).executes(c -> show(c.getSource(), StringArgumentType.getString(c, "id")))))
			.then(Commands.literal("pardon").then(Commands.argument("id", StringArgumentType.word()).executes(c -> {
				boolean ok = Cases.pardon(StringArgumentType.getString(c, "id"));
				c.getSource().sendSuccess(() -> Component.literal(ok ? "Pardoned." : "No such case."), true); return ok ? 1 : 0; })))
			.then(Commands.literal("unban").then(Commands.argument("player", StringArgumentType.word()).executes(c -> {
				int n = Cases.unban(StringArgumentType.getString(c, "player"));
				c.getSource().sendSuccess(() -> Component.literal(n > 0 ? "Lifted " + n + " ban(s)." : "No active ban."), true); return n; })))
			.then(Commands.literal("ban").then(Commands.argument("player", StringArgumentType.word())
				.then(Commands.argument("duration", StringArgumentType.word()).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> ban(c.getSource(),
					StringArgumentType.getString(c, "player"), StringArgumentType.getString(c, "duration"), StringArgumentType.getString(c, "reason")))))))
			.then(Commands.literal("status").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
				ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"); String s = Verdict.status(t);
				c.getSource().sendSuccess(() -> Component.literal(s), false); return 1; })))
			.then(Commands.literal("watch").executes(c -> {
				ServerPlayer me = c.getSource().getPlayer(); if (me == null) { c.getSource().sendFailure(Component.literal("Players only.")); return 0; }
				boolean on = Verdict.WATCH.add(me.getUUID()); if (!on) Verdict.WATCH.remove(me.getUUID());
				c.getSource().sendSuccess(() -> Component.literal(on ? "Watching flags." : "Stopped watching."), false); return 1; }))
			.then(Commands.literal("monitor").executes(c -> { c.getSource().sendSuccess(() -> Component.literal("Monitor mode is " + (Config.get().monitor() ? "on" : "off") + "."), false); return 1; })
				.then(Commands.literal("on").executes(c -> { Config.get().monitorOnly = true; c.getSource().sendSuccess(() -> Component.literal("Monitor mode on: flags are logged; only egregiousChecks-listed checks enforce."), true); return 1; }))
				.then(Commands.literal("off").executes(c -> { Config.get().monitorOnly = false; c.getSource().sendSuccess(() -> Component.literal("Monitor mode off."), true); return 1; })))
			.then(Commands.literal("kick").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> {
				ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"); String why = StringArgumentType.getString(c, "reason");
				Cases.Case cs = Cases.add(t.getGameProfile().name(), t.getUUID(), "kick", why, c.getSource().getTextName(), 0, List.of("Staff kick")); t.connection.disconnect(Screens.kick(why, cs));
				c.getSource().sendSuccess(() -> Component.literal("Kicked " + t.getGameProfile().name() + ", case " + cs.id), true); return 1; }))))
			.then(Commands.literal("veil")
				.executes(c -> { c.getSource().sendSuccess(() -> Component.literal(veilLine()), false); return 1; })
				.then(Commands.literal("xray").then(Commands.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> {
					boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "on"); dev.meshac.veil.Veil.xrayOn = on; dev.meshac.veil.Veil.clear();
					c.getSource().sendSuccess(() -> Component.literal("Anti-xray " + (on ? "on" : "off") + " until reload or restart (config veilXray is unchanged). Players see it on the next chunk they load."), true); return 1; })))
				.then(Commands.literal("esp").then(Commands.argument("on", com.mojang.brigadier.arguments.BoolArgumentType.bool()).executes(c -> {
					boolean on = com.mojang.brigadier.arguments.BoolArgumentType.getBool(c, "on"); dev.meshac.veil.Veil.espOn = on;
					c.getSource().sendSuccess(() -> Component.literal("Anti-ESP " + (on ? "on" : "off") + " until reload or restart (config veilEsp is unchanged)."), true); return 1; })))
				.then(Commands.literal("off").executes(c -> { dev.meshac.veil.Veil.xrayOn = false; dev.meshac.veil.Veil.espOn = false; dev.meshac.veil.Veil.clear();
					c.getSource().sendSuccess(() -> Component.literal("Chunkveil fully off until reload or restart."), true); return 1; })))
			.then(Commands.literal("check")
				.executes(c -> { var l = Config.get().disabledChecks; c.getSource().sendSuccess(() -> Component.literal("Disabled checks: " + (l == null || l.isEmpty() ? "none" : String.join(", ", l))), false); return 1; })
				.then(Commands.argument("name", StringArgumentType.word()).then(Commands.literal("off").executes(c -> setCheck(c.getSource(), StringArgumentType.getString(c, "name"), false)))
					.then(Commands.literal("on").executes(c -> setCheck(c.getSource(), StringArgumentType.getString(c, "name"), true)))))
			.then(Commands.literal("reload").executes(c -> { Config.load(); Config.applyVeil(); Cases.load(); Config.get().warnUnknownChecks(); c.getSource().sendSuccess(() -> Component.literal("meshac reloaded."), true); return 1; })));
	}


	private static String veilLine() { return "chunkveil (stale plan rebuilds " + dev.meshac.veil.Veil.staleRebuilds + "): anti-xray " + (dev.meshac.veil.Veil.xrayOn ? "ON" : "off") + ", anti-ESP " + (dev.meshac.veil.Veil.espOn ? "ON" : "off") + ", config veilXray=" + Config.get().veilXray + " veilEsp=" + Config.get().veilEsp; }
	/** Runtime switch for one sub-check (same list as config disabledChecks). Not written to disk; /mesh reload restores the config's list. */
	private static int setCheck(CommandSourceStack s, String name, boolean on) {
		if (!Config.knownCheck(name)) { s.sendFailure(Component.literal("No check called " + name + ". Known: " + String.join(", ", new java.util.TreeSet<>(Config.KNOWN_CHECKS)))); return 0; }
		var l = Config.get().disabledChecks; if (l == null) { l = new java.util.ArrayList<>(); Config.get().disabledChecks = l; }
		l.removeIf(d -> d != null && d.equalsIgnoreCase(name));
		if (!on) l.add(name);
		s.sendSuccess(() -> Component.literal("Check " + name + (on ? " enabled." : " disabled until reload or restart: no flags, no heat, no setbacks or refusals from it for anyone.") + ""), true); return 1;
	}
	private static int list(CommandSourceStack s, List<Cases.Case> cs) {
		if (cs.isEmpty()) { s.sendSuccess(() -> Component.literal("No cases."), false); return 0; }
		for (Cases.Case c : cs) s.sendSuccess(() -> Component.literal(c.id + "  " + c.action + "  " + c.player + "  " + c.reason + (c.pardoned ? "  (pardoned)" : c.lifted ? "  (lifted)" : "")), false);
		return cs.size();
	}
	private static int show(CommandSourceStack s, String id) {
		Cases.Case c = Cases.find(id);
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
		Discord.post(c);
		s.sendSuccess(() -> Component.literal("Banned " + shown + " (offline), case " + c.id + ". They see the ban screen on their next join."), true);
		return 1;
	}
}
