import dev.meshac.BounceTracker;

// Pure bounce-physics contract: arm at ground contact from the observed fall, cover exactly the energy the fall hands back.
public class BounceTrackerTest {
	private static int failed = 0;
	private static void check(boolean ok, String what) { if (!ok) { System.out.println("FAIL " + what); failed++; } }

	public static void main(String[] a) {
		// slime 10-fall: apex up to the full fall height (restitution 1.0)
		BounceTracker t = new BounceTracker();
		t.land(71, 10, 1.0, 1000);
		check(t.armed(), "slime 10-fall arms at ground contact");
		check(t.bouncing(71.9, 1050), "launch packet itself already covered (arm precedes the room math)");
		check(t.bouncing(80.9, 2000), "rise up to the fall height covered");
		check(!t.bouncing(83.0, 2100), "rise past fall + 1 margin disarms (fly cannot hide behind the bounce)");
		check(!t.armed(), "disarmed after the cap broke");

		// bed 10-fall: apex restitution^2 x fall = 4.36
		t = new BounceTracker();
		t.land(70.5625, 10, 0.66, 1000);
		check(t.armed(), "bed 10-fall arms");
		check(t.bouncing(70.5625, 1100), "landing packet baselines the rise");
		check(t.bouncing(74.5, 1500), "bed apex 4.36 covered");
		check(!t.bouncing(76.5, 1600), "bed cap 5.36: higher rise disarms");

		// honey / ladder / ordinary block / sneaking (restitution 0 at the call site): no bounce energy, no arm
		for (double r : new double[] {0.0}) {
			t = new BounceTracker();
			t.land(64, 10, r, 1000);
			check(!t.armed(), "restitution 0 never arms (honey, ladder, stone, sneak-landing)");
		}

		// flat-ground cheat claim: a jump is not a fall
		t = new BounceTracker();
		t.land(64, 0, 1.0, 1000);
		check(!t.armed(), "no fall, no arm");

		// small fall under MIN_FALL does not arm either (vanilla bounce from <1 block cannot breach the 1.45 room)
		t = new BounceTracker();
		t.land(64, 0.9, 1.0, 1000);
		check(!t.armed(), "fall under 1 block does not arm");

		// decay chain on bed: apex 4.36, re-land, decayed apex 1.9, re-land, down into the microhop band, still covered
		t = new BounceTracker();
		t.land(70.5625, 10, 0.66, 1000);
		check(t.bouncing(70.5625, 1100), "chain baselines at the pad");
		check(t.bouncing(74.92, 1500), "first apex");
		t.land(70.5625, 0, 0.66, 3000); // fallPeak resets at ground contact, so the re-landing fall reads 0
		check(t.armed(), "chain re-land re-arms from the last apex (4.36 >= 0.04)");
		check(t.bouncing(72.4, 3500), "decayed apex 1.9 covered");
		t.land(70.5625, 0, 0.66, 5000);
		check(t.armed(), "second chain re-land (apex 1.9 >= 0.04)");
		check(t.bouncing(71.4, 5500), "microhop-band decay (dy ~0.116 territory) covered");
		// walking off onto stone ends the chain
		t.land(70.0, 0, 0.0, 7000);
		check(!t.armed(), "ordinary landing disarms");

		// window: an armed bounce does not cover a jump 11s later
		t = new BounceTracker();
		t.land(71, 10, 1.0, 1000);
		check(!t.bouncing(72, 12001), "window expired");

		if (failed > 0) throw new AssertionError(failed + " failures");
		System.out.println("BounceTrackerTest ok");
	}
}
