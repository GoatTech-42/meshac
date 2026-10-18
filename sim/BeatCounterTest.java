import dev.meshac.ActionEvidence;
import dev.meshac.BeatCounter;
import java.util.Random;

/** Replays regular legal sorting and macro beats through the steady beat rule, old (signal every 2 moves on a long beat) against new (BeatCounter). Prints the table. */
public class BeatCounterTest {
	static int fails = 0;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }

	/** The rule as it was. */
	static int[] oldRun(long[] t) {
		long last = 0, lastGap = 0; int even = 0, sig = 0, first = -1;
		for (int i = 0; i < t.length; i++) {
			long gap = t[i] - last; last = t[i];
			if (ActionEvidence.steadyGap(gap, lastGap)) { if (++even >= 5) { even = 3; sig++; if (first < 0) first = i + 1; } } else even = 0;
			lastGap = gap;
		}
		return new int[]{sig, first};
	}
	static int[] newRun(long[] t) {
		BeatCounter b = new BeatCounter(); long last = 0; int sig = 0, first = -1;
		for (int i = 0; i < t.length; i++) { long gap = t[i] - last; last = t[i]; if (b.observe(gap, t[i])) { sig++; if (first < 0) first = i + 1; } }
		return new int[]{sig, first};
	}
	/** n clicks on a beat of period p ms with uniform network jitter of +-j ms. */
	static long[] beat(int n, long p, int j, long seed, double missTick) {
		Random r = new Random(seed); long[] t = new long[n]; long at = 100000;
		for (int i = 0; i < n; i++) { at += p; if (missTick > 0 && r.nextDouble() < missTick) at += 50; t[i] = at + (j == 0 ? 0 : r.nextInt(2 * j + 1) - j); }
		return t;
	}
	static long[] human(int n, double mean, double sd, long seed) {
		Random r = new Random(seed); long[] t = new long[n]; long at = 100000;
		for (int i = 0; i < n; i++) { at += Math.max(60, Math.round(mean + r.nextGaussian() * sd)); t[i] = at; }
		return t;
	}
	static void row(String name, long[] t) {
		int[] o = oldRun(t), n = newRun(t);
		System.out.printf("%-64s clicks %5d  old: %4d signals, first at click %3d   new: %3d signals, first at click %3d%n", name, t.length, o[0], o[1], n[0], n[1]);
	}

	public static void main(String[] a) {
		// legal: a client sorter sending one click per client tick, or every 2 or 3 ticks, for 60 s, with network jitter
		for (int p : new int[]{50, 100, 150}) { int n = 60000 / p; long[] t = beat(n, p, 3, p, 0); row("tick-locked sorter " + p + " ms, jitter +-3 ms, 60 s", t);
			check(newRun(t)[0] <= BeatCounter.SESSION_SIGNALS + 60000 / BeatCounter.RESIGNAL_MS + 1, "sorter " + p + " bounded: " + newRun(t)[0]);
			check(oldRun(t)[0] > 50, "replay is meaningful: old rule gives " + oldRun(t)[0]); }
		// legal: the same with a missed tick now and then
		long[] t2 = beat(1200, 50, 3, 9, 0.1); row("tick-locked sorter 50 ms with 10% missed ticks, 60 s", t2);
		// legal: a human working a chest by hand
		long[] h = human(500, 220, 60, 3); row("human, mean 220 ms sd 60", h); check(oldRun(h)[0] == 0 && newRun(h)[0] == 0, "human with natural spread never signals");
		long[] fast = human(1000, 100, 25, 4); row("fast human, mean 100 ms sd 25 (sorting, 60 s)", fast);
		// macros: must be caught as fast as before
		for (int p : new int[]{50, 75, 100, 150}) { long[] m = beat(27, p, 2, 100 + p, 0); int[] o = oldRun(m), n = newRun(m); row("AutoSteal macro " + p + " ms, one chest (27 stacks)", m);
			check(o[1] > 0 && n[1] == o[1], "macro " + p + " ms found at the same click, old " + o[1] + " new " + n[1]);
			check(n[0] == Math.min(o[0], BeatCounter.SESSION_SIGNALS), "macro " + p + " ms one chest: old " + o[0] + " new " + n[0]); }
		long[] dbl = beat(54, 100, 2, 77, 0); row("macro 100 ms, double chest (54 stacks)", dbl);
		check(newRun(dbl)[1] == oldRun(dbl)[1] && newRun(dbl)[0] >= BeatCounter.SESSION_SIGNALS, "double chest macro: same first click, at least the session allowance");
		long[] longMacro = beat(1200, 100, 2, 78, 0); row("macro holding a 100 ms beat for 2 minutes of moves", longMacro);
		check(newRun(longMacro)[0] >= BeatCounter.SESSION_SIGNALS + 10, "a macro that never stops still keeps signalling (every 10 s), got " + newRun(longMacro)[0]);
		// irregular gap inside one stretch of activity: no new allowance. A pause of 3 s or more: new allowance.
		long[] two = new long[40]; long at = 100000; for (int i = 0; i < 20; i++) { at += 100; two[i] = at; } at += 700; for (int i = 20; i < 40; i++) { at += 100; two[i] = at; }
		check(newRun(two)[0] == BeatCounter.SESSION_SIGNALS, "two beats 700 ms apart share one allowance, new " + newRun(two)[0]);
		long[] apart = new long[40]; at = 100000; for (int i = 0; i < 20; i++) { at += 100; apart[i] = at; } at += 3500; for (int i = 20; i < 40; i++) { at += 100; apart[i] = at; }
		check(newRun(apart)[0] == oldRun(apart)[0], "two beats 3.5 s apart (two chests): each gets its own, old " + oldRun(apart)[0] + " new " + newRun(apart)[0]);
		row("two chests 3.5 s apart, 100 ms beat", apart);
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("BeatCounterTest ok");
	}
}
