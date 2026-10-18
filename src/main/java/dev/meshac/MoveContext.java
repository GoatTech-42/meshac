package dev.meshac;

import java.util.Locale;

/** Log-only context line printed beside a noclip or speed flag, so a later log window can say what the player was doing. */
public final class MoveContext {
	private MoveContext() {}
	public static String ageS(long nowMs, long at) { return at == 0 ? "never" : String.format(Locale.ROOT, "%.1fs", (nowMs - at) / 1000.0); }
	public static String line(String player, double dx, double dy, double dz, double dtS, boolean ground, boolean lastGround,
			boolean water, boolean lava, boolean climb, boolean vehicle, boolean elytra, int grace, int hurtTicks, String damage,
			long nowMs, long windAt, long kbAt, long glideAt, long graceAt) {
		return String.format(Locale.ROOT, "[ctx] %s d=(%.3f,%.3f,%.3f) h=%.3f ticks=%.1f ground=%s/%s water=%s lava=%s climb=%s vehicle=%s elytra=%s grace=%d hurt=%d dmg=%s since: wind=%s kb=%s glide=%s tp=%s",
			player, dx, dy, dz, Math.hypot(dx, dz), dtS / 0.05, ground, lastGround, water, lava, climb, vehicle, elytra, grace, hurtTicks,
			damage == null ? "none" : damage, ageS(nowMs, windAt), ageS(nowMs, kbAt), ageS(nowMs, glideAt), ageS(nowMs, graceAt));
	}

	/** Rate gate for the noclip from/to line: at most one per player per second, so a stuck player cannot flood the log. */
	public static boolean noclipCtxDue(long nowMs, long lastAt) { return nowMs - lastAt >= 1000; }

	/** Log-only from/to line for a noclip flag: endpoints and the block each end sits in, so a later log window can tell a pearl/piston embed from a cheat. */
	public static String noclipLine(String player, double fx, double fy, double fz, String fromBlock, double tx, double ty, double tz, String toBlock) {
		return String.format(Locale.ROOT, "[ctx] %s noclip from=(%.3f,%.3f,%.3f) in=%s to=(%.3f,%.3f,%.3f) in=%s", player, fx, fy, fz, fromBlock, tx, ty, tz, toBlock);
	}
}
