import dev.meshac.BoostWindow;
import dev.meshac.GlideWindow;

public final class GlideWindowTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		long t0 = 1_000_000L;
		// elytra firework landing: last glide packet at t0, first hop one tick later (the live 0.762 flag), a second hop later
		check(!GlideWindow.jumpCheckLive(t0 + 50, t0), "first hop right after landing must be skipped");
		check(!GlideWindow.jumpCheckLive(t0 + 2500, t0), "still skipped at exactly 2.5 s (original: > 2500)");
		check(GlideWindow.jumpCheckLive(t0 + 2501, t0), "live again after 2.5 s");
		check(GlideWindow.jumpCheckLive(t0, 0), "never glided: the check runs");
		check(GlideWindow.jumpCheckLive(5, 0) == (5 - 0 > 2500), "tiny clock still follows the original arithmetic");
		// a cheater who lands and hops every 2 s from a fresh glide stamp stays covered only while they really glide: stamps need isFallFlying
		long glide = t0; for (int i = 0; i < 10; i++) glide += 1000; // glided for 10 s, stamping each second
		check(!GlideWindow.jumpCheckLive(glide + 1000, glide), "inside the window after a long flight");
		// cap window
		check(GlideWindow.capWindow(t0 + 999, t0) && !GlideWindow.capWindow(t0 + 1000, t0) && !GlideWindow.capWindow(t0 + 500, 0), "cap window bounds");
		check(Math.abs(GlideWindow.cap(0.4, t0 + 100, t0, 1.2) - 1.23) < 1e-9, "cap follows the glide speed inside the window");
		check(GlideWindow.cap(0.4, t0 + 100, t0, 0.2) == 0.4, "slow glide never lowers the cap");
		check(GlideWindow.cap(0.4, t0 + 2000, t0, 1.2) == 0.4, "window over: no raise");
		// BoostWindow: riptide stamps windAt every packet while spinning; mace smash and wind charge stamp once
		boolean boosted = false; long last = 0; long stamp = 0; int openTicks = 0;
		for (int tick = 0; tick < 400; tick++) { // riptide spin 40 ticks (2 s), airborne for 5 s, lands at tick 140
			long now = t0 + tick * 50L; if (tick < 40) stamp = now; boolean ground = tick >= 140 || tick < 2;
			boosted = BoostWindow.boosted(now, stamp, ground, boosted); if (boosted) openTicks++; if (tick == 30) check(boosted, "riptide: open while spinning");
			if (tick == 40 + 79) check(boosted, "riptide: open for the 4 s window after the spin ends");
			if (tick == 130) check(boosted, "riptide: open while still airborne");
			if (tick == 40 + 81 + 100 /* 181 */ ) check(!boosted, "riptide: closed after landing and the window");
		}
		// mace smash from a tower: single stamp, 10 s fall, landing before 15 s
		stamp = t0; boosted = false; for (int tick = 0; tick < 300; tick++) { long now = t0 + tick * 50L; boosted = BoostWindow.boosted(now, stamp, tick >= 200, boosted); if (tick == 150) check(boosted, "mace: open mid-fall at 7.5 s"); if (tick == 200 + 80) check(!boosted, "mace: closed after landing plus window"); }
		// wind charge held in the air by a cheater: one stamp does not exempt forever
		check(!BoostWindow.boosted(t0 + BoostWindow.MAX_AIR_MS, t0, false, true), "a single boost cannot exempt forever");
		check(BoostWindow.chargeCounts(true, 400) && !BoostWindow.chargeCounts(false, 400) && BoostWindow.chargeCounts(false, 9), "wind charge ownership scoping");
		System.out.println(fails == 0 ? "GlideWindowTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
