package dev.meshac;

import java.util.Locale;
import java.util.Set;

/** Pure list of check names (no Minecraft types) so it can be unit tested with plain javac. Config delegates here. Keep in sync when a check is added; sim/CheckNamesTest scans the source and fails if a signalled name is missing. */
public final class CheckNames {
	private CheckNames() {}
	public static final Set<String> ALL = Set.of(
		"fly", "speed", "step", "jump", "air", "high", "ladder", "levitation", "microhop", "timer", "nofall", "noclip", "blink", "movement", "antiknockback",
		"combat", "interact", "inventory", "mining", "mining-lookray", "glide", "boatfly", "chat");
	public static boolean known(String name) { return name != null && ALL.contains(name.toLowerCase(Locale.ROOT)); }
}
