package dev.meshac;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.fabricmc.loader.api.FabricLoader;

/** meshac's own case store (config/meshac-cases.json). Kicks, tempbans and bans all live here, with the evidence that caused them. */
public final class Cases {
	public static final class Case {
		public String id, player, uuid, action, reason, by;
		public long at, until;          // until: 0 = no expiry (kick, permanent ban)
		public boolean pardoned, lifted;
		public List<String> evidence = new ArrayList<>();
	}
	private static final Gson G = new GsonBuilder().setPrettyPrinting().create();
	private static List<Case> all = new ArrayList<>();
	private static Path file() { return FabricLoader.getInstance().getConfigDir().resolve("meshac-cases.json"); }

	public static synchronized void load() {
		try { if (Files.exists(file())) all = G.fromJson(Files.readString(file()), new TypeToken<List<Case>>() {}.getType()); }
		catch (Exception e) { Meshac.LOG.warn("[meshac] could not read cases: {}", e.toString()); }
		if (all == null) all = new ArrayList<>();
	}
	private static void save() { try { Files.writeString(file(), G.toJson(all)); } catch (Exception e) { Meshac.LOG.warn("[meshac] could not save cases: {}", e.toString()); } }

	public static synchronized Case add(String player, UUID uuid, String action, String reason, String by, long until, List<String> evidence) {
		Case c = new Case();
		String abc = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; StringBuilder b = new StringBuilder("MA-");
		var r = new java.util.Random(); for (int i = 0; i < 4; i++) b.append(abc.charAt(r.nextInt(abc.length())));
		c.id = b.toString(); c.player = player; c.uuid = uuid.toString(); c.action = action; c.reason = reason; c.by = by;
		c.at = System.currentTimeMillis(); c.until = until; c.evidence = new ArrayList<>(evidence);
		all.add(c); save(); return c;
	}
	/** Active ban for this player, or null. Permanent bans have action "ban" and until 0; tempbans expire. */
	public static synchronized Case activeBan(UUID id) {
		long now = System.currentTimeMillis();
		for (int i = all.size() - 1; i >= 0; i--) {
			Case c = all.get(i);
			if (!c.uuid.equals(id.toString()) || c.pardoned || c.lifted) continue;
			if (c.action.equals("ban") || (c.action.equals("tempban") && c.until > now)) return c;
		}
		return null;
	}
	/** meshac's own removals (kicks, tempbans, bans) inside the memory window that were not pardoned; this is what escalation counts. Staff actions do not count. */
	public static synchronized int offences(UUID id) {
		long from = System.currentTimeMillis() - Config.get().memoryDays() * 86_400_000L; int n = 0;
		for (Case c : all) if (c.uuid.equals(id.toString()) && !c.pardoned && c.at >= from && c.by.equals("meshac")) n++;
		return n;
	}
	/** Lifts every active ban on this player but keeps the offences on record, so the next one still escalates. Returns how many were lifted. */
	public static synchronized int unban(String name) {
		long now = System.currentTimeMillis(); int n = 0;
		for (Case c : all) if (c.player.equalsIgnoreCase(name) && !c.pardoned && !c.lifted && (c.action.equals("ban") || (c.action.equals("tempban") && c.until > now))) { c.lifted = true; n++; }
		if (n > 0) save();
		return n;
	}
	public static synchronized Case find(String id) { for (Case c : all) if (c.id.equalsIgnoreCase(id)) return c; return null; }
	public static synchronized boolean pardon(String id) { Case c = find(id); if (c == null) return false; c.pardoned = true; save(); return true; }
	public static synchronized List<Case> recent(int n) { return new ArrayList<>(all.subList(Math.max(0, all.size() - n), all.size())); }
	public static synchronized List<Case> of(String name) { List<Case> o = new ArrayList<>(); for (Case c : all) if (c.player.equalsIgnoreCase(name)) o.add(c); return o; }
}
