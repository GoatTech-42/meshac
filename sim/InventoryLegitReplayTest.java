import dev.meshac.BeatCounter;
import dev.meshac.InventoryBurst;

/**
 * C4 (FP-class-map pass 2): Java-side legit inventory streams replayed through the pure
 * BeatCounter + InventoryBurst classes, wired exactly as Inventory.click does
 * (Inventory.java:22-29): arrival-stamped gap, QUICK_CRAFT returns early for BOTH rules,
 * burst sees Ticks.bunched, beat sees the arrival gap. The r06 row covers Bedrock only;
 * this is the Java leg's sim half. The rig capture of real Java clients stays a proposal
 * (COVER-PROPOSALS-C4-C9-C7 I01); these streams are scripted stand-ins, not captures.
 */
public final class InventoryLegitReplayTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	/** One click: kind name as ActionTrace.clickKind returns it, item identity (-1 unknown), gap ms since the previous click, bunched = a packet backlog landed. */
	static final class Click {
		final String kind; final int item; final long gap; final boolean bunched;
		Click(String kind, int item, long gap, boolean bunched) { this.kind = kind; this.item = item; this.gap = gap; this.bunched = bunched; }
	}

	/** Replay a stream through both rules the way Inventory.click wires them. Returns {burstSignals, beatSignals}. */
	static int[] run(Click... stream) {
		InventoryBurst burst = new InventoryBurst();
		BeatCounter beat = new BeatCounter();
		int b = 0, t = 0;
		long now = 1_000_000;
		for (Click c : stream) {
			now += c.gap;
			InventoryBurst.Kind kind = InventoryBurst.kind(c.kind);
			if (kind == InventoryBurst.Kind.QUICK_CRAFT) continue; // Inventory.java:24: drags return early for both rules
			if (burst.observe(now, kind, c.item, c.bunched)) b++;
			if (beat.observe(c.gap, now)) t++;
		}
		return new int[] {b, t};
	}

	/** A human sorting run: n OTHER clicks with irregular gaps cycling around the given base. */
	static Click[] manualSort(int n, long baseGap) {
		Click[] s = new Click[n];
		long[] jitter = {0, 37, -19, 53, 91, -41, 12, 77, -63, 25};
		for (int i = 0; i < n; i++) s[i] = new Click("SWAP", i % 7, baseGap + jitter[i % jitter.length], false);
		return s;
	}

	public static void main(String[] a) {
		// 1. manual sorting, slow (300ms) and fast (90ms) hands: zero of both
		check(run(manualSort(60, 300))[0] == 0, "slow manual sort burst-signalled");
		check(run(manualSort(60, 300))[1] == 0, "slow manual sort beat-signalled");
		check(run(manualSort(80, 90))[0] == 0, "fast manual sort burst-signalled");
		check(run(manualSort(80, 90))[1] == 0, "fast manual sort beat-signalled");

		// 2. shift double-click emptying a chest of one item: 27 QUICK_MOVE same item in one tick = one counted click
		{
			Click[] s = new Click[27];
			for (int i = 0; i < 27; i++) s[i] = new Click("QUICK_MOVE", 42, i == 0 ? 2000 : 0, false);
			check(run(s)[0] == 0, "shift double-click empty (one item) burst-signalled");
		}

		// 3. drag-paint a full inventory: ~30 QUICK_CRAFT packets in one tick, excluded from both rules
		{
			Click[] s = new Click[31];
			for (int i = 0; i < 30; i++) s[i] = new Click("QUICK_CRAFT", 7, i == 0 ? 1500 : 0, false);
			s[30] = new Click("PICKUP", 7, 0, false);
			check(run(s)[0] == 0 && run(s)[1] == 0, "drag-paint signalled");
		}

		// 4. latency leg: the same sort stream with a backlog landing mid-run (bunched=true resets the burst window)
		{
			Click[] s = manualSort(40, 90);
			s[20] = new Click("SWAP", 3, 900, true); // a held packet batch lands: burst resets, gap 900 is no beat
			check(run(s)[0] == 0, "bunched backlog burst-signalled");
			check(run(s)[1] == 0, "bunched backlog beat-signalled");
		}

		// 5. same-tick batch from a backlog release: gap 0 is never a steady beat (steadyGap floor is 20 ms)
		{
			Click[] s = new Click[12];
			for (int i = 0; i < 12; i++) s[i] = new Click("SWAP", i, i == 0 ? 1000 : 0, false);
			check(run(s)[1] == 0, "same-tick batch beat-signalled (gap 0 must never count)");
		}

		// positive controls - these MUST signal or the rules went soft
		// P1. a perfect 50 ms macro beat signals (and the session cap bounds a long beat:
		// SESSION_SIGNALS 10 fast, then at most 1 per RESIGNAL_MS - a minute-long tick-locked
		// sorter beat may not build unbounded heat, BeatCounter class comment)
		{
			Click[] s = new Click[30];
			for (int i = 0; i < 30; i++) s[i] = new Click("SWAP", i, i == 0 ? 2000 : 50, false);
			check(run(s)[1] > 0, "50 ms macro beat missed");
			Click[] longBeat = new Click[1200];
			for (int i = 0; i < 1200; i++) longBeat[i] = new Click("SWAP", i, i == 0 ? 2000 : 50, false);
			int sig = run(longBeat)[1];
			check(sig <= 10 + 6, "minute-long beat exceeded the session cap + resignal allowance: " + sig);
			check(sig > 0, "minute-long beat never signalled at all");
		}
		// P2. a scripted one-tick empty of MIXED loot bursts (the documented known cost:
		// single-item scripts read like shift double-click, mixed loot does not)
		{
			Click[] s = new Click[27];
			for (int i = 0; i < 27; i++) s[i] = new Click("QUICK_MOVE", i, i == 0 ? 2000 : 0, false);
			check(run(s)[0] > 0, "scripted mixed-loot empty missed");
		}
		// P3. plain click spam: 11 OTHER clicks inside 100 ms bursts
		{
			Click[] s = new Click[11];
			for (int i = 0; i < 11; i++) s[i] = new Click("PICKUP", i, i == 0 ? 2000 : 5, false);
			check(run(s)[0] > 0, "11 clicks in 100 ms missed");
		}

		// historical note, pinned as a comment only: the 10-11 enforce-era logs carried
		// "steady beat of 0 ms" lines (MA-ZAK4 evidence). The current steadyGap floor
		// (gap >= 20, ActionEvidence.java:6) cannot emit that string; those lines came
		// from the unidentified enforce-phase jar, not this rule. Case 5 above pins the
		// behavior that makes the 0 ms string impossible here.
		System.out.println(fails == 0 ? "InventoryLegitReplayTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
