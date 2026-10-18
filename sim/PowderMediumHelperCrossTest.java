import dev.meshac.BounceTracker;
import dev.meshac.FpGuards;
import dev.meshac.MediumExit;
import dev.meshac.MediumFootprint;
import dev.meshac.StepAccumulator;

/** Powder/medium HELPER cross-product, not a production-order replay. It calls the helpers Movement calls with the booleans Movement derives, but omits flyMedAt 400 ms gate, ground/exemption resets, hit precedence, and the monitor/pending/teleport resets (all in Movement, uncompiled and native-unwitnessed). Production keeps two powder booleans: powderNear (feet/above/below/below2 cell heuristic, only widens the fly powderRise exemption) and the pose-box body overlap (stuckExpect, restingInPowder, step powderClimb). This test mirrors both. */
public final class PowderMediumHelperCrossTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	static boolean has(int[] c, int x, int y, int z) { return x >= c[0] && x <= c[1] && y >= c[2] && y <= c[3] && z >= c[4] && z <= c[5]; }

	static final class Rep {
		final MediumExit exit = new MediumExit(); final BounceTracker bounce = new BounceTracker(); final StepAccumulator step = new StepAccumulator();
		int buf, ajN; long ajAt; int sinceJump = 99; double prevDy; long now = 1_000_000; boolean fly, airJump; String stepHit; double y = 64;
		void hardMedium(double dy) { exit.onMedium(now); prevDy = dy; now += 50; }
		/** one air packet; web/powder are the body-overlap booleans Movement computes from the real pose box. */
		void air(double dy, boolean web, boolean powder) { air(dy, web, powder, powder); }
		void air(double dy, boolean web, boolean powder, boolean powderNear) {
			boolean firstExit = exit.firstAfter(now, dy);
			boolean stuckNow = FpGuards.stuckNow(dy, web, false);
			y += dy;
			if (!stuckNow) {
				double expect = FpGuards.stuckExpect((prevDy - 0.08) * 0.98, powder);
				boolean powderRise = FpGuards.powderRise(dy, powderNear);
				if (!firstExit) buf = FpGuards.flyBuf(buf, dy, expect, powderRise, FpGuards.restingInPowder(powder, true, dy));
				if (!firstExit && buf >= 10) fly = true;
				sinceJump++;
				if (FpGuards.airKick(dy, expect) && !firstExit && !bounce.bouncing(y, now)) {
					ajN = FpGuards.airKickCount(ajN, now, ajAt); ajAt = now;
					if (FpGuards.airJumpFlag(sinceJump, ajN)) airJump = true;
					sinceJump = 0;
				}
			}
			prevDy = dy; now += 50;
		}
	}

	public static void main(String[] a) {
		// 1. preloaded fly buffer at threshold, then a vanilla exit packet: waived packet must not emit and must not change the buffer
		Rep r = new Rep(); r.buf = 10; r.hardMedium(0.02); r.air(0.30, false, false);
		check(!r.fly && r.buf == 10, "firstExit with buf>=10 must not emit, buf=" + r.buf);
		r.air(0.30, false, false); // the NEXT packet is not waived: the buffer is real and may now emit (intent: the waiver is one packet)
		check(r.fly, "the packet after the waiver is judged normally");

		// 2. hard medium exit straight into powder on the same packet: waived, no air jump, no fly
		r = new Rep(); r.hardMedium(0.02); r.air(0.20, false, true);
		check(!r.fly && !r.airJump && r.buf == 0, "hard medium -> powder same packet");

		// 3. web+powder body overlap: stuckNow gate wins, a small dy never reaches fly/airKick
		r = new Rep(); r.buf = 5; r.air(0.04, true, true);
		check(!r.fly && !r.airJump && r.buf == 5, "web+powder overlap: gate skips the packet, buf=" + r.buf);

		// 4. bounce alive -> web -> powder, restitution>0: re-launch is not an air jump; with restitution 0 (sneak/no bounce) it counts
		r = new Rep(); r.bounce.land(r.y, 3.0, 1.0, r.now); r.air(0.9, false, false);
		check(!r.airJump && r.ajN == 0, "bounce alive: the re-launch kick is not counted");
		r.air(0.04, true, false); r.air(0.2, false, true);
		check(!r.airJump, "bounce -> web -> powder does not air-jump on one packet");
		Rep z = new Rep(); z.bounce.land(z.y, 3.0, 0.0, z.now); z.air(0.9, false, false);
		check(z.ajN == 1, "restitution 0: no bounce armed, kick counted once, ajN=" + z.ajN);
		z.air(-0.1, false, false); z.air(0.9, false, false);
		check(z.airJump, "restitution 0: two kicks in 8 packets still flag");

		// 5. tainted step ramp: powder climbs skip, then a rise above the band folds the skipped rise back
		StepAccumulator s = new StepAccumulator(); String h = null;
		h = s.packet(true, 0.2, false, true, true); check(h == null, "climb 1 skipped");
		h = s.packet(true, 0.2, false, true, true); check(h == null, "climb 2 skipped");
		h = s.packet(true, 0.3, false, true, false); check(h != null, "a 0.3 rise after 0.4 skipped folds back to 0.7 and flags: " + h);
		s = new StepAccumulator();
		s.packet(true, 0.2, false, true, true); h = s.packet(true, 0.25, false, true, false);
		check(h == null, "0.2 skipped + 0.25 = 0.45 under the 0.65 limit: legit exit step does not flag");
		s = new StepAccumulator();
		for (int i = 0; i < 4; i++) { h = s.packet(true, 0.2, false, true, true); check(h == null, "pure climb never flags " + i); }
		h = s.packet(true, 0.3, false, true, false); check(h == null, "the 4-packet window has run down after a long climb (skipped cleared), so a lone 0.3 rise is not folded back: documented window limit, " + h);

		// 5b. powderNear only (adjacent/below cell, body NOT overlapping): production's powderRise heuristic still exempts a 0.2 rise from the fly buffer (FP-first, pre-existing 9r behaviour, kept on purpose); stuckExpect and the step climb skip do NOT apply
		r = new Rep(); r.air(0.20, false, false, true);
		check(r.buf == 0, "adjacent-only powder: 0.2 rise exempt from fly via powderNear, buf=" + r.buf);
		r = new Rep(); r.air(0.20, false, false, false);
		check(r.buf > 0, "no powder anywhere: the same 0.2 rise counts, buf=" + r.buf);
		check(!FpGuards.powderClimb(0.20, false), "adjacent-only: step accumulator gets no climb skip (body must overlap)");
		check(FpGuards.stuckExpect(-0.2, false) == -0.2, "adjacent-only: no stuckExpect floor without body overlap");

		// 6. real pose box vs feet/above cell pair: a wide body at an x cell edge overlaps a powder cell the block position misses; a crouching box stops one cell lower
		int[] wide = MediumFootprint.cells(0.8, 64.0, 0.2, 1.4, 65.8, 0.8);
		check(has(wide, 1, 64, 0), "wide/edge body overlaps the powder cell at x=1 that blockPosition (x=0) misses");
		int[] stand = MediumFootprint.cells(0.2, 64.0, 0.2, 0.8, 65.8, 0.8), crouch = MediumFootprint.cells(0.2, 64.0, 0.2, 0.8, 65.5, 0.8);
		check(has(stand, 0, 65, 0) && has(crouch, 0, 65, 0), "stand and crouch both reach y=65 cell at 65.5+");
		int[] swim = MediumFootprint.cells(0.2, 64.0, 0.2, 0.8, 64.6, 0.8);
		check(!has(swim, 0, 65, 0), "a swim/crawl box (0.6 tall) does not reach the above cell, so no overlap is claimed there");

		System.out.println(fails == 0 ? "PowderMediumHelperCrossTest ok" : "PowderMediumHelperCrossTest FAILED " + fails);
		if (fails != 0) System.exit(1);
	}
}
