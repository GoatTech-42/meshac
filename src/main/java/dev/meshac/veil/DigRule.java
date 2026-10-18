package dev.meshac.veil;

/** The early-stop rule of Mining.digStop as a pure function, so the sim can test it without Minecraft classes. delta is the destroy progress per tick of the block the SERVER holds. */
public final class DigRule {
	private DigRule() {}
	public static boolean early(float delta, int ticks) {
		return delta < 1f && delta * (ticks + 1) < 0.5f;
	}
}
