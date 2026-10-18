import dev.meshac.FpGuards;

/** javac -d /tmp/t src/main/java/dev/meshac/FpGuards.java sim/FpGuardsTest.java && java -cp /tmp/t FpGuardsTest */
public final class FpGuardsTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	/** Vanilla fall, one packet per tick; dropTick merges that tick with the next into one packet. Returns the highest fly buffer reached. */
	static int fall(int ticks, int dropTick, boolean guard) {
		double v = 0; double prevDy = 0; int buf = 0, peak = 0; double prevDt = 0.05;
		double acc = 0; double dt = 0.05;
		for (int t = 0; t < ticks; t++) {
			v = (v - 0.08) * 0.98; acc += v; double packetDt = 0.05;
			if (t == dropTick) { dt += 0.05; continue; } // the client sent nothing this tick: the next packet carries two ticks
			double dy = acc; acc = 0; packetDt = dt; dt = 0.05;
			double expect = (prevDy - 0.08) * 0.98;
			buf = FpGuards.flyBuf(buf, dy, expect, false, guard && FpGuards.staleFall(dy, prevDt));
			peak = Math.max(peak, buf); prevDy = dy; prevDt = packetDt;
		}
		return peak;
	}
	public static void main(String[] a) {
		// powder snow climb: 12 ticks at dy 0.2 from a standing start, nothing near powder counts, near powder it does not
		int buf = 0, plain = 0; double prev = 0;
		for (int i = 0; i < 12; i++) { double dy = 0.2; double expect = (prev - 0.08) * 0.98; buf = FpGuards.flyBuf(buf, dy, expect, FpGuards.powderRise(dy, true), false); plain = FpGuards.flyBuf(plain, dy, expect, FpGuards.powderRise(dy, false), false); prev = dy; }
		check(buf == 0, "powder snow climb built fly buffer " + buf);
		check(plain >= 10, "the same climb away from powder snow must still flag, got " + plain);
		check(FpGuards.powderRise(0.2, true) && !FpGuards.powderRise(0.2, false), "powderRise needs powder");
		check(!FpGuards.powderRise(0.5, true) && !FpGuards.powderRise(-0.2, true) && !FpGuards.powderRise(0.0, true), "powderRise only covers the 0.2 climb band");
		check(FpGuards.powderRise(0.42, true) == false, "a real jump next to powder snow is not covered by the climb exemption (jump has its own rule)");
		// merged-tick fall: 30 ticks of vanilla falling, one packet lost at tick 12
		check(fall(30, -1, false) == 0, "vanilla fall without loss flagged");
		int noGuard = fall(30, 12, false);
		check(noGuard >= 1, "the lost-packet fall should raise the buffer without the guard (got " + noGuard + "); this is the live 'fly dy -0.161 expect -0.341' signature");
		check(fall(30, 12, true) == 0, "guard still left a buffer for a lost packet");
		// repeated lost packets (bad link): one in four packets
		int bad = 0; for (int d = 4; d < 40; d += 4) bad = Math.max(bad, fall(60, d, true));
		check(bad == 0, "jittery fall flagged with the guard, peak " + bad);
		// a hover (dy 0) or slow constant sink while claiming air, at normal packet rate, is still caught
		int hb = 0; double pd = 0; for (int i = 0; i < 20; i++) { double dy = -0.05; double expect = (pd - 0.08) * 0.98; hb = FpGuards.flyBuf(hb, dy, expect, false, FpGuards.staleFall(dy, 0.05)); pd = dy; }
		check(hb >= 10, "slow sink hack at normal packet rate must flag, got " + hb);
		check(!FpGuards.staleFall(0.1, 0.5) && !FpGuards.staleFall(-0.1, 0.06) && FpGuards.staleFall(-0.1, 0.1), "staleFall bands");
		// rig FP: 3-deep powder snow, steady stuck fall. dy -0.118 right after -0.118 (expect -0.194 from free fall)
		double free = (-0.118 - 0.08) * 0.98;
		check(Math.abs(free - -0.194) < 0.001, "free-fall chain gives the -0.194 the rig printed: " + free);
		check(Math.abs(FpGuards.POWDER_STUCK_DY - -0.1176) < 1e-9, "stuck fall is -0.1176");
		int pb = 0; for (int i = 0; i < 40; i++) pb = FpGuards.flyBuf(pb, -0.118, FpGuards.stuckExpect(free, true), false, false);
		check(pb == 0, "40 ticks of steady powder fall built fly buffer " + pb);
		int nb = 0; for (int i = 0; i < 40; i++) nb = FpGuards.flyBuf(nb, -0.118, FpGuards.stuckExpect(free, false), false, false);
		check(nb >= 10, "the same dy outside powder still flags (free fall would be -0.194): " + nb);
		int hv = 0; for (int i = 0; i < 40; i++) hv = FpGuards.flyBuf(hv, -0.02, FpGuards.stuckExpect(free, true), false, false);
		check(hb >= 10, "hovering at -0.02 inside powder still flags: " + hb);
		check(hv >= 10, "hovering at -0.02 inside powder still flags: " + hv);
		int up = 0; for (int i = 0; i < 40; i++) up = FpGuards.flyBuf(up, 0.05, FpGuards.stuckExpect(free, true), false, false);
		check(up >= 10, "rising 0.05 per tick without the climb inside powder still flags: " + up);
		check(FpGuards.stuckExpect(-0.05, true) == -0.05, "a higher expectation is never lowered");
		// earlier ilhsmc pair: dy -0.161 expect -0.341 follows a multi-tick packet (staleFall)
		check(FpGuards.flyBuf(0, -0.161, -0.341, false, FpGuards.staleFall(-0.161, 0.1)) == 0, "ilhsmc dy -0.161 expect -0.341 after a multi-tick packet");
		check(FpGuards.flyBuf(0, -0.161, -0.341, false, FpGuards.staleFall(-0.161, 0.05)) == 1, "same pair with a normal gap still counts one");
		// rig 2026-10-16 (candidate8, 3-deep pit): Motion.y -0.1552 for 2 ticks, then -0.0784 steady = zero-then-gravity; the displacement per tick is that times 1.5 = -0.1176
		int rb = 0; double prevDy = -0.1552; for (int i = 0; i < 60; i++) { double ex = FpGuards.stuckExpect((prevDy - 0.08) * 0.98, true); rb = FpGuards.flyBuf(rb, FpGuards.POWDER_STUCK_DY, ex, false, false); prevDy = FpGuards.POWDER_STUCK_DY; }
		check(rb == 0, "steady stuck fall built fly buffer " + rb);
		// the rig CSV samples pos_y about every 10 ticks: its steps are whole multiples of 0.1176 (-1.176 = 10 ticks, -0.3528 = 3 ticks, 0.9408 = 8 ticks), so the per-tick displacement is -0.1176 while Motion.y reads -0.0784
		check(Math.abs(1.176 / 10 - -FpGuards.POWDER_STUCK_DY) < 1e-9 && Math.abs(0.3528 / 3 - -FpGuards.POWDER_STUCK_DY) < 1e-9 && Math.abs(0.9408 / 8 - -FpGuards.POWDER_STUCK_DY) < 1e-9, "rig pos_y steps are multiples of the stuck displacement");
		check(FpGuards.restingInPowder(true, true, 0.0), "dy 0 on the floor inside snow is resting");
		check(!FpGuards.restingInPowder(true, false, 0.0), "dy 0 with nothing under the feet is still a hover");
		check(!FpGuards.restingInPowder(false, true, 0.0), "outside powder the rule is off");
		check(!FpGuards.restingInPowder(true, true, 0.2), "a rise is never resting");
		int fb2 = 0; for (int i = 0; i < 40; i++) fb2 = FpGuards.flyBuf(fb2, 0.0, FpGuards.stuckExpect(-0.078, true), false, FpGuards.restingInPowder(true, false, 0.0)); 
		check(fb2 >= 10, "hover at dy 0 in powder with no support still flags: " + fb2);
		int gb = 0; for (int i = 0; i < 40; i++) gb = FpGuards.flyBuf(gb, 0.0, FpGuards.stuckExpect(-0.078, true), false, FpGuards.restingInPowder(true, true, 0.0));
		check(gb == 0, "standing on the pit floor in snow built buffer " + gb);
		System.out.println(fails == 0 ? "FpGuardsTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
