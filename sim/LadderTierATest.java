import dev.meshac.Ladder;

/**
 * Ladder is the punishment maths. rig/tests/LadderTest.java pins the rung/offence arithmetic but is wired into no harness, and sim/LadderSim.java only prints: the heat gate and the tier-A gate that pick a kick / tempban / permanent ban shipped untested. This test is wired into sim/run-tests.sh.
 *
 * The tier-A gate is the sharp end. Verdict.remove() reads Heat.confirmedTierA() to decide the rung skip and permanent-ban eligibility, Heat.add() reads Params.minClustersTierA() to decide whether tier A may remove, and Config.ladderParams() feeds requireClustersTierA straight from config. Those three do not agree. Pins the divergence, the heat floor that silently kills the tier-A shortcut on the lenient preset, the 0.25 cold reset that expires tier-A evidence, and the rung/tempban arithmetic.
 */
public final class LadderTierATest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	static final long T0 = 1_000_000L;

	/** Flag stream: weight w[i] at T0 + 200 ms * i, so every flag opens a new burst (clusterMs is 150) and every one is an independent cause. */
	static Ladder.Step[] steps(Ladder.Params p, int... w) {
		Ladder.Step[] out = new Ladder.Step[w.length]; Ladder.Heat h = new Ladder.Heat();
		for (int i = 0; i < w.length; i++) out[i] = h.add("c" + i, w[i], T0 + 200L * i, p);
		return out;
	}
	/** Same stream, but keeps the Heat so confirmedTierA() can be read, the way Verdict.remove() reads it. */
	static Ladder.Heat replay(Ladder.Params p, int... w) {
		Ladder.Heat h = new Ladder.Heat();
		for (int i = 0; i < w.length; i++) h.add("c" + i, w[i], T0 + 200L * i, p);
		return h;
	}
	/** Two weight-3 bursts g ms apart, for bracketing the tier-A heat floor. */
	static Ladder.Step[] tierApair(Ladder.Params p, long g) {
		Ladder.Heat h = new Ladder.Heat(); h.add("fly", 3, T0, p); return new Ladder.Step[]{h.add("fly", 3, T0 + g, p)};
	}
	static boolean same(Ladder.Step[] a, Ladder.Step[] b) {
		if (a.length != b.length) return false;
		for (int i = 0; i < a.length; i++) if (a[i] != b[i]) return false;
		return true;
	}
	static String show(Ladder.Step[] s) { StringBuilder b = new StringBuilder(); for (Ladder.Step x : s) b.append(x).append(' '); return b.toString(); }

	public static void main(String[] a) {
		Ladder.Params def = Ladder.Params.defaults();                                  // removeAt 6, the default preset
		Ladder.Params lenient = new Ladder.Params(8, 3, 8, 3, 2, 3, 150);             // Config preset "lenient": removeAt 8
		// minClustersTierA past any real count switches the tier-A branch off without touching the generic gate, so a run can be compared against itself with only that branch removed.
		Ladder.Params noTierA = new Ladder.Params(8, 3, 6, 3, 99, 3, 150);
		Ladder.Params lenientNoTierA = new Ladder.Params(8, 3, 8, 3, 99, 3, 150);

		// one burst counts once, and the heaviest flag in it tops the heat up by only the difference
		Ladder.Heat one = new Ladder.Heat();
		check(one.add("fly", 3, T0, def) == Ladder.Step.HOLD, "one tier-A flag reaches holdAt (heat 3.0) and nothing more");
		check(one.add("fly", 3, T0 + 100, def) == Ladder.Step.SETBACK, "the same tier-A flag 100 ms later is the same burst: no heat is added, the decay drops 3.0 -> 2.97, and even the hold is lost");
		check(one.add("fly", 5, T0 + 120, def) == Ladder.Step.HOLD, "a weight 5 flag 20 ms later tops the burst up to its heaviest, so heat is 2.97 + (5 - 3) = 4.97: the burst cost 3, not 5");

		// the tier-A shortcut also needs heat >= removeAt - 1, and that floor is unreachable on the lenient preset
		Ladder.Step[] defOn = steps(def, 3, 3);
		Ladder.Step[] defOff = steps(noTierA, 3, 3);
		check(defOn[1] == Ladder.Step.REMOVE && !same(defOn, defOff), "default preset: two tier-A bursts remove on the second, which the tier-A branch alone is responsible for (" + show(defOn) + " vs " + show(defOff) + ")");
		Ladder.Step[] lenOn = steps(lenient, 3, 3, 3);
		Ladder.Step[] lenOff = steps(lenientNoTierA, 3, 3, 3);
		check(steps(lenient, 3, 3)[1] != Ladder.Step.REMOVE, "KNOWN: lenient preset, two independent tier-A causes leave the player on hold, not removed: the shortcut floor is removeAt - 1 = 7 and two weight-3 bursts cap at 5.95");
		check(same(lenOn, lenOff), "KNOWN: that makes the whole tier-A branch dead code on the shipped lenient preset - the same run with the branch disabled is identical, so tier A never beats the generic gate (" + show(lenOn) + " vs " + show(lenOff) + ")");
		check(tierApair(def, 1500)[0] == Ladder.Step.REMOVE, "the floor is exactly removeAt - 1: two tier-A bursts 1.5 s apart reach heat 5.63, which clears a floor of 5 but not 6");
		check(tierApair(def, 8000)[0] == Ladder.Step.HOLD, "and it is not removeAt - 2: the same bursts 8 s apart reach only 4.50, which clears a floor of 4 but not 5");

		// confirmedTierA() hardcodes 2 and never looks at minClustersTierA: high side
		Ladder.Params wantFour = new Ladder.Params(8, 3, 6, 3, 4, 3, 150);   // operator asks for 4 independent tier-A causes
		Ladder.Heat four = replay(wantFour, 3, 3);
		check(steps(wantFour, 3, 3)[1] == Ladder.Step.HOLD, "requireClustersTierA 4: the second tier-A burst no longer removes");
		check(!four.confirmedTierA(wantFour), "requireClustersTierA 4: confirmedTierA now honors the configured bar - false after only 2 tier-A bursts, so the rung skip and the permanent ban are withheld");
		check(steps(wantFour, 3, 3, 3)[2] == Ladder.Step.REMOVE, "requireClustersTierA 4: the third burst removes on the generic gate (heat 8.85 >= 6 and 3 clusters >= 3) with one tier-A burst still to come");
		check(!replay(wantFour, 3, 3, 3).confirmedTierA(wantFour), "requireClustersTierA 4: a generic-gate removal at 3 bursts is not tier-A confirmed under a bar of 4");
		check(replay(wantFour, 3, 3, 3, 3).confirmedTierA(wantFour), "requireClustersTierA 4: confirmed once the 4th independent tier-A cause lands");

		// confirmedTierA() against the other side: requireClustersTierA 1 removes on tier A that confirmedTierA() will not vouch for
		Ladder.Params wantOne = new Ladder.Params(8, 3, 6, 5, 1, 3, 150);   // generic gate needs 5 clusters, so only tier A can remove
		Ladder.Heat oneA = replay(wantOne, 2, 2, 3);
		check(steps(wantOne, 2, 2, 3)[2] == Ladder.Step.REMOVE, "requireClustersTierA 1: one tier-A burst on top of two tier-B bursts removes (heat 6.90 >= removeAt - 1 = 5)");
		check(steps(new Ladder.Params(8, 3, 6, 5, 99, 3, 150), 2, 2, 3)[2] != Ladder.Step.REMOVE, "setup: the same stream with the tier-A branch disabled does not remove, so the removal above did come from the tier-A branch");
		check(oneA.confirmedTierA(wantOne), "requireClustersTierA 1: the removal tier A caused is tier-A confirmed - the rung skip and permanent eligibility follow the same configured bar");

		// minClusters is a floor, not the trigger: the generic rule needs heat >= removeAt as well
		Ladder.Step[] tierB = steps(noTierA, 2, 2, 2, 2);
		check(tierB[2] == Ladder.Step.HOLD && tierB[3] == Ladder.Step.REMOVE, "tier-B weight 2, 200 ms apart: the 3rd burst meets minClusters 3 but heat is only 5.90, the 4th crosses removeAt 6 (" + show(tierB) + ")");

		// the 0.25 cold reset, and that it expires the tier-A evidence Verdict relies on
		Ladder.Heat cold = new Ladder.Heat(); cold.add("fly", 3, T0, def);
		check(cold.now(T0 + 28000, def) >= 0.25, "28 s after a single weight-3 flag the decayed heat is 0.265, still above the 0.25 cold threshold");
		check(cold.now(T0 + 30000, def) < 0.25, "30 s after it the heat is 0.223, so the next flag wipes clusters, tier-A clusters and the burst");
		Ladder.Heat probe = replay(new Ladder.Params(8, 3, 99, 3, 2, 3, 150), 3, 3);
		check(probe.confirmedTierA(new Ladder.Params(8, 3, 99, 3, 2, 3, 150)), "setup: two tier-A bursts confirm tier A");
		probe.add("speed", 1, T0 + 40000, new Ladder.Params(8, 3, 99, 3, 2, 3, 150));
		check(!probe.confirmedTierA(new Ladder.Params(8, 3, 99, 3, 2, 3, 150)), "INTENTIONAL (decision relayed via parent 10-18, option a: keep the heat coupling while enforcement is log-only; revisit a longer tier-A evidence lifetime before any enforcement-ON proposal): 'tier A confirmed', the evidence Verdict uses for the rung skip and the permanent ban, is wiped by the 0.25 cold reset, so it only survives while the heat stays above 0.25");

		// offence arithmetic
		check(Ladder.effectiveOffences(new double[]{0}, 14) == 1.0, "a fresh offence is worth exactly 1");
		check(Ladder.rung(0.99995, 1, false) == 2, "a repeat offence a minute later (effective 0.99995) is still rung 2: the 0.05 tolerance that stops floor() dropping it back to a kick");
		check(Ladder.rung(0.94, 1, false) == 1, "an offence forgiven to 0.94 is rung 1, the kick");
		check(Ladder.rung(2.0, 1, true) == Ladder.rung(2.0, 1, false) + 1, "tier-A confirmed skips exactly one rung, and skip is the only thing that does");
		check(Ladder.rung(2.0, 0, false) == Ladder.rung(2.0, 25, false), "KNOWN: rung() takes kicks and ignores it - the rung is floor(effective) + 1, and kicks only reaches the ladder through tempMinutes");
		double future = Ladder.effectiveOffences(new double[]{-30}, 14);
		check(future == 1.0 && Ladder.rung(future, 1, false) == 2, "a case dated in the future (clock skew, or a hand-edited cases.json) is clamped to fresh: worth exactly 1 offence, rung unchanged");
		check(Ladder.effectiveOffences(new double[]{-30, 5}, 14) == 1.0 + Ladder.effectiveOffences(new double[]{5}, 14), "the clamp is per-case: a real 5-day-old offence beside the future-dated one is unaffected");
		check(Ladder.tempMinutes(3, 1, 10, 6, 10080) == 60, "rung 3 with 1 prior kick is the second tempban: base * growth^1");
		check(Ladder.tempMinutes(6, 1, 10, 6, 10080) == 10080, "the geometric ladder is capped at tempMax");
		check(Ladder.tempMinutes(1, 1, 10, 6, 10080) == 10, "KNOWN: the kick rung still returns a tempban length, because the index is clamped at 0 - Verdict only reaches it when rung > kicks");

		// invalid inputs fail closed (0079v2): no increased punishment, caller logs the diagnostic
		check(Ladder.Params.problems(Ladder.Params.defaults()).isEmpty(), "the reviewed defaults pass validation");
		Ladder.Params bad = new Ladder.Params(Double.NaN, 0, -1, 0, 0, 0, 0);
		check(Ladder.Params.problems(bad).equals("halfLifeSeconds,holdAt,removeAt,requireClusters,requireClustersTierA,tierAWeight,clusterMs"), "every invalid knob is named for the diagnostic: " + Ladder.Params.problems(bad));
		check(bad.sanitized().equals(Ladder.Params.defaults()), "every invalid knob falls back to the reviewed defaults, nothing stricter or looser");
		Ladder.Step[] badRun = steps(bad.sanitized(), 3, 3);
		check(badRun[1] == Ladder.Step.REMOVE, "sanitized-invalid params behave exactly like the defaults (2 tier-A bursts remove)");
		check(Ladder.effectiveOffences(new double[]{1}, 0) == 0 && Ladder.effectiveOffences(new double[]{1}, Double.NaN) == 0 && Ladder.effectiveOffences(new double[]{1}, -14) == 0, "invalid offence half-life fails closed: no offences counted, no punishment inflation");
		check(Ladder.effectiveOffences(new double[]{Double.NaN, 5}, 14) == Ladder.effectiveOffences(new double[]{5}, 14), "a NaN age (corrupt case data) is skipped, not counted");
		check(Ladder.effectiveOffences(new double[]{Double.POSITIVE_INFINITY}, 14) == 0, "an infinitely old offence is fully decayed");
		check(Ladder.rung(Double.NaN, 1, false) == 1 && Ladder.rung(-3, 1, false) == 1 && Ladder.rung(Double.NaN, 1, true) == 1, "non-finite or negative effective offences fail closed to the kick rung - with NO tier-A skip benefit on corrupt data");
		check(Ladder.rung(1e300, 1, false) == Integer.MAX_VALUE - 1 && Ladder.rung(1e300, 1, true) == Integer.MAX_VALUE, "a huge offence history saturates instead of wrapping the int cast negative");
		Ladder.Params rawInvalid = new Ladder.Params(Double.NaN, 0, -1, 0, 0, 0, 0);
		Ladder.Heat rawBad = new Ladder.Heat();
		check(rawBad.add("glide", 3, T0, rawInvalid) == Ladder.Step.LOG && rawBad.add("glide", 3, T0 + 200, rawInvalid) == Ladder.Step.LOG, "explicit invalid-policy contract: raw invalid params record evidence but the step is LOG - no action of any kind, even for a caller that skips Config");
		check(rawBad.add("glide", 3, T0 + 400, def) == Ladder.Step.HOLD, "invalid->valid same-values regression: evidence collected under invalid params (sanitized to the defaults) belongs to a DIFFERENT epoch - the first valid default-params burst starts fresh (one burst, HOLD) instead of instantly removing on the invalid period's counts");
		check(!rawBad.confirmedTierA(rawInvalid) && rawBad.now(T0 + 300, rawInvalid) == 0, "read entry points require bound + valid params: invalid params read as nothing");
		check(new Ladder.Heat().now(T0, def) == 0 && !new Ladder.Heat().confirmedTierA(def), "a never-fed (unbound) heat reads as zero and confirms nothing, even with valid params");
		check(Ladder.PolicyDecision.of(def, 14, null, 14).equals(new Ladder.PolicyDecision(def, 14, false)), "policy decision: valid raw config publishes as-is, not invalid");
		Ladder.PolicyDecision dec = Ladder.PolicyDecision.of(rawInvalid, Double.NaN, null, 14);
		check(dec.invalid() && dec.params().equals(Ladder.Params.defaults()) && dec.offenceHalfLifeDays() == 14, "policy decision: fully invalid config publishes the reviewed defaults (first boot) and is marked invalid - the caller pauses enforcement");
		Ladder.PolicyDecision dec2 = Ladder.PolicyDecision.of(rawInvalid, 14, lenient, 14);
		check(dec2.invalid() && dec2.params().equals(lenient), "policy decision: invalid reload keeps the LAST VALID params, not the broken ones");
		check(!Ladder.PolicyDecision.of(def, -1, null, 14).invalid() == false, "policy decision: an invalid offence half-life alone marks the policy invalid");
		check(Ladder.PolicyDecision.of(rawInvalid, 14, rawInvalid, 14).params().equals(Ladder.Params.defaults()), "policy decision: a corrupt lastValid is no fallback - ignored, defaults used");
		check(Ladder.PolicyDecision.of(def, Double.NaN, null, -1).offenceHalfLifeDays() == 14, "policy decision: a corrupt default half-life argument is repaired to 14");
		Ladder.Heat gate = new Ladder.Heat();
		gate.add("glide", 3, T0, wantFour); gate.add("glide", 3, T0 + 200, wantFour);
		check(!gate.confirmedTierA(wantOne), "epoch-gated read: evidence collected under a bar of 4 does not confirm when read with a bar of 1 (no new add needed to lose confirmation)");
		check(gate.now(T0 + 500, wantOne) == 0, "epoch-gated read: stale-epoch heat reads as zero under the new params, never old heat under a new decay curve");

		// policy epoch: a config change wipes old evidence instead of reclassifying it (fail closed)
		Ladder.Heat epoch = new Ladder.Heat();
		epoch.add("glide", 3, T0, def); epoch.add("glide", 3, T0 + 200, def);
		check(epoch.confirmedTierA(def), "setup: two tier-A bursts confirm under the default params");
		epoch.add("speed", 1, T0 + 400, lenient); // params change (e.g. /mesh reload)
		check(!epoch.confirmedTierA(lenient), "a params change resets the heat: old clusters are never reclassified under the new thresholds (the tier-A confirmation is gone)");
		check(epoch.add("glide", 3, T0 + 600, lenient) != Ladder.Step.REMOVE, "after the reset, one tier-A burst under the new params removes nobody");

		System.out.println(fails == 0 ? "LadderTierATest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
