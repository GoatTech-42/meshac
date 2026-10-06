package dev.meshac;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** config/meshac.json. Written with defaults on first start so staff can see every knob. */
public final class Config {
	public String appeal = "Think this is a mistake? Tell staff the case ID above.";
	public String discordWebhook = ""; // empty = off
	public boolean discordOnHold = true; // also post when a player reaches the hold step, not only on kicks and bans
	/** lenient, default or strict. Every knob below that is null follows the preset; set one to override just that knob. */
	public String preset = "default";
	public Integer holdAt, removeAt, heatCoolSeconds, kicksBeforeTempban, offenceMemoryDays;
	public int[] tempbanMinutes;
	public Boolean permanentBan; // false caps the ladder at its longest tempban
	public String accent = "#7CF5C8";
	public String warn = "#FFB454";
	public String serverName = "this server";

	// holdAt, removeAt, heat cool-down s, kicks before the first tempban, memory days, permanent ban (1/0)
	private int[] base() {
		return switch (preset.toLowerCase()) {
			case "lenient" -> new int[] {4, 7, 10, 2, 14, 1};
			case "strict" -> new int[] {3, 4, 10, 1, 90, 1};
			default -> new int[] {3, 5, 10, 1, 30, 1};
		};
	}
	public int holdAt() { return holdAt != null ? holdAt : base()[0]; }
	public int removeAt() { return removeAt != null ? removeAt : base()[1]; }
	public long heatCoolMs() { return (heatCoolSeconds != null ? heatCoolSeconds : base()[2]) * 1000L; }
	public int kicks() { return kicksBeforeTempban != null ? kicksBeforeTempban : base()[3]; }
	public int memoryDays() { return offenceMemoryDays != null ? offenceMemoryDays : base()[4]; }
	public boolean perma() { return permanentBan != null ? permanentBan : base()[5] == 1; }
	public int[] tempMinutes() {
		if (tempbanMinutes != null && tempbanMinutes.length > 0) return tempbanMinutes;
		return switch (preset.toLowerCase()) {
			case "lenient" -> new int[] {10, 60, 360, 1440};
			case "strict" -> new int[] {60, 1440, 10080};
			default -> new int[] {5, 30, 1440};
		};
	}

	private static Config cur = new Config();
	public static Config get() { return cur; }

	public static void load() {
		Path p = FabricLoader.getInstance().getConfigDir().resolve("meshac.json");
		Gson g = new GsonBuilder().setPrettyPrinting().serializeNulls().create();
		try {
			if (Files.exists(p)) cur = g.fromJson(Files.readString(p), Config.class);
			else Files.writeString(p, g.toJson(cur));
		} catch (Exception e) { Meshac.LOG.warn("[meshac] could not read config, using defaults: {}", e.toString()); }
	}
}
