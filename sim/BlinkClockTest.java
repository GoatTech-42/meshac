import dev.meshac.BlinkClock;

/**
 * Synthetic 20 tps client timelines through the real BlinkClock: pins the blink-duration floor the README number implies,
 * the tick-end discriminator, the 150 ms release window, and the no-FP controls. No Minecraft needed.
 * Timeline model: move packets at t=50,100,...,2000 (x += 0.2 per tick, sprint-walk pace). A blink starts at 2000+phase
 * (phase in [0,50)) and lasts blinkMs; the held packets (due at 2050, 2100, ... up to release) then land releaseStep apart.
 * Tick-end packets keep arriving every 50 ms during the blink unless holdTickEnds (a real network stall holds those too).
 */
public final class BlinkClockTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	static final long MS = 1_000_000L;

	/** Returns the gap (ms) at which the hit fired, 0 for no hit, or -1 for a hit during normal movement (false positive in the harness). */
	static double run(double blinkMs, double phase, boolean holdTickEnds, double releaseStepMs, double movePerTick) {
		BlinkClock c = new BlinkClock();
		double x = 0;
		long t = 0;
		for (int i = 0; i < 40; i++) { // 2 s of normal movement, must never hit
			t += 50; x += movePerTick;
			if (c.arrive(t * MS, x, 0)) return -1;
			c.tickEnd();
		}
		long blinkStart = 2000 + (long) phase, release = blinkStart + (long) blinkMs;
		java.util.List<Long> heldTicks = new java.util.ArrayList<>();
		for (long pt = 2050; pt <= release; pt += 50) heldTicks.add(pt);
		long teAt = 2050;
		for (int i = 0; i < heldTicks.size(); i++) {
			long at = release + (long) (i * releaseStepMs);
			if (!holdTickEnds) while (teAt <= at) { c.tickEnd(); teAt += 50; } // tick-ends flow in real time through the blink
			x += movePerTick;
			if (c.arrive(at * MS, x, 0)) return release - 2000.0;
		}
		return 0;
	}

	public static void main(String[] a) {
		double[] phases = {0, 12.5, 25, 37.5, 49.9};
		// 1. Floor sweep: detection iff phase + blinkMs >= 500 (the measured gap), nothing below, always at >= 500.
		for (double phase : phases) {
			double dmin = -1;
			for (double d = 300; d <= 900; d += 5) if (run(d, phase, false, 1.0, 0.2) > 0) { dmin = d; break; }
			check(dmin > 0, "no detection at all for phase " + phase);
			check(phase + dmin >= 500 && phase + dmin < 505, "floor for phase " + phase + " should be 500-phase, got blinkMs=" + dmin);
			check(run(dmin - 5, phase, false, 1.0, 0.2) == 0, "detected below the floor at " + (dmin - 5) + " phase " + phase);
		}
		for (double phase : phases) {
			check(run(450, phase, false, 1.0, 0.2) == 0, "0.45 s blink detected at phase " + phase + " (max gap 499.9)");
			check((run(455, phase, false, 1.0, 0.2) > 0) == (phase + 455 >= 500), "0.455 s blink outcome should follow phase+455 vs the 500 ms gate, phase " + phase);
			check(run(750, phase, false, 1.0, 0.2) > 0, "0.75 s blink (rig-proven point) missed at phase " + phase);
			check(run(400, phase, false, 1.0, 0.2) == 0, "0.4 s blink (rig-proven miss) detected at phase " + phase);
		}
		// 2. Theoretical floor is ~0.45-0.5 s phase-dependent; the README's 0.75 s is the rig-tested point, not the floor.
		// 3. Tick-end discriminator: a stall that also holds tick-end packets never opens a candidate, however long.
		for (double phase : phases) check(run(2000, phase, true, 1.0, 0.2) == 0, "full stall (tick-ends held) detected, phase " + phase);
		// Tick-ends at half rate during the gap: 0.5 < 0.7 of expected, candidate must not open.
		{
			BlinkClock c = new BlinkClock();
			double x = 0; long t = 0;
			for (int i = 0; i < 40; i++) { t += 50; x += 0.2; check(!c.arrive(t * MS, x, 0), "hit in warm-up"); c.tickEnd(); }
			for (int i = 0; i < 20; i++) { // 1 s blink, tick-ends every 100 ms, packets flushed at once
				if (i % 2 == 0) c.tickEnd();
				x += 0.2;
				check(!c.arrive((3000 + i) * MS, x, 0), "half-rate tick-ends still opened a candidate");
			}
		}
		// 4. The 150 ms release window, both sides of the boundary: 6th packet at 145 ms hits, at 155 ms resets.
		check(run(500, 10, false, 29.0, 0.2) > 0, "release inside the window (6th at 145 ms) should hit");
		check(run(500, 10, false, 31.0, 0.2) == 0, "trickled release (6th at 155 ms) should miss: known gap, blink hacks that dribble");
		check(run(750, 10, false, 40.0, 0.2) == 0, "slow release of a 0.75 s blink should miss");
		// 5. Long blink still caught (needs 60 of ~100 packets in the window; an instant flush delivers all).
		check(run(5000, 25, false, 1.0, 0.2) > 0, "5 s blink missed");
		// 6. No-FP controls: jittered walk, a 300 ms lag bunch with stalled tick-ends, a stationary player, sneak-pace movement.
		{
			BlinkClock c = new BlinkClock();
			double x = 0; long t = 0; java.util.Random r = new java.util.Random(7);
			for (int i = 0; i < 2400; i++) { // 120 s of jittered normal play
				t += 50 + (long) (r.nextDouble() * 16 - 8); x += 0.2;
				check(!c.arrive(t * MS, x, 0), "jittered walk hit at tick " + i);
				c.tickEnd();
			}
		}
		check(run(300, 25, true, 1.0, 0.2) == 0, "300 ms lag bunch flagged");
		check(run(750, 25, false, 1.0, 0.0) == 0, "blink by a stationary player flagged");
		check(run(750, 25, false, 1.0, 0.05) == 0, "blink at sneak pace flagged (under the 0.1 block movement gate)");
		System.out.println(fails == 0 ? "BlinkClockTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
