import dev.meshac.SustainBudget;

/** Audit of candidate4 sustained-speed budget: legit traces must never flag, soul speed style efficiency and cheats are pinned. No Minecraft needed. */
public class SustainBudgetTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }

	static double run(double hPerTick, double perTick, int ticks) {
		double bal = 0, max = 0;
		for (int i = 0; i < ticks; i++) { bal = SustainBudget.step(bal, hPerTick, perTick, 0.05); max = Math.max(max, bal); }
		return max;
	}

	public static void main(String[] a) {
		double flat = SustainBudget.perTick(1.0, false, 0, 1, false, false, false, 0);
		check(run(0.3564, flat, 20 * 300) < SustainBudget.LIMIT, "7.127 bps sprint jump flagged");
		double s2 = SustainBudget.perTick(1.4, false, 0, 1, false, false, false, 0);
		check(run(0.3564 * 1.4, s2, 20 * 300) < SustainBudget.LIMIT, "Speed II sprint jump flagged");
		double bal = 0; for (int i = 0; i < 10; i++) bal = SustainBudget.step(bal, 0.3564, flat, 0.05);
		check(bal == 0, "bunched legit packets built balance " + bal);
		bal = 0; for (int i = 0; i < 4; i++) bal = SustainBudget.step(bal, 0.9, flat, 0.05);
		check(bal < SustainBudget.LIMIT, "short knockback-sized burst flagged");
		int t = 0; bal = 0; while (!SustainBudget.flagged(bal) && t < 2000) { bal = SustainBudget.step(bal, 0.4075, flat, 0.05); t++; }
		check(t > 0 && t < 20 * 8, "8.15 bps cheat flag took " + t + " ticks");
		// FINDING S1
		check(Math.abs(SustainBudget.effectiveSlow(0.4, 0) - 0.4) < 1e-9, "no efficiency keeps soul sand factor 0.4");
		check(Math.abs(SustainBudget.effectiveSlow(0.4, 1) - 1.0) < 1e-9, "full efficiency removes the slowdown");
		double raw = SustainBudget.perTick(1.0, false, 10, 0.4, false, false, false, 0);
		double eff = SustainBudget.perTick(1.0, false, 10, SustainBudget.effectiveSlow(0.4, 1), false, false, false, 0);
		check(eff > raw * 1.5, "budget with full efficiency must exceed the floored raw budget (" + eff + " vs " + raw + ")");
		check(run(0.3564, raw, 20 * 60) >= SustainBudget.LIMIT, "documents the old false flag: normal pace on soul sand with the raw 0.65 floor");
		check(run(0.3564, eff, 20 * 60) < SustainBudget.LIMIT, "fixed: same pace passes with efficiency applied");
		check(SustainBudget.perTick(1.0, true, 0, 1, false, false, false, 0) > flat * 1.5, "ice window");
		check(SustainBudget.perTick(1.0, false, 0, 1, false, true, false, 0) == 0.12, "web");
		check(SustainBudget.perTick(1.0, false, 0, 1, false, false, true, 1.2) >= 1.25, "glide exit");
		// --- scenarios ported from the movement worker's SpeedBudgetTest (reconciliation: SustainBudget is the one budget class) ---
		// sustain pins
		check(Math.abs(flat - 0.37) < 1e-9, "java sprint baseline 0.37/tick");
		check(SustainBudget.perTick(1.0, false, 0, 1, true, false, false, 0) > 0.48, "bedrock gets 1.30");
		check(Math.abs(SustainBudget.perTick(1.2, false, 0, 1, false, false, false, 0) - 0.444) < 1e-3, "Speed I scales with the attribute");
		check(SustainBudget.perTick(1.0, false, 3, 0.4, false, false, false, 0) == flat, "slow block not counted before 5 ticks");
		check(Math.abs(SustainBudget.perTick(1.0, false, 9, 0.4, false, false, false, 0) - 0.37 * 0.65) < 1e-9, "slow block floor 0.65");
		// top-legit pace stays far below the limit
		check(run(7.35 / 20, flat, 20 * 120) < 1.0, "7.35 bps for 120 s gets near the limit");
		check(run(7.30 / 20, flat, 20 * 30) < SustainBudget.LIMIT, "7.30 bps is under budget by design");
		// legit 9.2 bps 0.4 s microbursts (rig worst window) separated by walking
		double bal2 = 0, peak2 = 0;
		for (int rep = 0; rep < 50; rep++) { peak2 = Math.max(peak2, run(9.2 / 20, flat, 8)); bal2 = SustainBudget.step(bal2, 7.09 / 20, flat, 0.05); }
		check(peak2 < 2.0, "9.2 bps bursts reached " + peak2);
		// stalled link releasing 24 held packets: caller floors each dt to one tick
		double bal3 = 0; for (int i = 0; i < 24; i++) bal3 = SustainBudget.step(bal3, 7.09 / 20, flat, 0.05);
		check(!SustainBudget.flagged(bal3), "stall burst flagged, bal " + bal3);
		double before = 3.0; bal3 = before; for (int i = 0; i < 24; i++) bal3 = SustainBudget.step(bal3, 0.30, flat, 0.05);
		check(bal3 <= before, "burst of sub-budget packets raised the balance " + before + " -> " + bal3);
		// clamp, idle drain
		check(SustainBudget.step(5.9, 5, flat, 0.05) == SustainBudget.CAP, "balance clamps at 6");
		check(SustainBudget.step(3.5, 0, flat, 1.0) < 0.01, "standing still drains the balance");
		// FINDING S2: the real caller used to be the only place dt was clamped; the class now owns it, so a test can see it
		double b4 = 0; for (int i = 0; i < 24; i++) b4 = SustainBudget.step(b4, 7.09 / 20, flat, 0.001);
		check(!SustainBudget.flagged(b4), "1 ms apart burst flagged without caller clamp, bal " + b4);
		check(SustainBudget.step(6, 0.3, flat, 30.0) == SustainBudget.step(6, 0.3, flat, 1.0), "gap credit capped at 1 s");
		check(SustainBudget.step(0, 0.3, flat, -5.0) == SustainBudget.step(0, 0.3, flat, 0.05), "negative dt treated as one tick");
		// FINDING S3: exempt stretch halves the bank once instead of zeroing it
		check(SustainBudget.onExempt(3.0, false) == 1.5, "first exempt packet halves");
		check(SustainBudget.onExempt(1.5, true) == 1.5, "later exempt packets keep the balance");
		// legit: sprinting near the limit, then a splash through water and a normal step: no flag, and the leftover is small
		double lb = 3.6; lb = SustainBudget.onExempt(lb, false); for (int i = 0; i < 20; i++) lb = SustainBudget.onExempt(lb, true);
		lb = SustainBudget.step(lb, 0.3564, flat, 0.05);
		check(!SustainBudget.flagged(lb), "legit sprint then water then normal step flagged, bal " + lb);
		// legit: knocked back by a hit (exempt 30 ticks), then back to sprinting at 7.1 bps for a minute
		double kb = 3.0; kb = SustainBudget.onExempt(kb, false); for (int i = 0; i < 30; i++) kb = SustainBudget.onExempt(kb, true);
		double kmax = kb; for (int i = 0; i < 20 * 60; i++) { kb = SustainBudget.step(kb, 0.3564, flat, 0.05); kmax = Math.max(kmax, kb); }
		check(kmax < SustainBudget.LIMIT, "after knockback the legit pace flagged, peak " + kmax);
		// cheat: 8.15 bps, a 1-packet water touch every 4 s. Zeroing never flags; halving flags within 20 s
		double cb = 0; boolean was = false; int ct = 0; boolean flaggedAt = false;
		for (int i = 0; i < 20 * 60 && !flaggedAt; i++) {
			boolean touch = i % 80 == 79;
			if (touch) { cb = SustainBudget.onExempt(cb, was); was = true; continue; }
			was = false; cb = SustainBudget.step(cb, 0.4075, flat, 0.05); if (SustainBudget.flagged(cb)) { flaggedAt = true; ct = i; }
		}
		check(flaggedAt && ct < 20 * 20, "water-touch cheat flagged after " + ct + " ticks (flagged=" + flaggedAt + ")");
		double zb = 0; boolean zflag = false;
		for (int i = 0; i < 20 * 60 && !zflag; i++) { if (i % 80 == 79) { zb = 0; continue; } zb = SustainBudget.step(zb, 0.4075, flat, 0.05); zflag = SustainBudget.flagged(zb); }
		check(!zflag, "documents the old gap: zeroing on exempt lets the touch cheat run a full minute");
		if (fails > 0) { System.out.println(fails + " FAIL"); System.exit(1); }
		System.out.println("SustainBudgetTest ok");
	}
}
