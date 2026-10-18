import dev.meshac.SustainBudget;

/**
 * Legit movement edge cases replayed through the pure speed rules: per-packet cap (SustainBudget.packetCap) and the sustained budget.
 * The player is a one-axis model of vanilla LivingEntity travel (ground accel = attr * 0.98 * 0.216 / blockFriction^3, air accel 0.026 sprinting, ground drag blockFriction * 0.91,
 * drag 0.91 in air, sprint-jump boost 0.2, jump 0.42, gravity 0.08/0.98, block speed factor applied after the move). The model values are from memory of vanilla
 * and are checked here only by calibration: flat sprint-jump must land near the rig's measured 7.09 bps. Failures of "exceeds cap" are candidates, not proof.
 */
public class LegitEdgeSimTest {
	static int fails; static double cheat = 1.0; /* multiplies each tick's real displacement, like a motion-scaling hack */ static boolean verbose = System.getProperty("edge.verbose") != null;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }

	/** terrain(tick, x) gives friction, speed factor, jump factor under the player for this tick. */
	interface Terrain { double[] at(int tick); } // {blockFriction, speedFactor, jumpFactor, web(0/1)}
	static final class Result { double avgBps, maxH, capOver, maxBal; int overCap, flagged, ticks; double worstOverRatio; String worst = ""; }

	static Result run(String name, Terrain terr, double ratio, double eff, int ticks, boolean jump, boolean bedrock) {
		double vx = 0, y = 0, vy = 0, x = 0; boolean ground = true, prevGround = true; long iceMs = -100000; int slowTicks = 0; double bal = 0, sumH = 0; boolean stuck = false, prevInWeb = false;
		Result r = new Result(); r.ticks = ticks;
		for (int t = 0; t < ticks; t++) {
			long nowMs = t * 50L; double[] tr = terr.at(t); double bf = tr[0], sf = tr[1], jf = tr[2]; boolean inWebEnd = tr[3] > 0; boolean webNow = prevInWeb; // the server reads the web at its own position, which is where the player ended the previous tick
			boolean gnd = ground;
			double f = gnd ? bf * 0.91 : 0.91;
			if (gnd && jump) { vy = 0.42 * jf; vx += 0.2; } // jump runs before travel; the tick still uses ground friction and accel
			double a = gnd ? 0.13 * ratio * 0.98 * 0.21600002 / (bf * bf * bf) : 0.026 * 0.98; // forward input is 0.98; ground accel cubes the block friction, not the drag factor
			vx += a;
			double disp = vx;
			if (stuck) { disp = vx * 0.25; vx = 0; stuck = false; }
			disp *= cheat; x += disp; y += vy; boolean landed = false;
			if (y <= 0 && vy <= 0) { y = 0; vy = 0; landed = true; }
			if (y < 0.5) vx *= sf;
			if (inWebEnd) stuck = true; prevInWeb = inWebEnd;
			vx *= f; vy = (vy - 0.08) * 0.98;
			prevGround = gnd; ground = landed;
			// packet checks as Movement does them, using the state the server holds when the packet arrives
			double h = Math.abs(disp);
			double fr = bf; double slow = sf;
			slowTicks = slow < 1.0 ? slowTicks + 1 : 0;
			if (fr > 0.61) iceMs = nowMs;
			double slowEff = SustainBudget.effectiveSlow(slow, eff);
			double cap = SustainBudget.packetCap(prevGround && ground, ratio, fr, slowTicks, slowEff, bedrock);
			if (webNow) cap = 0.10;
			double per = SustainBudget.perTick(ratio, nowMs - iceMs < 2000, slowTicks, slowEff, bedrock, webNow, false, 0);
			bal = SustainBudget.step(bal, h, per, 0.05); r.maxBal = Math.max(r.maxBal, bal);
			if (SustainBudget.flagged(bal)) { r.flagged++; bal = 0; }
			if (h > cap) { r.overCap++; double o = h / cap; if (o > r.worstOverRatio) { r.worstOverRatio = o; r.worst = "tick " + t + " h=" + String.format("%.3f", h) + " cap=" + String.format("%.3f", cap); } }
			r.maxH = Math.max(r.maxH, h); sumH += h;
		}
		r.avgBps = sumH / ticks * 20;
		if (verbose) System.out.printf("%-34s avg %.2f bps maxH %.3f maxBal %.2f overCap %d (worst x%.2f %s) sustainFlags %d%n", name, r.avgBps, r.maxH, r.maxBal, r.overCap, r.worstOverRatio, r.worst, r.flagged);
		return r;
	}
	static Terrain flat(double bf, double sf, double jf) { return t -> new double[] {bf, sf, jf, 0}; }

	public static void main(String[] a) {
		int T = 20 * 60;
		Result base = run("flat sprint-jump", flat(0.6, 1, 1), 1.0, 0, T, true, false);
		check(base.avgBps > 6.5 && base.avgBps < 7.7, "model calibration: flat sprint-jump should be near the rig's 7.09 bps, got " + base.avgBps);
		check(base.overCap == 0 && base.flagged == 0, "flat sprint-jump exceeded the cap " + base.overCap + " times / sustain flags " + base.flagged);
		Result sp2 = run("speed II sprint-jump", flat(0.6, 1, 1), 1.4, 0, T, true, false);
		check(sp2.overCap == 0 && sp2.flagged == 0, "speed II sprint-jump: overCap " + sp2.overCap + " flags " + sp2.flagged + " " + sp2.worst);
		for (double ice : new double[] {0.98, 0.989}) {
			Result ir = run("ice " + ice + " sprint-jump", flat(ice, 1, 1), 1.0, 0, T, true, false);
			check(ir.overCap == 0 && ir.flagged == 0, "ice " + ice + " sprint-jump: overCap " + ir.overCap + " flags " + ir.flagged + " " + ir.worst);
			Result ir2 = run("ice " + ice + " speed II", flat(ice, 1, 1), 1.4, 0, T, true, false);
			check(ir2.overCap == 0 && ir2.flagged == 0, "ice " + ice + " speed II: overCap " + ir2.overCap + " flags " + ir2.flagged + " " + ir2.worst);
		}
		// ice for 6 s then normal ground: the budget window after ice is 2 s, momentum decays in ticks
		Terrain iceThenGround = t -> new double[] {t < 120 ? 0.98 : 0.6, 1, 1, 0};
		Result it = run("ice 6 s then ground", iceThenGround, 1.0, 0, 20 * 30, true, false);
		check(it.flagged == 0 && it.overCap == 0, "stepping off ice: overCap " + it.overCap + " flags " + it.flagged + " " + it.worst);
		// ice patch hopped over: 1 s ice every 4 s
		Terrain patchy = t -> new double[] {(t % 80) < 20 ? 0.98 : 0.6, 1, 1, 0};
		Result pr = run("ice patches", patchy, 1.0, 0, T, true, false);
		check(pr.flagged == 0 && pr.overCap == 0, "ice patches: overCap " + pr.overCap + " flags " + pr.flagged + " " + pr.worst);
		// soul sand: walking sprint (no jump) and sprint-jumping, with speed II, full soul speed
		for (double eff : new double[] {0, 0.5, 1.0}) {
			Result s1 = run("soul sand sprint eff " + eff, flat(0.6, 0.4, 1), 1.0, eff, T, false, false);
			check(s1.overCap == 0 && s1.flagged == 0, "soul sand sprint eff " + eff + ": overCap " + s1.overCap + " flags " + s1.flagged + " " + s1.worst);
			Result s2 = run("soul sand sprint-jump speedII eff " + eff, flat(0.6, 0.4, 1), 1.4, eff, T, true, false);
			check(s2.overCap == 0 && s2.flagged == 0, "soul sand sprint-jump speed II eff " + eff + ": overCap " + s2.overCap + " flags " + s2.flagged + " " + s2.worst);
		}
		// soul sand 4 s then normal ground, speed II (leaving a slow block with momentum bonus)
		Terrain sandThenGround = t -> new double[] {0.6, t < 80 ? 0.4 : 1, 1, 0};
		Result st = run("soul sand then ground", sandThenGround, 1.4, 0, 20 * 20, true, false);
		check(st.overCap == 0 && st.flagged == 0, "leaving soul sand: overCap " + st.overCap + " flags " + st.flagged + " " + st.worst);
		// honey top: speed factor 0.4, jump factor 0.5
		Result h1 = run("honey sprint-jump", flat(0.6, 0.4, 0.5), 1.0, 0, T, true, false);
		check(h1.overCap == 0 && h1.flagged == 0, "honey sprint-jump: overCap " + h1.overCap + " flags " + h1.flagged + " " + h1.worst);
		Terrain honeyThenGround = t -> new double[] {0.6, t < 80 ? 0.4 : 1, t < 80 ? 0.5 : 1, 0};
		Result h2 = run("honey then ground", honeyThenGround, 1.0, 0, 20 * 20, true, false);
		check(h2.overCap == 0 && h2.flagged == 0, "leaving honey: overCap " + h2.overCap + " flags " + h2.flagged + " " + h2.worst);
		// cobweb: run in at sprint-jump speed, 12 ticks stuck, run out. server position is in the web from the tick after entry
		Terrain web = t -> new double[] {0.6, 1, 1, (t >= 41 && t < 53) ? 1 : 0}; // 1 = the player ends this tick inside the web
		Result w = run("cobweb enter 12 ticks exit", web, 1.0, 0, 20 * 10, true, false);
		check(w.overCap == 0 && w.flagged == 0, "cobweb enter/exit: overCap " + w.overCap + " flags " + w.flagged + " " + w.worst);
		Terrain webWalk = t -> new double[] {0.6, 1, 1, (t >= 41 && t < 53) ? 1 : 0};
		Result w2 = run("cobweb enter/exit walking", webWalk, 1.0, 0, 20 * 10, false, false);
		check(w2.overCap == 0 && w2.flagged == 0, "cobweb walk enter/exit: overCap " + w2.overCap + " flags " + w2.flagged + " " + w2.worst);
		// bedrock gets more room on both
		Result b1 = run("bedrock flat sprint-jump", flat(0.6, 1, 1), 1.0, 0, T, true, true);
		check(b1.overCap == 0 && b1.flagged == 0, "bedrock flat sprint-jump flagged");
		// cheats must still trip: 1.5x attribute on flat ground flags the budget or the cap
		cheat = 1.5; Result c15 = run("cheat 1.5x flat", flat(0.6, 1, 1), 1.0, 0, T, true, false);
		check(c15.flagged > 0 && c15.overCap > 0, "1.5x motion cheat goes unnoticed: avg " + c15.avgBps);
		cheat = 1.3; Result c13 = run("cheat 1.3x flat", flat(0.6, 1, 1), 1.0, 0, T, true, false);
		check(c13.flagged > 0, "1.3x motion cheat never trips the sustain budget: avg " + c13.avgBps);
		cheat = 1.3; Result c13b = run("bedrock cheat 1.3x flat", flat(0.6, 1, 1), 1.0, 0, T, true, true);
		System.out.println("info: bedrock 1.3x motion cheat (about " + String.format("%.1f", c13b.avgBps) + " bps): sustain flags " + c13b.flagged + ", over cap " + c13b.overCap);
		cheat = 1.1; Result c11 = run("cheat 1.1x flat", flat(0.6, 1, 1), 1.0, 0, T, true, false);
		System.out.println("info: 1.1x motion cheat (about " + String.format("%.1f", c11.avgBps) + " bps): sustain flags " + c11.flagged + ", over cap " + c11.overCap + "");
		cheat = 1.0;
		System.out.println(fails == 0 ? "LegitEdgeSimTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
