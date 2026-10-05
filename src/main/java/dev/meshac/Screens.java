package dev.meshac;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

/** The disconnect screens. Own palette and layout, all text and colours come from the config. */
public final class Screens {
	private static MutableComponent t(String s, String hex, boolean bold) {
		return Component.literal(s).withStyle(Style.EMPTY.withColor(TextColor.parseColor(hex).result().orElse(TextColor.fromRgb(0xFFFFFF))).withBold(bold));
	}
	private static final String GREY = "#8A93A6", WHITE = "#EDEFF5";

	private static MutableComponent frame(String title, String titleHex, String reason, Cases.Case c, String timeLine) {
		Config cf = Config.get();
		MutableComponent m = Component.empty();
		m.append(t("meshac", cf.accent, true)).append(t("  |  ", GREY, false)).append(t(title, titleHex, true)).append("\n\n");
		m.append(t(reason, WHITE, false)).append("\n");
		if (timeLine != null) m.append("\n").append(t(timeLine, titleHex, true));
		if (c != null) m.append("\n\n").append(t("Case ", GREY, false)).append(t(c.id, WHITE, true));
		m.append("\n\n").append(t(cf.appeal, GREY, false));
		return m;
	}
	public static Component kick(String reason, Cases.Case c) { return frame("Removed from " + Config.get().serverName, Config.get().warn, reason, c, null); }
	public static Component ban(Cases.Case c) {
		boolean temp = c.action.equals("tempban");
		String line = temp ? "Time left: " + left(c.until - System.currentTimeMillis()) : "This ban does not expire.";
		return frame(temp ? "Temporarily banned" : "Banned", "#FF6B6B", c.reason, c, line);
	}
	public static String left(long ms) {
		long m = Math.max(1, ms / 60_000);
		if (m >= 1440) return (m / 1440) + "d " + ((m % 1440) / 60) + "h";
		if (m >= 60) return (m / 60) + "h " + (m % 60) + "m";
		return m + "m";
	}
}
