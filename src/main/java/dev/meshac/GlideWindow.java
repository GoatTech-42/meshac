package dev.meshac;

/** Pure timing rules around an elytra flight. Movement stamps glideAt on every packet while the player is fall-flying, so the windows run from the last glide packet. */
public final class GlideWindow {
	private GlideWindow() {}
	/** After the last glide packet the first hop still carries rocket and glide momentum (live false flag: jump dy 0.762 right after a firework landing). */
	public static final long JUMP_SKIP_MS = 2500;
	/** After the last glide packet the speed caps follow the last verified glide speed for this long. */
	public static final long CAP_MS = 1000;
	public static final double CAP_SLACK = 0.03;

	/** True when the high jump / step check should run. glideAt 0 (never glided) always runs it. */
	public static boolean jumpCheckLive(long nowMs, long glideAt) { return nowMs - glideAt > JUMP_SKIP_MS; }

	public static boolean capWindow(long nowMs, long glideAt) { return glideAt != 0 && nowMs - glideAt < CAP_MS; }

	/** Per-packet cap, raised to the last glide speed inside the window. */
	public static double cap(double cap, long nowMs, long glideAt, double glideH) { return capWindow(nowMs, glideAt) ? Math.max(cap, glideH + CAP_SLACK) : cap; }
}
