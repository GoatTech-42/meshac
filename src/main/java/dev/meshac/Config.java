package dev.meshac;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** config/meshac.json. Written with defaults on first start so staff can see every knob. */
public final class Config {
	/** Optional client-supplied identity hints. Empty exact lists, disabled by default. */
	public boolean clientFingerprintEnabled = false;
	public String[] clientFingerprintBrands = {};
	public String[] clientFingerprintChannels = {};
	public String appeal = "Think this is a mistake? Tell staff the case ID above.";
	public String discordWebhook = ""; // empty = off
	public boolean discordOnHold = true; // also post when a player reaches the hold step, not only on kicks and bans
	/** lenient, default or strict. Every knob below that is null follows the preset; set one to override just that knob. */
	public String preset = "default";
	public Double holdAt, removeAt; public Integer heatCoolSeconds, kicksBeforeTempban, offenceMemoryDays;
	public int[] tempbanMinutes;
	/** Ladder maths, see docs/PUNISHMENT-DESIGN.md. null follows the preset. */
	public Double halfLifeSeconds, tempbanBaseMinutes, tempbanGrowth, tempbanMaxMinutes, offenceHalfLifeDays;
	public Integer requireClusters, requireClustersTierA;
	public Boolean permanentNeedsTierA = true, skipRungOnTierA = true;
	public Boolean fixPistonWater; // patches MC-130183 (short-pulse sticky piston keeps waterlogging), stops flood machines. OFF by default
	public Boolean veilXray, veilEsp; // anti-xray and anti-ESP, both off until proven on a real client
	public Boolean monitorOnly; // true: log and alert but never kick or ban
	public Boolean permanentBan; // false caps the ladder at its longest tempban
	public String accent = "#7CF5C8";
	public String warn = "#FFB454";
	public String serverName = "this server";

	// holdAt, removeAt, heat cool-down s, kicks before the first tempban, memory days, permanent ban (1/0)
	private int[] base() {
		return switch (preset.toLowerCase()) {
			case "lenient" -> new int[] {4, 7, 10, 2, 14, 0};
			case "strict" -> new int[] {3, 4, 10, 1, 28, 0};
			default -> new int[] {3, 5, 10, 1, 14, 0};
		};
	}
	
	
	public long heatCoolMs() { return (heatCoolSeconds != null ? heatCoolSeconds : base()[2]) * 1000L; }
	public int kicks() { return kicksBeforeTempban != null ? kicksBeforeTempban : base()[3]; }
	public int memoryDays() { return offenceMemoryDays != null ? offenceMemoryDays : base()[4]; }
	public java.util.List<String> exemptPlayers = new java.util.ArrayList<>(); // names (case-insensitive) skipped by the movement and combat checks
	public java.util.List<String> egregiousChecks = new java.util.ArrayList<>(); // in monitor mode only THESE checks enforce (setback/hold/kick); everything else logs only. e.g. ["fly"]
	public boolean isEgregious(String check) { return egregiousChecks != null && egregiousChecks.contains(check); }
	public boolean enforce(String check) { return !monitor() || isEgregious(check); } // soft mode: raw action refusals (break/click/use) follow the same gate as verdicts
	public java.util.Map<String, java.util.List<String>> exemptChecks = new java.util.HashMap<>(); // check name -> player names exempt from that check only
	public java.util.List<String> disabledChecks = new java.util.ArrayList<>(); // sub-check names switched fully off (Luke "Exempt that check rn" 2026-10-11)
	public boolean disabled(String check) { if (disabledChecks == null) return false; for (String d : disabledChecks) if (d != null && d.equalsIgnoreCase(check)) return true; return false; }
	public boolean exemptFrom(net.minecraft.server.level.ServerPlayer pl, String check) { if (exemptChecks == null) return false; String n = pl.getGameProfile().name(); for (var en : exemptChecks.entrySet()) { if (en.getKey() == null || !en.getKey().equalsIgnoreCase(check) || en.getValue() == null) continue; for (String e : en.getValue()) if (e != null && e.equalsIgnoreCase(n)) return true; } return false; } // keys are matched case-insensitively like names
	/** Every name Verdict.signal / disabled() / enforce() is called with. Movement sub-checks use the first word of their message (hit.split(" ")[0]). Keep in sync when a check is added. */
	public static final java.util.Set<String> KNOWN_CHECKS = java.util.Set.of(
		"fly", "speed", "step", "jump", "air", "high", "ladder", "levitation", "microhop", "timer", "nofall", "noclip", "blink", "movement", "antiknockback",
		"combat", "interact", "inventory", "mining", "mining-lookray", "glide", "boatfly", "chat");
	public static boolean knownCheck(String name) { return name != null && KNOWN_CHECKS.contains(name.toLowerCase(java.util.Locale.ROOT)); }
	/** Log a warning for every unknown name in disabledChecks, exemptChecks and egregiousChecks: a typo silently does nothing otherwise. Call after load and reload. */
	public void warnUnknownChecks() {
		java.util.List<String> all = new java.util.ArrayList<>();
		if (disabledChecks != null) all.addAll(disabledChecks);
		if (egregiousChecks != null) all.addAll(egregiousChecks);
		if (exemptChecks != null) all.addAll(exemptChecks.keySet());
		for (String n : all) if (!knownCheck(n)) Meshac.LOG.warn("[meshac] config names a check that does not exist: \"{}\" (known: {})", n, String.join(", ", new java.util.TreeSet<>(KNOWN_CHECKS)));
	}
	/** One answer for "should this check ignore this player": creative/spectator, the global exempt roster, the per-check roster, or the check switched off. Every check and Verdict.signal use it, so no code path can forget one of the four. */
	public boolean skips(net.minecraft.server.level.ServerPlayer pl, String check) { return pl.isCreative() || pl.isSpectator() || exempt(pl) || exemptFrom(pl, check) || disabled(check); }
	public boolean exempt(net.minecraft.server.level.ServerPlayer pl) { if (exemptPlayers == null) return false; String n = pl.getGameProfile().name(); for (String e : exemptPlayers) if (e != null && e.equalsIgnoreCase(n)) return true; return false; }
	public Boolean combatLog; public Integer combatLogSeconds; // combat log protection: on by default, tag length 15 s
	public boolean combatLog() { return combatLog == null || combatLog; }
	public int combatLogSeconds() { return combatLogSeconds != null ? combatLogSeconds : 15; }
	public boolean monitor() { return monitorOnly != null && monitorOnly; }
	public boolean perma() { return permanentBan != null ? permanentBan : false; }
	public int holdAtI() { return (int) Math.round(holdAtD()); }
	public double holdAtD() { return holdAt != null ? holdAt : 3; }
	public double removeAtD() { return removeAt != null ? removeAt : switch (preset.toLowerCase()) { case "lenient" -> 8; case "strict" -> 5; default -> 6; }; }
	public Ladder.Params ladderParams() { return new Ladder.Params(halfLifeSeconds != null ? halfLifeSeconds : 8, holdAtD(), removeAtD(), requireClusters != null ? requireClusters : 3, requireClustersTierA != null ? requireClustersTierA : 2, 3, 150); }
	public boolean explicitLadder() { return tempbanMinutes != null && tempbanMinutes.length > 0; }
	public double tempBase() { return tempbanBaseMinutes != null ? tempbanBaseMinutes : switch (preset.toLowerCase()) { case "lenient" -> 5; case "strict" -> 30; default -> 10; }; }
	public double tempGrowth() { return tempbanGrowth != null ? tempbanGrowth : 6; }
	public double tempMax() { return tempbanMaxMinutes != null ? tempbanMaxMinutes : 10080; }
	public double offenceHalfLifeDays() { return offenceHalfLifeDays != null ? offenceHalfLifeDays : memoryDays(); }
	public boolean permanentNeedsTierA() { return permanentNeedsTierA == null || permanentNeedsTierA; }
	public boolean skipRungOnTierA() { return skipRungOnTierA == null || skipRungOnTierA; }
	public int[] tempMinutes() {
		if (tempbanMinutes != null && tempbanMinutes.length > 0) return tempbanMinutes;
		return switch (preset.toLowerCase()) {
			case "lenient" -> new int[] {10, 60, 360, 1440};
			case "strict" -> new int[] {60, 1440, 10080};
			default -> new int[] {5, 30, 1440};
		};
	}

	private static String documented(Gson g, Config c) {
		var root = g.toJsonTree(c).getAsJsonObject();
		var help = new com.google.gson.JsonObject();
		help.addProperty("preset", "lenient, default or strict. null overrides below use the preset.");
		help.addProperty("punishments", "holdAt/removeAt are heat levels; heatCoolSeconds cools heat. kicksBeforeTempban counts removals, offenceMemoryDays sets history. tempbanMinutes is the ladder. permanentBan=false caps it.");
		help.addProperty("clientFingerprint", "OFF by default. Exact client-provided brand/channel matches only, case-sensitive. Empty lists match nobody. Hints can be spoofed; never proof of cheats. First match kicks, matching rejoin bans separately from behavior. Generic vanilla/fabric/forge/neoforge and minecraft/fabric/forge channels are ignored. Pardon cases to clear history.");
		help.addProperty("discord", "Empty discordWebhook disables posts. Treat webhook as secret. discordOnHold also posts holds.");
		help.addProperty("appearance", "accent and warn are #RRGGBB colors. serverName and appeal appear on removal screens.");
		root.add("_help", help); return g.toJson(root) + "\n";
	}
	private static Config cur = new Config();
	public static Config get() { return cur; }

	public static void applyVeil() { dev.meshac.veil.Veil.xrayOn = get().veilXray != null && get().veilXray; dev.meshac.veil.Veil.espOn = get().veilEsp != null && get().veilEsp; dev.meshac.veil.Veil.clear(); }
	public static void load() {
		Path p = FabricLoader.getInstance().getConfigDir().resolve("meshac.json");
		Gson g = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		try {
			if (Files.exists(p)) {
				var root = com.google.gson.JsonParser.parseString(Files.readString(p)).getAsJsonObject();
				cur = g.fromJson(root, Config.class);
				// Add new defaults/help without changing existing or unknown owner settings.
				var defaults = com.google.gson.JsonParser.parseString(documented(g, new Config())).getAsJsonObject();
				for (var entry : defaults.entrySet()) if (!root.has(entry.getKey())) root.add(entry.getKey(), entry.getValue());
				Files.writeString(p, g.toJson(root) + "\n");
			}
			else Files.writeString(p, documented(g, cur));
		} catch (Exception e) { Meshac.LOG.warn("[meshac] could not read config, using defaults: {}", e.toString()); }
	}
}
