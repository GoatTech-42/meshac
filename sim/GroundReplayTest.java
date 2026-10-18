import java.util.List;
public final class GroundReplayTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	static void eq(long[] r, long claims, long un, long max, long flags, long arms, String m) {
		check(r[0] == claims && r[1] == un && r[2] == max && r[3] == flags && r[4] == arms,
			m + " got claims=" + r[0] + " un=" + r[1] + " max=" + r[2] + " flags=" + r[3] + " arms=" + r[4]);
	}
	public static void main(String[] a) {
		// interleaved spoof under the 3-of-5 flag: count 1,1,2,2,3 - one arm at >=2 (third claim), one flag at 3
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true)), 5, 3, 2), 5, 3, 3, 1, 1, "interleaved spoof arms below the flag then flags");
		// eviction: five supported claims drain the window; a refill re-flags (rising edges counted separately)
		eq(GroundReplay.score(List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2), 13, 6, 3, 2, 2, "drained window re-arms and re-flags");
		// clear: exempt/grace ground claim resets the window and the edges
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2), 4, 4, 2, 0, 2, "clear resets count and edges");
		// tighter window: W=3 T=2 - supported claim evicts the oldest unsupported
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true)), 3, 2, 2), 5, 3, 2, 2, 2, "W=3 T=2 eviction re-triggers edges");
		// armAt == flagAt behaves like the flag itself
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 3), 3, 3, 3, 1, 1, "arm at the flag level mirrors the flag");
		// decay policy: a clear forgives only the oldest unsupported claim - the same stream that stays
		// sub-flag under full clear now flags (grace-boundary persistence catches the grace-farming spoofer)
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 4, 4, 3, 1, 2, "decay keeps all but the oldest evidence across a grace boundary; arm re-fires after dipping below");
		// decay on an empty window is a no-op, same as clear
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(false), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 2, 1, 1, 0, 0, "decay on a zero window is a no-op");
		// decay drops the flag latch only when the surviving count falls below the level: flag re-fires, arm does not
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 4, 4, 3, 2, 1, "decay re-flags without re-arming while the arm level holds");
		// consecutive grace boundaries (double self-damage): two decays, arm drops only after the second, re-arms on the next claim
		eq(GroundReplay.score(List.of(GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true)), 5, 3, 2, true), 4, 4, 3, 1, 2, "two consecutive decays forgive two oldest claims");
		// K4 (reviewer FIX 2): the jar latches arm/flag, so agreement uses maxCount + presence, not edge counts.
		// The drained-window stream has TWO flag edges and TWO arm edges but ONE latched presence each.
		long[] k4 = GroundReplay.score(List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(true),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true)), 5, 3, 2);
		check(k4[3] == 2 && k4[4] == 2, "K4: edge counts stay available for tuning");
		check(k4[2] == 3 && k4[3] > 0 && k4[4] > 0, "K4 agreement: maxCount=3, flag presence + first-arm presence both PRESENT");
		long[] k4z = GroundReplay.score(List.of(GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false)), 5, 3, 2);
		check(k4z[2] == 0 && k4z[3] == 0 && k4z[4] == 0, "K4 agreement: a clean stream has maxCount 0 and both presences absent");
		// === D-ROW FIXTURES (grace-boundary persistence, patch 6; the battery D-row evidence set) ===
		// D1 grace-farm cheat stream: spoof, spoof, grace-clear, spoof, spoof.
		List<GroundReplay.Ev> d1 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(),
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true));
		long[] d1Live = GroundReplay.score(d1, 5, 3, 2), d1Decay = GroundReplay.score(d1, 5, 3, 2, true);
		// DIVERGENCE IS THE EVIDENCE: live policy misses (maxCount 2, flag absent), decay flags (count reaches 3)
		eq(d1Live, 4, 4, 2, 0, 2, "D1 live: grace-farming beats the full clear (the known hole)");
		eq(d1Decay, 4, 4, 3, 1, 2, "D1 decay: the same stream FLAGS - grace-boundary persistence catches the farmer");
		check(d1Live[3] == 0 && d1Decay[3] > 0, "D1: the live-vs-decay divergence on one stream is the D-row evidence");
		// D2 legit knockback landing: all supported claims around a grace clear - both policies spotless
		List<GroundReplay.Ev> d2 = List.of(
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.clear(),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false));
		eq(GroundReplay.score(d2, 5, 3, 2), 4, 0, 0, 0, 0, "D2 live: legit knockback landing is clean");
		eq(GroundReplay.score(d2, 5, 3, 2, true), 4, 0, 0, 0, 0, "D2 decay: legit knockback landing stays clean");
		// D3 legit medium dip with borderline claims around the exit: decay must NOT invent an FP -
		// maxCount stays 2 under both policies (flagAt=3), flag/arm presence absent on both
		List<GroundReplay.Ev> d3 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.clear(),
			GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false), GroundReplay.Ev.claim(false));
		long[] d3Live = GroundReplay.score(d3, 5, 3, 2), d3Decay = GroundReplay.score(d3, 5, 3, 2, true);
		eq(d3Live, 5, 2, 2, 0, 1, "D3 live: borderline claims die at the clear");
		eq(d3Decay, 5, 2, 2, 0, 1, "D3 decay: forgiveness keeps the same sub-flag ceiling - no invented FP");
		// D4 double grace-farm (consecutive boundaries): decay forgives one claim per boundary, not the window
		List<GroundReplay.Ev> d4 = List.of(
			GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true), GroundReplay.Ev.claim(true),
			GroundReplay.Ev.clear(), GroundReplay.Ev.clear(), GroundReplay.Ev.claim(true));
		eq(GroundReplay.score(d4, 5, 3, 2, true), 4, 4, 3, 1, 2, "D4 decay: two boundaries forgive exactly two claims");
		System.out.println(fails == 0 ? "GroundReplayTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
