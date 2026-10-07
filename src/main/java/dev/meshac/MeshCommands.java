package dev.meshac;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /mesh for staff: cases, case, pardon, unban, ban, reload. */
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
				.then(Commands.argument("minutes", IntegerArgumentType.integer(0)).then(Commands.argument("reason", StringArgumentType.greedyString()).executes(c -> ban(c.getSource(),
					StringArgumentType.getString(c, "player"), IntegerArgumentType.getInteger(c, "minutes"), StringArgumentType.getString(c, "reason")))))))
			.then(Commands.literal("reload").executes(c -> { Config.load(); Cases.load(); c.getSource().sendSuccess(() -> Component.literal("meshac reloaded."), true); return 1; })));
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
	private static int ban(CommandSourceStack s, String name, int minutes, String reason) {
		ServerPlayer p = s.getServer().getPlayerList().getPlayerByName(name);
		if (p == null) { s.sendFailure(Component.literal("Player must be online.")); return 0; }
		Cases.Case c = Cases.add(p.getGameProfile().name(), p.getUUID(), minutes == 0 ? "ban" : "tempban", reason, s.getTextName(), minutes == 0 ? 0 : System.currentTimeMillis() + minutes * 60_000L, List.of("staff action"));
		p.connection.disconnect(Screens.ban(c));
		Discord.post(c);
		s.sendSuccess(() -> Component.literal("Banned " + name + ", case " + c.id), true);
		return 1;
	}
}
