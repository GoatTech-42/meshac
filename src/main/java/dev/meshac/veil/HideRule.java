package dev.meshac.veil;

/**
 * Whether a buried block may be shown as a different block without tripping the mining check.
 * The client times its dig by the block it is shown and sends STOP when its own progress reaches 1. Mining.digStop reads the REAL block and flags a stop with less than half the real progress done (DigRule).
 * With the same tool on both, real progress is shown progress times shownHardness / realHardness, so the flag cannot fire while shownHardness >= realHardness / 2.
 * Vanilla values: stone 1.5, deepslate 3.0, netherrack 0.4, end stone 3.0. Ores 3.0 (deepslate 4.5), chest 2.5, spawner 5.0, ender chest 22.5, ancient debris 30.
 * So ancient debris, ender chests and spawners under plain stone, and debris under netherrack, are not masked. Everything else on the hidden list is.
 */
public final class HideRule {
	private HideRule() {}
	public static boolean safeToMask(float realHardness, float shownHardness) {
		if (realHardness <= 0f || shownHardness <= 0f) return true; // instant blocks have no dig to judge
		return shownHardness * 2f >= realHardness;
	}
}
