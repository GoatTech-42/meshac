package dev.meshac;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Read-only "has this name joined before" lookup. Never creates a UUID and never writes usercache.json
 * (the vanilla UserNameToIdResolver.get(name) does both in offline mode). No Minecraft imports, so it behaves
 * the same on 1.21.11 and 26.x and can be unit tested on a bare JDK.
 */
public final class SeenNames {
	public static final class Hit { private final UUID id; private final String name; public Hit(UUID id, String name) { this.id = id; this.name = name; } public UUID id() { return id; } public String name() { return name; } }
	/** One case-history row: name and uuid string. */
	public static final class Past { private final String player, uuid; public Past(String player, String uuid) { this.player = player; this.uuid = uuid; } public String player() { return player; } public String uuid() { return uuid; } }

	private static final Pattern ENTRY = Pattern.compile("\\{[^{}]*\\}");
	private static final Pattern NAME = Pattern.compile("\"name\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
	private static final Pattern UUID_F = Pattern.compile("\"uuid\"\\s*:\\s*\"([0-9a-fA-F-]{32,36})\"");

	private SeenNames() {}

	/** Looks in usercache.json first, then in the newest-first case history. Returns null if the name was never seen. */
	public static Hit find(Path usercache, List<Past> casesOldestFirst, String name) {
		if (name == null || name.isBlank()) return null;
		Hit h = fromUserCache(usercache, name);
		if (h != null) return h;
		for (int i = casesOldestFirst.size() - 1; i >= 0; i--) {
			Past p = casesOldestFirst.get(i);
			if (p.player() == null || !p.player().equalsIgnoreCase(name)) continue;
			UUID id = parse(p.uuid());
			if (id != null) return new Hit(id, p.player());
		}
		return null;
	}

	static Hit fromUserCache(Path file, String name) {
		if (file == null) return null;
		String txt;
		try { if (!Files.isRegularFile(file)) return null; txt = Files.readString(file); } catch (Exception e) { return null; }
		Matcher m = ENTRY.matcher(txt);
		while (m.find()) {
			String e = m.group();
			Matcher n = NAME.matcher(e), u = UUID_F.matcher(e);
			if (!n.find() || !u.find()) continue;
			String nm = n.group(1);
			if (!nm.toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT))) continue;
			UUID id = parse(u.group(1));
			if (id != null) return new Hit(id, nm);
		}
		return null;
	}

	private static UUID parse(String s) {
		try {
			if (s == null) return null;
			if (s.length() == 32) s = s.replaceFirst("([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})", "$1-$2-$3-$4-$5");
			return UUID.fromString(s);
		} catch (Exception e) { return null; }
	}
}
