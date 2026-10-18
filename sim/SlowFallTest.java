import dev.meshac.FpGuards;

/** Slow Falling (gravity 0.01) walks dy through -0.118 and builds the fly buffer if unguarded (live flags: "fly dy -0.118 expect -0.194").
 *  The fix keys on server state: the medium/effect flag, never on dy. A bare constant sink with no medium must still flag (RF1). */
public final class SlowFallTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	/** Slow-falling fall, medium flag from the server for ticks [0, mediumTicks). Returns the buffer peak. */
	static int slowFall(int ticks, int mediumTicks) {
		double prev = 0, v = 0; int buf = 0, peak = 0; long ms = 0, mediumAt = -10000;
		for (int t = 0; t < ticks; t++, ms += 50) {
			v = (v - (t < mediumTicks ? 0.01 : 0.08)) * 0.98; // after the effect ends vanilla gravity resumes
			boolean in = t < mediumTicks; if (in) mediumAt = ms;
			double expect = (prev - 0.08) * 0.98;
			if (in || FpGuards.flyMediumExempt(false, ms - mediumAt)) { buf = 0; prev = v; continue; }
			buf = FpGuards.flyBuf(buf, v, expect, false, false); peak = Math.max(peak, buf); prev = v;
		}
		return peak;
	}
	public static void main(String[] a) {
		check(Math.abs((-0.118 - 0.08) * 0.98 - -0.194) < 0.001, "expect arithmetic");
		int unguarded = 0; { double prev = 0, v = 0; int buf = 0; for (int t = 0; t < 40; t++) { v = (v - 0.01) * 0.98; buf = FpGuards.flyBuf(buf, v, (prev - 0.08) * 0.98, false, false); unguarded = Math.max(unguarded, buf); prev = v; } }
		check(unguarded >= 10, "unguarded slow fall should build the buffer (documents the live flag), got " + unguarded);
		check(slowFall(40, 40) == 0, "slow falling with the effect flag built a buffer");
		check(slowFall(60, 40) == 0, "effect ending mid-air, then vanilla gravity, built a buffer");
		check(FpGuards.flyMediumExempt(true, 99999) && FpGuards.flyMediumExempt(false, 100) && !FpGuards.flyMediumExempt(false, 400), "grace bounds");
		// RF1: hover cheat with constant -0.118 sink, no medium, long after any medium
		int buf = 0, peak = 0; double prev = -0.118;
		for (int i = 0; i < 20; i++) { buf = FpGuards.flyBuf(buf, -0.118, (prev - 0.08) * 0.98, false, false); peak = Math.max(peak, buf); }
		check(!FpGuards.flyMediumExempt(false, 60000) && peak >= 10, "RF1 constant sink without a medium must flag, peak " + peak);
		System.out.println(fails == 0 ? "SlowFallTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
