package dev.meshac;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;

/** config/meshac.json. Written with defaults on first start so staff can see every knob. */
public final class Config {
	public String appeal = "Think this is a mistake? Tell staff the case ID above.";
	public String discordWebhook = "";
	public int[] tempbanMinutes = {10, 60, 360, 1440, 10080}; // 6th offence is permanent
	public int offenceMemoryDays = 30;
	public String accent = "#7CF5C8";
	public String warn = "#FFB454";
	public String serverName = "this server";

	private static Config cur = new Config();
	public static Config get() { return cur; }

	public static void load() {
		Path p = FabricLoader.getInstance().getConfigDir().resolve("meshac.json");
		Gson g = new GsonBuilder().setPrettyPrinting().create();
		try {
			if (Files.exists(p)) cur = g.fromJson(Files.readString(p), Config.class);
			else Files.writeString(p, g.toJson(cur));
		} catch (Exception e) { Meshac.LOG.warn("[meshac] could not read config, using defaults: {}", e.toString()); }
	}
}
