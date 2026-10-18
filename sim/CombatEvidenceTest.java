import dev.meshac.CombatEvidence;

public final class CombatEvidenceTest {
	private static void check(boolean ok, String why) { if (!ok) throw new AssertionError(why); }
	public static void main(String[] args) {
		double best = 2.9;
		for (double old : new double[]{3.4, 3.5, 3.6}) best = CombatEvidence.closest(best, old);
		check(best == 2.9, "current legal hit lost to stale history");
		check(CombatEvidence.closest(3.5, 2.9) == 2.9, "rewound legal hit lost");
		check(CombatEvidence.closest(3.5, 3.4) > 3.2, "all candidates out of reach still fail");
		check(CombatEvidence.eitherAim(false, true), "vertical/attacker rewind ray ignored");
		check(CombatEvidence.eitherAim(true, false), "current edge hit erased by history");
		check(!CombatEvidence.eitherAim(false, false), "true ray miss accepted");
		int snap = 0;
		for (boolean sample : new boolean[]{true, false, true, false, true}) snap = CombatEvidence.nextConsecutive(snap, sample);
		check(snap == 1, "interleaved legal samples must end a streak");
		for (boolean sample : new boolean[]{false, true, true, true}) snap = CombatEvidence.nextConsecutive(snap, sample);
		check(snap == 3, "three consecutive qualifiers still counted");
		System.out.println("CombatEvidenceTest: 8 assertions passed");
	}
}
