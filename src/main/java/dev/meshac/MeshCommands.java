package dev.meshac;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /mesh for staff: cases, case, pardon, unban, ban, kick, status, watch, monitor, reload. */
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
				.then(Commands.literal("on").executes(c -> { Config.get().monitorOnly = true; c.getSource().sendSuccess(() -> Component.literal("Monitor mode on: flags are logged, nobody is kicked or banned."), true); return 1; }))
				.then(Commands.literal("off").executes(c -> { Config.get().monitorOnly = false; c.getSource().sendSuccess(() -> Component.literal("Monitor mode off."), true); return 1; })))
			.then(Commands.literal("kick").then(Commands.argument("player", net.minecraft.commands.arguments.EntityArgument.player()).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> {
				ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "player"); String why = StringArgumentType.getString(c, "reason");
				Cases.Case cs = Cases.add(t.getGameProfile().name(), t.getUUID(), "kick", why, c.getSource().getTextName(), 0, List.of("Staff kick")); t.connection.disconnect(Screens.kick(why, cs));
				c.getSource().sendSuccess(() -> Component.literal("Kicked " + t.getGameProfile().name() + ", case " + cs.id), true); return 1; }))))
			.then(Commands.literal("reload").executes(c -> { Config.load(); Config.applyVeil(); Cases.load(); c.getSource().sendSuccess(() -> Component.literal("meshac reloaded."), true); return 1; })));
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
		if (p == null) { s.sendFailure(Component.literal("Player must be online.")); return 0; }
		Cases.Case c = Cases.add(p.getGameProfile().name(), p.getUUID(), minutes == 0 ? "ban" : "tempban", reason, s.getTextName(), ms == 0 ? 0 : System.currentTimeMillis() + ms, List.of("staff action"));
		p.connection.disconnect(Screens.ban(c));
		Discord.post(c);
		s.sendSuccess(() -> Component.literal("Banned " + name + ", case " + c.id), true);
		return 1;
	}
}
