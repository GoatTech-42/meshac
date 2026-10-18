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
		System.out.println(fails == 0 ? "GroundReplayTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
