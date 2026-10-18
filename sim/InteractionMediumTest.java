import dev.meshac.FpGuards;
import dev.meshac.HoneyRule;
import dev.meshac.MediumExit;
import dev.meshac.MediumFootprint;

/** Source-only audit sims: water exit and re-entry, honey, cobweb, crouch against the fly, air-jump and speed rules. Vanilla constants from javap of server-26.2.jar. The per-packet replay below uses the same helpers Movement calls (MediumExit, FpGuards.flyBuf/airKick), in the same order; Movement itself needs a ServerPlayer and is native-unwitnessed. */
public final class InteractionMediumTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	static boolean has(int[] c, int x, int y, int z) { return x >= c[0] && x <= c[1] && y >= c[2] && y <= c[3] && z >= c[4] && z <= c[5]; }

	/** One player's fly and air-jump state, fed packets the way Movement does: medium tick (exempt, server says in water) or air packet (non-exempt). */
	static final class Rep {
		final MediumExit exit = new MediumExit(); int buf, ajN; long ajAt; int sinceJump = 99; double prevDy; long now = 1_000_000; boolean fly, airJump;
		void medium(double dy) { exit.onMedium(now); prevDy = dy; now += 50; }
		void air(double dy) {
			boolean firstExit = exit.firstAfter(now, dy);
			double expect = (prevDy - 0.08) * 0.98;
			if (!firstExit) buf = FpGuards.flyBuf(buf, dy, expect, false, false); // a waived exit packet neither adds nor decays the buffer
			if (buf >= 10) fly = true;
			sinceJump++;
			if (FpGuards.airKick(dy, expect) && !firstExit) {
				ajN = now - ajAt > 2500 ? 1 : ajN + 1; ajAt = now;
				if ((sinceJump <= 8 && ajN >= 2) || ajN >= 4) airJump = true;
				sinceJump = 0;
			}
			prevDy = dy; now += 50;
		}
	}
	static double chain(double d) { return (d - 0.08) * 0.98; }

	public static void main(String[] a) throws Exception {
		// 1 shore bob: three water-exit jumps in 2 s, each from a 0.02 swim packet, vanilla chain after the jump-out
		Rep bob = new Rep();
		for (int i = 0; i < 3; i++) { bob.medium(0.02); double d = 0.30; for (int k = 0; k < 6; k++) { bob.air(d); d = chain(d); } bob.medium(-0.03); }
		check(!bob.fly && !bob.airJump, "legit shore bob: no fly, no air jump");
		// the same without the exit gate (what 9r does) shows the FP
		check(FpGuards.airKick(0.30, chain(0.02)) && FpGuards.flyBuf(0, 0.30, chain(0.02), false, false) == 3, "without the gate one exit packet is a triple overshoot and an air-jump kick");
		// 2 cheat hovers at +0.30 per packet after one exit: only the first packet is waived, the rest flag as fly
		Rep hov = new Rep(); hov.medium(0.02); int n = 0; while (!hov.fly && n < 40) { hov.air(0.30); n++; }
		check(hov.fly && n <= 14, "cheat hover after an exit flags (packets: " + n + ")");
		// 3 cheat dips in and out of water every other packet, 0.32 up each exit
		Rep dip = new Rep(); int cycles = 0; while (!dip.fly && cycles < 60) { dip.medium(0.02); dip.air(0.32); dip.air(0.32); cycles++; }
		check(dip.fly && cycles <= 14, "re-entry dips flag as fly (cycles: " + cycles + ")");
		// 4 exit above the envelope is not waived at all
		Rep big = new Rep(); big.medium(0.02); big.air(0.60); check(big.buf == 3 && FpGuards.airKick(0.60, chain(0.02)), "an exit step of 0.60 is not waived");
		// 5 the window: first packet 350 ms after the last medium tick is not waived, and a second packet never is
		MediumExit w = new MediumExit(); w.onMedium(1000);
		check(!w.firstAfter(1350, 0.30) && !w.firstAfter(1360, 0.30), "after 300 ms nothing is waived");
		w.onMedium(2000); check(w.firstAfter(2050, 0.30) && !w.firstAfter(2100, 0.30), "one waiver per medium tick");
		MediumExit ground = new MediumExit(); ground.onMedium(0); ground.firstAfter(50, 0.0); ground.onMedium(100);
		check(ground.firstAfter(150, 0.30), "a fresh medium tick re-arms");
		// 6 a levitation or ladder step out (0.2) is inside the envelope
		Rep lad = new Rep(); lad.medium(0.20); lad.air(0.20); lad.air(chain(0.20)); check(!lad.fly && !lad.airJump && lad.buf == 0, "ladder top exit chain clean");
		// 7 footprint from the real box: crouched body does not reach a web above it, standing does
		int[] stand = MediumFootprint.cells(0.7, 70.5, 0.7, 1.3, 70.5 + 1.8, 1.3);
		int[] crouch = MediumFootprint.cells(0.7, 70.5, 0.7, 1.3, 70.5 + 1.5, 1.3);
		int[] swim = MediumFootprint.cells(0.7, 70.0, 0.7, 1.3, 70.0 + 0.6, 1.3);
		check(has(stand, 0, 72, 0) && !has(crouch, 0, 72, 0) && !has(swim, 0, 71, 0) && has(swim, 0, 70, 0), "web above a crouched or swimming body is not touched");
		int[] corner = MediumFootprint.cells(0.7, 70.0, 0.7, 1.3, 71.8, 1.3);
		check(has(corner, 0, 70, 0) && has(corner, 1, 71, 1) && !has(corner, 2, 70, 1), "corner overlap includes diagonals");
		int[] edge = MediumFootprint.cells(1.0, 70.0, 1.0, 1.6, 71.8, 1.6);
		check(!has(edge, 0, 70, 0), "a body that only touches the cell boundary does not overlap it");
		// 8 honey: HoneyBlock.isSlidingDown, width 0.6, reach 0.7375
		check(HoneyRule.sliding(false, 70.5, 0, 70, 0, 1.25, 0.5, 0.6), "pressed against the east face: slides");
		check(HoneyRule.sliding(false, 70.5, 0, 70, 0, 1.25, -0.15, 0.6), "diagonal contact at the face: slides");
		check(!HoneyRule.sliding(false, 70.5, 0, 70, 0, 1.0, 1.0, 0.6), "the exact corner (inside both reaches) is not a slide");
		check(!HoneyRule.sliding(true, 70.5, 0, 70, 0, 1.25, 0.5, 0.6), "on the ground: no slide");
		check(!HoneyRule.sliding(false, 71.0, 0, 70, 0, 1.25, 0.5, 0.6), "above the 0.9375 line: no slide");
		check(!HoneyRule.sliding(false, 70.5, 0, 70, 0, 0.5, 0.5, 0.6), "centred over the block: no slide");
		check(HoneyRule.sliding(false, 70.5, 0, 70, 0, 1.25, 0.5, 0.6) && !HoneyRule.sliding(false, 70.5, 0, 70, 0, 0.9, 0.5, 1.8), "wider body reaches further");
		check(FpGuards.honeyDyOk(-0.05) && !FpGuards.honeyDyOk(0.0) && !FpGuards.honeyDyOk(0.2) && FpGuards.webDyOk(-0.004) && FpGuards.webDyOk(0.02) && !FpGuards.webDyOk(0.3), "dy gates: a hovering or rising packet is not a slide or a web");
		// 9 web cap: validated ratio, bounded
		check(FpGuards.webCap(Double.NaN) == 0.10 && FpGuards.webCap(Double.POSITIVE_INFINITY) == 0.10 && FpGuards.webCap(Double.NEGATIVE_INFINITY) == 0.10 && FpGuards.webCap(0) == 0.10 && FpGuards.webCap(-3) == 0.10, "bad ratios give the flat cap");
		check(FpGuards.webCap(1.0) == 0.10 && FpGuards.webCap(0.7) == 0.10, "plain or slow speed keeps 0.10");
		double e2 = 0.36 * 1.4 * 0.25;
		check(e2 > 0.10 && FpGuards.webCap(1.4) >= e2, "speed II web entry " + e2 + " fits " + FpGuards.webCap(1.4));
		check(FpGuards.webCap(1e9) == 0.25 && FpGuards.webCap(100) == 0.25, "a huge attribute is bounded at 0.25");
		check(3 * 0.30 * 0.25 > FpGuards.webCap(1.0) * 1.25 && 3 * 0.30 * 0.25 > FpGuards.webCap(1.4) * 1.25 && 3.0 * 0.36 * 0.25 > FpGuards.webCap(1.4), "a 3x web runner still overshoots plain and speed-II caps");
		// production predicate: FpGuards.stuckNow (what Movement calls) with rise/hover next to honey and web cheats
		check(FpGuards.stuckNow(-0.05, false, true) && FpGuards.stuckNow(-0.30, false, true), "honey slide or fall into it: exempt");
		check(!FpGuards.stuckNow(0.0, false, true) && !FpGuards.stuckNow(0.20, false, true) && !FpGuards.stuckNow(0.42, false, true), "hover or rise beside honey: not exempt");
		check(!FpGuards.stuckNow(-0.05, false, false), "no honey slide geometry: nothing exempt");
		check(FpGuards.stuckNow(-0.004, true, false) && FpGuards.stuckNow(0.02, true, false) && !FpGuards.stuckNow(0.30, true, false) && !FpGuards.stuckNow(0.0 + 0.06, true, false), "web: slow settle exempt, rising cheat at feet or head not");
		check(!FpGuards.stuckNow(0.30, true, true), "web and honey together do not exempt a rise");
		check(FpGuards.hardMedium(false, false, false, false, false, true, false) && FpGuards.hardMedium(false, false, false, false, false, false, true) && !FpGuards.hardMedium(false, false, false, false, false, false, false), "hard mediums: bubble column, scaffolding");
		// replay: a cheat that hugs honey and rises or hovers still accumulates fly; a real slide does not
		int cb = 0; for (int k = 0; k < 14; k++) { if (!FpGuards.stuckNow(0.0, false, true)) cb = FpGuards.flyBuf(cb, 0.0, chain(-0.05), false, false); }
		check(cb >= 10, "hovering beside honey flags fly (buf " + cb + ")");
		int sb = 0; for (int k = 0; k < 40; k++) { if (!FpGuards.stuckNow(-0.05, false, true)) sb = FpGuards.flyBuf(sb, -0.05, chain(-0.05), false, false); }
		check(sb == 0, "a real honey slide never touches the fly buffer");
		// 10 Movement source: the code part of each line (before //) must carry the statements, so a comment can never swallow them
		java.util.List<String> code = new java.util.ArrayList<>();
		for (String l : java.nio.file.Files.readAllLines(java.nio.file.Paths.get("src/main/java/dev/meshac/Movement.java"))) { int i = l.indexOf("//"); code.add(i < 0 ? l : l.substring(0, i)); }
		String all = String.join("\n", code);
		for (String must : new String[] { "SustainBudget.onExempt(s.spBal, s.wasExempt)", "SustainBudget.onExempt(s.spShadow, s.wasExempt)", "s.wasExempt = exempt;", "s.wasExempt = false;", "s.exit.onMedium(nowMs)", "s.exit.firstAfter(nowMs, dy)", "firstExit ? s.bufFly : FpGuards.flyBuf(", "!firstExit", "FpGuards.webCap(pl.getSpeed() / 0.13)", "FpGuards.stuckNow(dy, StuckBlocks.cobweb(fl, srvBox) || StuckBlocks.cobweb(fl, endBox)", "StuckBlocks.honeySlide(fl, endBox, x, y, z", "boolean web = StuckBlocks.cobweb(pl.level(), pl.getBoundingBox())", "FpGuards.hardMedium(", "!stuckNow &&" })
			check(all.contains(must), "Movement code (not comment) has: " + must);
		check(!all.contains("HONEY_BLOCK") && !all.contains("Blocks.COBWEB"), "Movement has no legacy cobweb or honey cell checks left (only the gated body-box calls)");
		int open = 0, close = 0; for (char ch : all.toCharArray()) { if (ch == '{') open++; else if (ch == '}') close++; }
		check(open == close, "Movement braces balance outside comments (" + open + "/" + close + ")");
		for (String l : code) if (l.contains("s.exit.onMedium(nowMs)")) check(l.contains("if (FpGuards.bendsGravity"), "the medium tick sits on its own guarded line");
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("InteractionMediumTest ok");
	}
}
