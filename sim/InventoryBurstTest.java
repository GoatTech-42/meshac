import dev.meshac.ActionEvidence;
import dev.meshac.FpGuards;
import dev.meshac.InventoryBurst;
import dev.meshac.InventoryBurst.Kind;

/** Replays what the vanilla client sends for legit inventory gestures (see InventoryBurst) and what a script sends, through the burst rule. */
public class InventoryBurstTest {
	static int fails = 0;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }

	/** The rule before this change: every packet counted. */
	static int oldFlags(long[] t) {
		long start = 0; int clicks = 0, flags = 0;
		for (long now : t) { if (now - start > 100) { start = now; clicks = 0; } if (++clicks > 10) { clicks = 0; flags++; } }
		return flags;
	}
	static int flags(long[] t, Kind[] k, int[] item, boolean bunched) {
		InventoryBurst b = new InventoryBurst(); int f = 0;
		for (int i = 0; i < t.length; i++) if (b.observe(t[i], k[i], item[i], bunched)) f++;
		return f;
	}
	static long[] same(int n, long at) { long[] t = new long[n]; java.util.Arrays.fill(t, at); return t; }
	static Kind[] all(int n, Kind k) { Kind[] r = new Kind[n]; java.util.Arrays.fill(r, k); return r; }
	static int[] items(int n, int v) { int[] r = new int[n]; java.util.Arrays.fill(r, v); return r; }

	public static void main(String[] a) {
		// 1. drag across a whole inventory: start + 36 slots + end, one tick
		long[] t = same(38, 5000);
		Kind[] k = all(38, Kind.QUICK_CRAFT);
		check(oldFlags(t) >= 1, "replay is meaningful: the old rule flags a 38 packet drag");
		check(flags(t, k, items(38, -1), false) == 0, "drag release is not a burst");
		// 2. shift + double click on one item across 36 slots, plus the first single shift click
		t = same(37, 8000); k = all(37, Kind.QUICK_MOVE);
		check(oldFlags(t) >= 1, "replay is meaningful: the old rule flags shift double click");
		check(flags(t, k, items(37, 42), false) == 0, "shift double click on one item is one run");
		// 3. two such runs of different items back to back
		t = same(60, 9000); k = all(60, Kind.QUICK_MOVE); int[] it = new int[60]; for (int i = 0; i < 60; i++) it[i] = i < 30 ? 1 : 2;
		check(flags(t, k, it, false) == 0, "two runs count 2, nowhere near the limit");
		// 4. AutoSteal style script: 27 stacks of mixed loot in one tick
		t = same(27, 12000); k = all(27, Kind.QUICK_MOVE); it = new int[27]; for (int i = 0; i < 27; i++) it[i] = i % 9;
		check(flags(t, k, it, false) >= 1, "a script emptying mixed loot in one tick is still flagged");
		// 4b. alternating two items still flags (every switch is a new run)
		it = new int[27]; for (int i = 0; i < 27; i++) it[i] = i % 2;
		check(flags(t, k, it, false) >= 1, "alternating two items is not a run");
		// 4c. unknown item never joins a run
		check(flags(t, k, items(27, -1), false) >= 1, "unknown items are counted one by one");
		// 5. the script cannot hide moves behind drag packets
		t = same(54, 15000); k = new Kind[54]; it = new int[54];
		for (int i = 0; i < 54; i++) { k[i] = i % 2 == 0 ? Kind.QUICK_CRAFT : Kind.QUICK_MOVE; it[i] = i; }
		check(flags(t, k, it, false) >= 1, "27 mixed shift clicks interleaved with drag packets still flag");
		// 6. ordinary clicks (pickup, throw, swap) are counted as before
		t = same(12, 20000); check(flags(t, all(12, Kind.OTHER), items(12, 5), false) == 1, "12 ordinary clicks in one ms flag once");
		check(flags(same(10, 20000), all(10, Kind.OTHER), items(10, 5), false) == 0, "10 is the limit, not over it");
		// 7. a fast human: 9 clicks per second
		t = new long[40]; for (int i = 0; i < 40; i++) t[i] = 30000 + i * 110L; check(flags(t, all(40, Kind.OTHER), items(40, 5), false) == 0, "9 clicks a second");
		// 8. backlog landing after a lag spike: 12 clicks of mixed items arrive in one ms, link was stalled
		t = same(12, 40000); k = all(12, Kind.QUICK_MOVE); it = new int[12]; for (int i = 0; i < 12; i++) it[i] = i;
		check(flags(t, k, it, false) == 1, "without the backlog signal this is a burst");
		check(flags(t, k, it, true) == 0, "right after a packet backlog (Ticks.bunched) nothing counts");
		// 9. a window that restarts: runs split by more than 100 ms
		InventoryBurst b = new InventoryBurst(); int f = 0;
		for (int i = 0; i < 100; i++) if (b.observe(50000 + i * 101L, Kind.OTHER, 1, false)) f++;
		check(f == 0, "one click per 101 ms never accumulates");
		// 10. a run that is interrupted by another click starts over
		b = new InventoryBurst(); int fl = 0;
		for (int i = 0; i < 12; i++) { if (b.observe(61000, Kind.QUICK_MOVE, 7, false)) fl++; if (b.observe(61000, Kind.OTHER, 7, false)) fl++; }
		check(fl >= 1, "shift click, other click, shift click... is not one run");
  InventoryBurst stress=new InventoryBurst();int dragFlags=0,mixedFlags=0;
  for(int w=0;w<300;w++)for(int stage=0;stage<38;stage++)if(stress.observe(100000+w*200L,Kind.QUICK_CRAFT,-1,false))dragFlags++;
  check(dragFlags==0,"60second11400dragstage events emit0burstsignals");
  stress=new InventoryBurst();int shiftFlags=0;
  for(int w=0;w<300;w++)for(int slot=0;slot<37;slot++)if(stress.observe(200000+w*200L,Kind.QUICK_MOVE,42,false))shiftFlags++;
  check(shiftFlags==0,"60second11100sameitemshift events emit0burstsignals");
  stress=new InventoryBurst();
  for(int w=0;w<300;w++)for(int slot=0;slot<27;slot++)if(stress.observe(300000+w*200L,Kind.QUICK_MOVE,slot%9,false))mixedFlags++;
  check(mixedFlags==600,"60second8100mixedhack events retain600burstsignals");
  System.out.println("Gesturestress60s:drag="+dragFlags+" shift="+shiftFlags+" mixedhack="+mixedFlags);
		// kind parsing: both names the game has used
		check(InventoryBurst.kind("QUICK_CRAFT") == Kind.QUICK_CRAFT && InventoryBurst.kind("QUICK_MOVE") == Kind.QUICK_MOVE, "kind names");
		check(InventoryBurst.kind("PICKUP") == Kind.OTHER && InventoryBurst.kind("UNKNOWN_ACCESSOR") == Kind.OTHER && InventoryBurst.kind(null) == Kind.OTHER, "unknown kinds are ordinary clicks");
		// the steady beat rule (unchanged): same-tick batches have gap 0 and never count as a beat
		for (int gap : new int[]{0, 1, 5, 19}) check(!ActionEvidence.steadyGap(gap, gap), "batched gap " + gap + " is not a beat");
		check(ActionEvidence.steadyGap(100, 100), "a 100 ms metronome still is");
		// Bedrock through Geyser: its click batches are skipped by UUID before any of this runs
		check(FpGuards.bedrockUuid(0L) && !FpGuards.bedrockUuid(0x1234_0000_0000_4000L), "bedrock uuid skip");
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("InventoryBurstTest ok");
	}
}
