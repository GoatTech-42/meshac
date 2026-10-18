package dev.meshac;

/** A legit bounce off a restitution block, so the rise it hands back does not read as jump-dy, high-jump or microhop.
 *  Armed the moment a real fall ends on the block (BEFORE the jump/high-jump room math runs: the launch packet itself
 *  is already covered), with the energy cap from the block's own restitution. Modelled on vanilla 26.2 (Entity.restituteMovementAfterCollisions), as a HEURISTIC allowance, not its exact physics:
 *  the bounce restitution is the landed block's getBounceRestitution (slime 1.0, bed 0.66 for living entities, everything else -
 *  honey, ladder, ordinary blocks - 0), sneaking suppresses it, and an impact slower than gravity does not bounce.
 *  Apex of each bounce is restitution^2 x the fall, so a bounce decays into the microhop band: a chain re-landing re-arms
 *  from the last apex until the impact would be slower than gravity. Not implemented: the SUPPRESSES_BOUNCE tag, gravity and air drag terms (the cap below is restitution^2 x fall + 1, an allowance). Pure: no Minecraft types, so sim tests can drive it. */
public final class BounceTracker {
	public static final double MIN_FALL = 1.0;   // a real bounce needs a real fall; also blocks a flat-ground cheat claim
	public static final double CHAIN_MIN = 0.04; // vanilla stops restituting once the impact is slower than gravity (h = g/2)
	private static final long WINDOW_MS = 10_000;
	private boolean armed, chaining;
	private double riseFrom = Double.NaN, peak = Double.NaN, maxRise;
	private long armedAt;

	/** Ground contact at y after a fall of fallH, on a block with this restitution (0 for honey/ladder/ordinary blocks and when sneaking). */
	public void land(double y, double fallH, double restitution, long nowMs) {
		if (armed && restitution > 0 && fallH < CHAIN_MIN && peak - y < CHAIN_MIN) return; // a ground claim while still rising or at the apex is not a landing: it must not drop an armed chain (9n slime-25 second impact tick); restitution 0 (water, ladder, sneak) still cancels
		double eff = fallH;
		if (chaining && !Double.isNaN(peak) && peak - y > eff) eff = peak - y; // a chain re-landing: the decayed fall is the last apex
		armed = restitution > 0 && (fallH >= MIN_FALL || (chaining && eff >= CHAIN_MIN));
		if (armed) { maxRise = eff * restitution * restitution + 1.0; armedAt = nowMs; riseFrom = Double.NaN; peak = y; }
		chaining = armed;
	}

	/** True while the armed bounce plausibly covers this position: rise from the first rise after landing, capped at the energy the fall handed back, 10s window. */
	public boolean bouncing(double y, long nowMs) {
		if (!armed) return false;
		if (nowMs - armedAt > WINDOW_MS) { armed = false; chaining = false; return false; }
		if (Double.isNaN(riseFrom)) riseFrom = y;
		if (y < riseFrom) riseFrom = y; // re-landed lower (walked off mid-chain): re-baseline
		if (Double.isNaN(peak) || y > peak) peak = y;
		if (y - riseFrom > maxRise) { armed = false; chaining = false; return false; }
		return true;
	}

	public boolean armed() { return armed; }

	/** Block row to read for restitution at a claimed landing y: floor(y - 0.2), the y of vanilla 26.2 getOnPosLegacy() = getOnPos(0.2F) (javap of server-26.2.jar). */
	public static int probeBlockY(double landingY) { return (int) Math.floor(landingY - (double) 0.2F); }

	/** X and Z block range {x0, x1, z0, z1} the 0.6-wide player footprint can touch at the claimed landing. Vanilla picks one mainSupportingBlockPos from the real AABB (with fence/wall/gate special cases); we do not reproduce that pick. Callers take the MAX restitution over this range, which is deliberately permissive at pad edges (covers the tested edge cases; may also cover a cheat that lands half off a pad). */
	public static int[] footprint(double x, double z) {
		double h = 0.3;
		return new int[] { (int) Math.floor(x - h), (int) Math.floor(x + h), (int) Math.floor(z - h), (int) Math.floor(z + h) };
	}

	/** Arm only on a real landing contact: the packet claims ground, or the fall was a real one. A resting packet within 0.1 of the surface at a bounce apex (ground false, almost no fall) must not break a chain. */
	public static boolean contact(boolean groundClaim, double fallH) { return groundClaim || fallH >= MIN_FALL; }
}
