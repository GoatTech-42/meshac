package dev.meshac;

/**
 * The ground-rise accumulator behind the "step rose" flag, pure so the stair-tolerant rule (case MA-M9HG) can run the
 * legit battery without a server. A single vanilla auto-step (slab, stair, anything 0.42 to 0.66 high, plus the jump
 * boost band) resets the window and never counts; other onGround rises add up over a 4-packet window.
 */
public final class StepAccumulator {
	public static final double STEP_MIN = 0.42, STEP_MAX = 0.66, LIMIT = 0.65;
	private double rise, skipped;
	private int riseT;
	private boolean tainted;

	/** One position packet. Returns the flag message when the window passes the limit, else null. flagAllowed carries STEP_ACCUMULATOR_ENABLED && hit == null, matching the original reset semantics exactly. */
	public String packet(boolean ground, double dy, boolean jumpBoost, boolean flagAllowed) { return packet(ground, dy, jumpBoost, flagAllowed, false); }

	/** powderClimb marks a verified in-powder climb packet (FpGuards.powderClimb: body overlaps powder, dy in the 0.2 band). The exemption holds only while the window is ALL climb: a rise above the band is not a climb, so the skipped climb folds back into the counted rise (alternating band/hack packets total 1.0 in a window and flag - sim PowderInteractionTest). A sub-band rise while out of powder counts on its own and ends the exemption. */
	public String packet(boolean ground, double dy, boolean jumpBoost, boolean flagAllowed, boolean powderClimb) {
		double band = 0.1 * (jumpBoost ? 3 : 0);
		if (ground && dy >= STEP_MIN && dy <= STEP_MAX + band) { rise = 0; skipped = 0; riseT = 0; tainted = false; }
		else {
			if (riseT > 0 && --riseT <= 0) { rise = 0; skipped = 0; tainted = false; } // the window runs down every packet and does not renew on counted rises: a continuous legit climb of partial tops (snow 0.125, carpet 0.0625) must not accumulate forever (sim: old renewal flagged any sub-0.42 ramp after 0.65 total)
			if (ground && dy > 0.05) {
				if (riseT <= 0) { rise = 0; skipped = 0; riseT = 4; tainted = false; }
				if (powderClimb && !tainted) skipped += dy;
				else if (dy > FpGuards.POWDER_CLIMB_DY) { rise += dy + skipped; skipped = 0; tainted = true; } // above the climb band: the window was not a plausible powder climb - fold the skipped rise back and taint the window, so later band packets count too (leading-hack then band)
				else { rise += dy; skipped = 0; } // below the band but no powder (or a tainted window): counts on its own, climb exemption over
			}
		}
		if (flagAllowed && rise > LIMIT + band) { String m = String.format(java.util.Locale.ROOT, "step rose %.2f on the ground", rise); rise = 0; skipped = 0; riseT = 0; tainted = false; return m; } // reset ALL window state: skipped must not be charged again after the flag
		return null;
	}
}
