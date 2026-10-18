import dev.meshac.*;

/** Pure legit-mechanic replays: server-ordered moves must open the grace path, launches must not trip the per-packet or sustained speed budgets. */
public final class LegitReplayTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		// pearl, chorus fruit, Enderman: server position is far from both the last start and the last claim
		check(TeleportDetect.serverMoved(true, false, false, 8.0), "pearl landing (8 blocks) is a server move");
		check(TeleportDetect.serverMoved(true, false, false, 16.0), "chorus fruit is a server move");
		check(TeleportDetect.serverMoved(true, false, false, 30.0), "enderman teleport of the victim/player is a server move");
		check(TeleportDetect.serverMoved(false, false, false, 8.0), "later packet after a pearl is still caught");
		check(!TeleportDetect.serverMoved(true, true, false, 0.0), "normal packet is not a teleport");
		check(!TeleportDetect.serverMoved(true, false, true, 0.0), "packet matching claim is not a teleport");
		check(!TeleportDetect.serverMoved(true, false, false, 0.5), "small collision correction is not a teleport");
		check(TeleportDetect.serverMoved(false, false, false, 0.76) , "just over the bar counts");
		// mace smash / wind charge / riptide: boosted window covers the 4 s after the launch, and until landing
		long t = 100000;
		check(BoostWindow.boosted(t + 1000, t, false, false), "wind charge launch 1s ago is boosted");
		check(BoostWindow.boosted(t + 3900, t, false, false), "still boosted at 3.9s");
		check(BoostWindow.boosted(t + 9000, t, false, true), "still airborne after launch stays boosted");
		check(!BoostWindow.boosted(t + 5000, t, true, false), "grounded after the window is not boosted");
		check(BoostWindow.chargeCounts(true, 400), "own charge counts at any range");
		check(!BoostWindow.chargeCounts(false, 100), "someone else's charge 10 blocks away does not");
		check(BoostWindow.chargeCounts(false, 9), "someone else's charge 3 blocks away does");
		// elytra exit: cap is lifted to the last glide speed for one second, then back
		double base = 0.69;
		check(GlideWindow.cap(base, t + 500, t, 1.8) >= 1.8, "elytra exit keeps glide speed for the first second");
		check(GlideWindow.cap(base, t + 1500, t, 1.8) == base, "back to normal after one second");
		// sustained budget: a launch burst of 3 blocks per tick for 6 ticks is a long jump in vanilla
		double bal = 0; double per = SustainBudget.perTick(1.0, false, 0, 1.0, false, false, true, 1.8);
		for (int i = 0; i < 20; i++) bal = SustainBudget.step(bal, 1.8, per, 0.05);
		check(!SustainBudget.flagged(bal), "one second of elytra exit at 1.8 per tick does not flag");
		// context line is stable and carries the fields the next log review needs
		String l = MoveContext.line("p", 1, 0.5, 2, 0.1, true, false, false, false, false, false, false, 0, 3, "mace", 10000, 9000, 0, 0, 8000);
		check(l.contains("ticks=2.0") && l.contains("dmg=mace") && l.contains("kb=never") && l.contains("wind=1.0s"), "ctx line fields: " + l);
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("LegitReplayTest ok");
	}
}
