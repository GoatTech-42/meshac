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
 public Boolean veilEnabled, enderchestEggRule; // feature switches, independent of punishment monitor mode
 public boolean veilEnabled() { return veilEnabled == null || veilEnabled; }
 public boolean enderchestEggRule() { return enderchestEggRule == null || enderchestEggRule; }
	public Boolean veilXray, veilEsp, veilBedrock; // anti-xray and anti-ESP, both off until proven on a real client
	public Boolean monitorOnly; // true: log and alert but never kick or ban
	public Boolean permanentBan; // false caps the ladder at its longest tempban
	public String accent = "#7CF5C8";
	public String warn = "#FFB454";
	public String serverName = "this server";

	// holdAt, removeAt, (unused slot, was heat cool-down s), kicks before the first tempban, memory days, permanent ban (1/0)
	private int[] base() {
		return switch (preset.toLowerCase()) {
			case "lenient" -> new int[] {4, 7, 10, 2, 14, 0};
			case "strict" -> new int[] {3, 4, 10, 1, 28, 0};
			default -> new int[] {3, 5, 10, 1, 14, 0};
		};
	}
	
	
	public int kicks() { return kicksBeforeTempban != null ? kicksBeforeTempban : base()[3]; }
	public int memoryDays() { return offenceMemoryDays != null ? offenceMemoryDays : base()[4]; }
	public java.util.List<String> exemptPlayers = new java.util.ArrayList<>(); // names (case-insensitive) skipped by the movement and combat checks
	public java.util.List<String> egregiousChecks = new java.util.ArrayList<>(); // legacy setting retained for file compatibility; never bypasses monitor mode
	public boolean isEgregious(String check) { if (egregiousChecks == null) return false; for (String e : egregiousChecks) if (e != null && e.equalsIgnoreCase(check)) return true; return false; } // case-insensitive like the other check lists
	public boolean enforce(String check) { return !monitor() && !policyInvalid; } // soft mode: raw action refusals (break/click/use) follow the same gate as verdicts; an invalid policy pauses enforcement (log-only) until a valid reload
	public java.util.Map<String, java.util.List<String>> exemptChecks = new java.util.HashMap<>(); // check name -> player names exempt from that check only
	public java.util.List<String> disabledChecks = new java.util.ArrayList<>(); // sub-check names switched fully off (Luke "Exempt that check rn" 2026-10-11)
	public boolean disabled(String check) { if (disabledChecks == null) return false; for (String d : disabledChecks) if (d != null && d.equalsIgnoreCase(check)) return true; return false; }
	public boolean exemptFrom(net.minecraft.server.level.ServerPlayer pl, String check) { if (exemptChecks == null) return false; String n = pl.getGameProfile().name(); for (var en : exemptChecks.entrySet()) { if (en.getKey() == null || !en.getKey().equalsIgnoreCase(check) || en.getValue() == null) continue; for (String e : en.getValue()) if (e != null && e.equalsIgnoreCase(n)) return true; } return false; } // keys are matched case-insensitively like names
	/** Every name Verdict.signal / disabled() / enforce() is called with. Movement sub-checks use the first word of their message (hit.split(" ")[0]). Keep in sync when a check is added. */
	public static final java.util.Set<String> KNOWN_CHECKS = CheckNames.ALL;
	public static boolean knownCheck(String name) { return CheckNames.known(name); }
	/** Log a warning for every unknown name in disabledChecks, exemptChecks and egregiousChecks: a typo silently does nothing otherwise. Call after load and reload. */
	public void warnUnknownChecks() {
		java.util.List<String> all = new java.util.ArrayList<>();
		if (disabledChecks != null) all.addAll(disabledChecks);
		if (egregiousChecks != null) all.addAll(egregiousChecks);
		if (exemptChecks != null) all.addAll(exemptChecks.keySet());
		if (heatCoolSeconds != null) Meshac.LOG.warn("[meshac] heatCoolSeconds is ignored (it never did anything). Heat cooling is set by halfLifeSeconds; remove heatCoolSeconds from the config.");
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
	/** Last ladder params that passed validation; a reload with invalid values keeps these instead of the broken ones. */
	private static transient Ladder.Params lastValidLadder;
	/** True while the active config carries an invalid policy value: enforcement pauses (log-only) until a valid reload. Transient: never written to meshac.json. */
	public transient boolean policyInvalid;
	public Ladder.Params ladderParams() {
		Ladder.Params raw = new Ladder.Params(halfLifeSeconds != null ? halfLifeSeconds : 8, holdAtD(), removeAtD(), requireClusters != null ? requireClusters : 3, requireClustersTierA != null ? requireClustersTierA : 2, 3, 150);
		String probs = Ladder.Params.problems(raw);
		if (!probs.isEmpty()) {
			policyInvalid = true;
			Meshac.LOG.warn("[meshac] invalid ladder config ({}): keeping the last valid values and pausing enforcement (log-only) until a valid reload", probs);
			return lastValidLadder != null ? lastValidLadder : Ladder.Params.defaults();
		}
		if (!policyInvalid) lastValidLadder = raw; // do not clear a posture set by offenceHalfLifeDays() in the same call pass
		return raw;
	}
	public boolean explicitLadder() { return tempbanMinutes != null && tempbanMinutes.length > 0; }
	public double tempBase() { return tempbanBaseMinutes != null ? tempbanBaseMinutes : switch (preset.toLowerCase()) { case "lenient" -> 5; case "strict" -> 30; default -> 10; }; }
	public double tempGrowth() { return tempbanGrowth != null ? tempbanGrowth : 6; }
	public double tempMax() { return tempbanMaxMinutes != null ? tempbanMaxMinutes : 10080; }
	public double offenceHalfLifeDays() {
		double hl = offenceHalfLifeDays != null ? offenceHalfLifeDays : memoryDays();
		if (!(hl > 0) || !Double.isFinite(hl)) {
			policyInvalid = true;
			Meshac.LOG.warn("[meshac] invalid offenceHalfLifeDays ({}): keeping 14 d and pausing enforcement (log-only) until a valid reload", hl);
			return 14;
		}
		return hl;
	}
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
		help.addProperty("punishments", "holdAt/removeAt are heat levels; heat cools by half every halfLifeSeconds (default 8). kicksBeforeTempban counts removals, offenceMemoryDays sets history. tempbanMinutes is the ladder. permanentBan=false caps it.");
		help.addProperty("clientFingerprint", "OFF by default. Exact client-provided brand/channel matches only, case-sensitive. Empty lists match nobody. Hints can be spoofed; never proof of cheats. First match kicks, matching rejoin bans separately from behavior. Generic vanilla/fabric/forge/neoforge and minecraft/fabric/forge channels are ignored. Pardon cases to clear history.");
		help.addProperty("discord", "Empty discordWebhook disables posts. Treat webhook as secret. discordOnHold also posts holds.");
		help.addProperty("appearance", "accent and warn are #RRGGBB colors. serverName and appeal appear on removal screens.");
		root.add("_help", help); return g.toJson(root) + "\n";
	}
	private static Config cur = new Config();
	public static Config get() { return cur; }

	public static void applyVeil() { dev.meshac.veil.Veil.xrayOn = get().veilEnabled() && get().veilXray != null && get().veilXray; dev.meshac.veil.Veil.espOn = get().veilEnabled() && get().veilEsp != null && get().veilEsp; dev.meshac.veil.Veil.bedrockOn = get().veilBedrock != null && get().veilBedrock; dev.meshac.veil.Veil.clear(); dev.meshac.veil.Veil.startupCheck(); Meshac.LOG.info("[meshac] {}", new dev.meshac.veil.VeilSettings(get().veilEnabled, get().veilXray, get().veilEsp, get().veilBedrock).describe()); }
	/** Parse failures keep prior settings. Persistence failures keep the successfully applied settings and report that the file was not saved. */
 public static AdminLog.ReloadResult load() {
  Path p = FabricLoader.getInstance().getConfigDir().resolve("meshac.json");
  Gson g = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
  Config next = cur; String output; boolean existed = Files.exists(p);
  try {
   if (existed) {
    var root = com.google.gson.JsonParser.parseString(Files.readString(p)).getAsJsonObject();
    var parsed = SafeSwap.parse(() -> g.fromJson(root, Config.class));
    if (parsed.isEmpty()) { Meshac.LOG.warn("[meshac] config did not parse into settings, keeping the previous ones"); return AdminLog.ReloadResult.PARSE_FAILED; }
    next = parsed.get();
    for (String t : dev.meshac.veil.VeilSettings.typos(root.keySet())) Meshac.LOG.warn("[meshac] config key {} is ignored, the veil key is spelled exactly like that", t);
    // Prepare the complete persisted document before changing any active state.
    var defaults = com.google.gson.JsonParser.parseString(documented(g, new Config())).getAsJsonObject();
    for (var entry : defaults.entrySet()) if (!root.has(entry.getKey())) root.add(entry.getKey(), entry.getValue());
    output = g.toJson(root) + "\n";
   } else output = documented(g, cur);
  } catch (Exception e) { Meshac.LOG.warn("[meshac] config read/parse failed, previous settings kept: {}", e.toString()); return AdminLog.ReloadResult.PARSE_FAILED; }
  Movement.clearPendingActions(); Verdict.clearPendingActions(); cur = next; // includes file-absent success: no old hold or NoFall debt survives
  try { Files.writeString(p, output); }
  catch (Exception e) { Meshac.LOG.warn("[meshac] config settings applied but file could not be saved: {}", e.toString()); return AdminLog.ReloadResult.APPLIED_NOT_SAVED; }
  return existed ? AdminLog.ReloadResult.APPLIED : AdminLog.ReloadResult.DEFAULTS_CREATED;
 }
}
