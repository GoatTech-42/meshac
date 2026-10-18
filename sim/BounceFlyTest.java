import dev.meshac.BounceTracker;
import dev.meshac.FpGuards;

// Bounce chains vs the fly buffer and the air-jump kick counter, with and without the BounceTracker guard Movement uses.
// Physics per 26.2 Entity.restituteMovementAfterCollisions: v' = (0.08 - vImpact) x restitution at the pad, (v-0.08)x0.98 chain between packets.
public class BounceFlyTest {
	private static double prevExpect(int i) { return i % 2 == 0 ? -0.1 : 0.5; }
	private static int failed = 0;
	private static void check(boolean ok, String what) { if (!ok) { System.out.println("FAIL " + what); failed++; } }

	private static double impactSpeed(double h) { double v = 0, d = 0; while (d < h) { v = (v - 0.08) * 0.98; d -= v; if (v < -4) break; } return v; }

	/** Replays a vanilla bounce chain; returns {flyFlags, airJumpFlags, maxFlyBuf}. guarded mirrors Movement: the fly buffer and kick clock always update; only the airKick branch is skipped while the tracker covers the rise. */
	private static int[] chain(double fallH, double r, boolean guarded) {
		BounceTracker t = new BounceTracker();
		double v = impactSpeed(fallH), prevDy = v, y = 0;
		int buf = 0, maxBuf = 0, flyFlags = 0, ajN = 0, ajFlags = 0, sinceJump = 99, launches = 0;
		long ajAt = 0, ms = 0;
		boolean firstLand = true;
		while (launches < 60) {
			double v0 = (0.08 - v) * r;
			if (v0 < 0.08) break; // vanilla stop: impact slower than gravity does not bounce
			if (firstLand) { t.land(0, fallH, r, ms); firstLand = false; } else t.land(0, 0, r, ms); // fallPeak resets at contact: re-lands read fallH 0
			launches++;
			double vv = v0;
			while (true) {
				ms += 50;
				double expect = (prevDy - 0.08) * 0.98;
				y += vv; prevDy = vv; // Movement reads y from the packet first: bouncing() sees the endpoint, not the start
				boolean covered = guarded && t.bouncing(y, ms);
				buf = FpGuards.flyBuf(buf, vv, expect, false, false); // Movement always updates the fly buffer and the kick clock; only the airKick branch is guarded
				maxBuf = Math.max(maxBuf, buf);
				if (buf >= 10) flyFlags++;
				sinceJump++;
				if (!covered && FpGuards.airKick(vv, expect)) { ajN = FpGuards.airKickCount(ajN, ms, ajAt); ajAt = ms; if (FpGuards.airJumpFlag(sinceJump, ajN)) ajFlags++; sinceJump = 0; }
				vv = (vv - 0.08) * 0.98;
				if (y <= 0 && vv < 0) { y = 0; v = vv; break; }
			}
		}
		return new int[] {flyFlags, ajFlags, maxBuf};
	}

	public static void main(String[] a) {
		// unguarded: air-jump flags on every sustained chain (kicks land inside the 2.5s window even at slime spacing); fly peaks at buf 3, never flags
		for (double[] cfg : new double[][] {{10, 1.0}, {25, 1.0}, {10, 0.66}, {5.61, 0.66}}) {
			int[] r = chain(cfg[0], cfg[1], false);
			check(r[1] > 50, "unguarded air-jump flags the whole chain fallH=" + cfg[0] + " r=" + cfg[1] + ": " + r[1]);
			check(r[2] < 10, "unguarded fly never reaches the signal bar on a bounce chain fallH=" + cfg[0] + ": " + r[2]);
		}
		// guarded: nothing flags
		for (double[] cfg : new double[][] {{10, 1.0}, {25, 1.0}, {10, 0.66}, {5.61, 0.66}}) {
			int[] r = chain(cfg[0], cfg[1], true);
			check(r[0] == 0 && r[1] == 0, "guarded chain clean fallH=" + cfg[0] + " r=" + cfg[1] + ": fly=" + r[0] + " air=" + r[1]);
		}
		// control: an impulse hack (repeated fresh kicks in mid-air, no bounce) still flags with the guard code path active -
		// an unarmed tracker never covers anything
		{
			BounceTracker t = new BounceTracker();
			int ajN = 0, sinceJump = 99, flags = 0; long ajAt = 0; double prevDy = -0.1, y = 100;
			for (int i = 0; i < 40; i++) {
				long ms = i * 50L; double dy = i % 2 == 0 ? 0.5 : -0.1, expect = (prevDy - 0.08) * 0.98;
				y += dy;
				boolean covered = t.bouncing(y, ms); // unarmed: always false
				sinceJump++;
				if (!covered && FpGuards.airKick(dy, expect)) { ajN = FpGuards.airKickCount(ajN, ms, ajAt); ajAt = ms; if (FpGuards.airJumpFlag(sinceJump, ajN)) flags++; sinceJump = 0; }
				prevDy = dy;
			}
			check(flags > 0, "impulse hack still flags air-jump (guard does not open a hole without a bounce)");
		}
		// control: an armed tracker stops covering once the rise passes the cap, and once the 10s window expires - kicks flag again
		{
			BounceTracker t = new BounceTracker();
			t.land(71, 10, 1.0, 1000); // slime 10-fall: cap 11
			check(t.bouncing(71, 1050), "control armed");
			check(!t.bouncing(82.5, 1100), "rise past the cap drops the cover");
			BounceTracker t2 = new BounceTracker();
			t2.land(71, 10, 1.0, 1000);
			check(!t2.bouncing(72, 12001), "window-expired tracker drops the cover");
			int ajN = 0, sinceJump = 99, flags = 0; long ajAt = 0;
			for (int i = 0; i < 10; i++) {
				long ms = 13000 + i * 50L; double dy = i % 2 == 0 ? 0.5 : -0.1, expect = (prevExpect(i) - 0.08) * 0.98;
				sinceJump++;
				if (!t2.bouncing(72 + i * dy, ms) && FpGuards.airKick(dy, expect)) { ajN = FpGuards.airKickCount(ajN, ms, ajAt); ajAt = ms; if (FpGuards.airJumpFlag(sinceJump, ajN)) flags++; sinceJump = 0; }
			}
			check(flags > 0, "kicks flag again once the cover is gone");
		}
		if (failed > 0) throw new AssertionError(failed + " failures");
		System.out.println("BounceFlyTest ok");
	}
}
