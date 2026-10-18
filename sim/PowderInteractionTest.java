import dev.meshac.BounceTracker;
import dev.meshac.FpGuards;
import dev.meshac.StepAccumulator;

// Powder-snow x the bounce tracker and step accumulator, over the FpGuards powder rules and the movement worker's stuck-fall fix.
// Vanilla 26.2: powder snow is climbable (fall cancels, descent clamps to the stuck drop (0-0.08)*0.98*1.5 = -0.1176, jump-hold climbs at 0.2/tick).
public class PowderInteractionTest {
	private static int failed = 0;
	private static void check(boolean ok, String what) { if (!ok) { System.out.println("FAIL " + what); failed++; } }

	public static void main(String[] a) {
		// stuck fall (descent): the step accumulator never counts negative dy, ground-claimed or not
		StepAccumulator st = new StepAccumulator();
		for (int i = 0; i < 30; i++) check(st.packet(i % 2 == 0, FpGuards.POWDER_STUCK_DY, false, true) == null, "descent counted at " + i);
		check(true, "descent clean");

		// resting on the floor inside powder (dy ~0, supported): no count
		st = new StepAccumulator();
		for (int i = 0; i < 30; i++) check(st.packet(true, 0.0, false, true) == null, "resting counted at " + i);
		check(FpGuards.restingInPowder(true, true, 0.0) && !FpGuards.restingInPowder(true, false, 0.0) && !FpGuards.restingInPowder(false, true, 0.0), "restingInPowder needs body, support and near-zero dy");

		// powder climb ground-claimed at 0.2/tick: without the skip the accumulator flags in 4 packets (0.8 > 0.65)
		st = new StepAccumulator();
		boolean flaggedOld = false;
		for (int i = 0; i < 4; i++) flaggedOld |= st.packet(true, 0.2, false, true) != null; // no skip
		check(flaggedOld, "interaction proven: ground-claimed powder climb trips the step accumulator without the skip");
		// verified in-powder climb (body overlaps, dy 0.2 and 0.21 alternating): 40 packets clean
		st = new StepAccumulator();
		boolean flaggedNew = false;
		for (int i = 0; i < 40; i++) { double dy = i % 2 == 0 ? 0.2 : 0.21; flaggedNew |= st.packet(true, dy, false, true, FpGuards.powderClimb(dy, true)) != null; }
		check(!flaggedNew, "verified in-powder climb (0.2/0.21) stays clean");
		// ADVERSARIAL: near powder but body NOT inside (powderClimb false) - the same 0.21 claim must accumulate and flag
		st = new StepAccumulator();
		boolean nearFlags = false;
		for (int i = 0; i < 4; i++) nearFlags |= st.packet(true, 0.21, false, true, FpGuards.powderClimb(0.21, false)) != null;
		check(nearFlags, "near-but-not-in-powder 0.21 ground claim still flags (skip requires body intersection)");
		// ADVERSARIAL: mixed 0.2/0.3 window with body in powder - the 0.3 packets are above the climb band and must count
		st = new StepAccumulator();
		boolean mixedFlags = false;
		for (double dy : new double[] {0.3, 0.2, 0.3, 0.3}) mixedFlags |= st.packet(true, dy, false, true, FpGuards.powderClimb(dy, true)) != null;
		check(mixedFlags, "mixed 0.3/0.2/0.3/0.3 window in powder flags (0.3 is outside the band and accumulates)");
		// ADVERSARIAL, side-by-side: the old 4-arg accumulator counted every ground-claimed rise (alternating 0.3/0.2 = 1.0 > 0.65 flags);
		// the fold-back keeps the new 5-arg behavior equivalent for that sequence - the band exemption applies only to an all-climb window.
		StepAccumulator oldAcc = new StepAccumulator(), newAcc = new StepAccumulator();
		boolean oldFlags = false, newFlags = false;
		for (int i = 0; i < 4; i++) {
			double dy = i % 2 == 0 ? 0.3 : 0.2;
			oldFlags |= oldAcc.packet(true, dy, false, true) != null;
			newFlags |= newAcc.packet(true, dy, false, true, FpGuards.powderClimb(dy, true)) != null;
		}
		check(oldFlags, "side-by-side: old signature flags alternating 0.3/0.2 (1.0 in the window)");
		check(newFlags, "side-by-side: fold-back flags alternating 0.3/0.2 too (above-band packet folds the skipped climb back)");
		// the intentional band exemption, documented: a SUSTAINED ground-claimed climb at the vanilla powder rate (dy <= 0.21, body in powder) is accepted
		st = new StepAccumulator();
		boolean bandClean = true;
		for (int i = 0; i < 40; i++) bandClean &= st.packet(true, 0.21, false, true, true) == null;
		check(bandClean, "intentional band exemption: sustained 0.21 in-powder climb stays clean (accepted trajectory, documented)");

		// ADVERSARIAL, leading hack then band: [0.3, 0.2, 0.2, 0.2] taints the window - old and new both flag (0.9)
		oldAcc = new StepAccumulator(); newAcc = new StepAccumulator();
		oldFlags = false; newFlags = false;
		for (double dy : new double[] {0.3, 0.2, 0.2, 0.2}) {
			oldFlags |= oldAcc.packet(true, dy, false, true) != null;
			newFlags |= newAcc.packet(true, dy, false, true, FpGuards.powderClimb(dy, true)) != null;
		}
		check(oldFlags, "leading-hack control: old signature flags [0.3,0.2,0.2,0.2] (0.9)");
		check(newFlags, "leading-hack: tainted window counts the trailing band packets (0.9)");

		// ADVERSARIAL, reverse order: [0.2, 0.2, 0.2, 0.3] folds at the 0.3 - old and new both flag (0.9)
		oldAcc = new StepAccumulator(); newAcc = new StepAccumulator();
		oldFlags = false; newFlags = false;
		for (double dy : new double[] {0.2, 0.2, 0.2, 0.3}) {
			oldFlags |= oldAcc.packet(true, dy, false, true) != null;
			newFlags |= newAcc.packet(true, dy, false, true, FpGuards.powderClimb(dy, true)) != null;
		}
		check(oldFlags, "reverse-order control: old signature flags [0.2,0.2,0.2,0.3] (0.9)");
		check(newFlags, "reverse-order: above-band packet folds the skipped climb (0.9)");

		// flag resets ALL window state: after a flag, one hack packet must not re-charge the old skipped rise
		st = new StepAccumulator();
		boolean firstFlag = false;
		for (double dy : new double[] {0.2, 0.2, 0.2, 0.3}) firstFlag |= st.packet(true, dy, false, true, FpGuards.powderClimb(dy, true)) != null;
		check(firstFlag, "setup: first window flags");
		String after = st.packet(true, 0.2, false, true, FpGuards.powderClimb(0.2, true));
		check(after == null, "after the flag a climb packet starts a clean window");
		after = st.packet(true, 0.3, false, true, FpGuards.powderClimb(0.3, true)); // powderClimb(0.3,true) = false: the real trajectory - above the climb band, so the 0.3 folds the new window's skipped 0.2
		check(after == null, "after the flag a single 0.3 folds only the new skipped rise (0.5, no double charge)");
		// exit transition: climb in powder, then a sub-band rise out of powder - counts on its own, skipped climb does NOT fold (no FP on exit)
		st = new StepAccumulator();
		for (int i = 0; i < 3; i++) st.packet(true, 0.2, false, true, true);
		check(st.packet(true, 0.125, false, true, false) == null, "climb-then-exit onto a snow layer does not fold (no exit FP)");
		// ADVERSARIAL: repeated band-max rises with body in powder stay clean only while the body check holds; flip it mid-window and the window flags
		st = new StepAccumulator();
		String m = null;
		for (int i = 0; i < 3; i++) st.packet(true, 0.2, false, true, true);
		m = st.packet(true, 0.2, false, true, false); // body left the powder: this packet counts (0.2), and the window had no counted rise yet - no flag, but the window is now armed
		check(m == null, "single counted 0.2 after skipped packets does not flag by itself");
		boolean flipFlags = false;
		for (int i = 0; i < 4; i++) flipFlags |= st.packet(true, 0.2, false, true, false) != null;
		check(flipFlags, "continued ground-claimed 0.2 once the body is out flags (0.8 in one window)");

		// the climb's first kick vs the air-jump counter: switching from the stuck fall to the 0.2 climb is one kick, never a flag
		{
			int ajN = 0, sinceJump = 99, flags = 0; long ajAt = 0; double prev = FpGuards.POWDER_STUCK_DY;
			for (int i = 0; i < 60; i++) {
				long ms = i * 50L; double dy = 0.2, expect = FpGuards.stuckExpect((prev - 0.08) * 0.98, true);
				sinceJump++;
				if (FpGuards.airKick(dy, expect)) { ajN = FpGuards.airKickCount(ajN, ms, ajAt); ajAt = ms; if (FpGuards.airJumpFlag(sinceJump, ajN)) flags++; sinceJump = 0; }
				prev = dy;
			}
			check(flags == 0, "powder climb is one kick at most, air-jump stays quiet");
		}

		// bounce tracker x powder: a fall ending on powder snow carries restitution 0 (powder is climbable, vanilla cancels the fall) - no arm
		BounceTracker t = new BounceTracker();
		t.land(64, 10, 0.0, 1000);
		check(!t.armed(), "powder landing never arms the bounce tracker");
		// a powder climb right after a slime landing next to the column stays inside the armed cap - no disarm churn
		t = new BounceTracker();
		t.land(71, 10, 1.0, 1000);
		check(t.bouncing(71.2, 1050) && t.bouncing(71.4, 1100), "powder climb beside an armed pad is covered, not churned");

		if (failed > 0) throw new AssertionError(failed + " failures");
		System.out.println("PowderInteractionTest ok");
	}
}
