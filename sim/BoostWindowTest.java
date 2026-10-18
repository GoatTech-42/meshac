import dev.meshac.BoostWindow;

/** FINDING B1: riptide (isAutoSpinAttack) launched Java players in rain or out of water with no exemption; the window must open for it and last until landing. */
public class BoostWindowTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		long t0 = 1_000_000;
		check(BoostWindow.boosted(t0 + 100, t0, false, false), "open right after the boost");
		check(BoostWindow.boosted(t0 + 3999, t0, true, false), "open for the full window even on ground");
		check(!BoostWindow.boosted(t0 + 4000, t0, true, true), "closes at window end once on the ground");
		check(BoostWindow.boosted(t0 + 9000, t0, false, true), "stays open in the air after the window until landing");
		check(!BoostWindow.boosted(t0 + 9000, 0, false, false), "no boost seen, nothing exempt");
		// FINDING B2: a boost must not exempt an airborne player forever
		check(!BoostWindow.boosted(t0 + BoostWindow.MAX_AIR_MS, t0, false, true), "airborne exemption never ends");
		check(BoostWindow.boosted(t0 + BoostWindow.MAX_AIR_MS - 1, t0, false, true), "long legit launch (wind burst smash from a tower) cut off before the cap");
		check(!BoostWindow.boosted(t0 + 20000, 0, false, true), "stale flag without a boost stamp stays exempt");
		// FINDING B3: someone else's wind charge far away must not exempt the player
		check(BoostWindow.chargeCounts(true, 81), "own charge 9 blocks away");
		check(!BoostWindow.chargeCounts(false, 81), "an ally's or a breeze's charge 9 blocks away exempted the player");
		check(BoostWindow.chargeCounts(false, 4), "a charge bursting 2 blocks away does knock the player");
		check(!BoostWindow.chargeCounts(false, 3.6 * 3.6), "3.6 blocks away is outside the burst");
		boolean boosted = false;
		for (int tick = 0; tick < 120; tick++) { long now = t0 + tick * 50L; boolean ground = tick > 90; boosted = BoostWindow.boosted(now, t0, ground, boosted); if (tick <= 80) check(boosted, "exempt at tick " + tick); }
		if (fails > 0) { System.out.println(fails + " FAIL"); System.exit(1); }
		System.out.println("BoostWindowTest ok");
	}
}
