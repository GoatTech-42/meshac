import dev.meshac.veil.*;
import java.util.*;

/** The two border leaks: bounded detection, and the respawn / dimension change detection that replaces a version-specific Fabric event. */
public class VeilBorderTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	static int idx(int x, int y, int z) { return x | y << 4 | z << 8; }
	public static void main(String[] a) {
		// ---- BorderGuard ----
		check(BorderGuard.faces(idx(5, 3, 5)) == 0 && !BorderGuard.isBorder(idx(5, 3, 5)), "interior is not border");
		check(BorderGuard.faces(idx(0, 3, 5)) == BorderGuard.WEST && BorderGuard.faces(idx(15, 3, 5)) == BorderGuard.EAST, "west and east");
		check(BorderGuard.faces(idx(5, 3, 0)) == BorderGuard.NORTH && BorderGuard.faces(idx(5, 3, 15)) == BorderGuard.SOUTH, "north and south");
		check(BorderGuard.faces(idx(0, 9, 15)) == (BorderGuard.WEST | BorderGuard.SOUTH), "a corner touches two faces");
		check(BorderGuard.faces(idx(5, 0, 5)) == 0 && BorderGuard.faces(idx(5, 15, 5)) == 0, "top and bottom of a section are not chunk borders");
		int[] probes = {0};
		BorderGuard.Probe solid = (dx, dz) -> { probes[0]++; return false; };
		BorderGuard.Probe open = (dx, dz) -> { probes[0]++; return true; };
		int stored = BorderGuard.bits(idx(0, 3, 5), solid);
		check(stored == 0 && BorderGuard.unchanged(idx(0, 3, 5), stored, solid), "unchanged while the neighbour stays solid");
		check(!BorderGuard.unchanged(idx(0, 3, 5), stored, open), "a neighbour that became see-through is noticed");
		int s2 = BorderGuard.bits(idx(0, 3, 5), open); check(s2 == BorderGuard.WEST && BorderGuard.unchanged(idx(0, 3, 5), s2, open) && !BorderGuard.unchanged(idx(0, 3, 5), s2, solid), "and the reverse");
		probes[0] = 0; check(BorderGuard.unchanged(idx(7, 7, 7), 0, open) && probes[0] == 0, "interior makes no lookup at all");
		probes[0] = 0; BorderGuard.bits(idx(0, 1, 15), solid); check(probes[0] == 2, "a corner costs two lookups, not more: " + probes[0]);
		// worst case per section send: only border positions, at most 2 lookups each
		int border = 0; for (int i = 0; i < 4096; i++) if (BorderGuard.isBorder(i)) border++;
		check(border == 4096 - 14 * 14 * 16, "border cells are the outer ring: " + border);
		// ---- NeighbourMask ----
		int none = NeighbourMask.of(false, false, false, false), all = NeighbourMask.of(true, true, true, true);
		check(NeighbourMask.needsReplan(none, NeighbourMask.of(true, false, false, false)), "a newly loaded neighbour replans");
		check(!NeighbourMask.needsReplan(all, none), "an unload does not replan");
		check(!NeighbourMask.needsReplan(all, all) && !NeighbourMask.needsReplan(none, none), "nothing changed, nothing to do");
		int m = none, replans = 0; Random r = new Random(4);
		for (int i = 0; i < 1000; i++) { int now = r.nextInt(16); if (NeighbourMask.needsReplan(m, now)) { replans++; m = NeighbourMask.stored(now); } }
		check(replans > 0, "random loads do replan sometimes");
		// bound: starting from a fresh plan, rises can only happen while a bit is 0 in the stored mask; with the store updated at each replan the total is limited by the churn
		m = NeighbourMask.stored(NeighbourMask.of(true, false, false, false)); int steady = 0;
		for (int i = 0; i < 1000; i++) { int now = NeighbourMask.of(true, i % 2 == 0, false, false); if (NeighbourMask.needsReplan(m, now)) { steady++; m = NeighbourMask.stored(now); } }
		check(steady <= 2, "a neighbour flapping loaded/unloaded does not make a rebuild storm: " + steady);
		// ---- BorderDiff ----
		int[] oldP = {idx(0, 1, 1), idx(5, 5, 5), idx(15, 2, 2)}, newP = {idx(5, 5, 5), idx(15, 2, 2), idx(0, 9, 9), idx(3, 3, 0)};
		BorderDiff.Result d = BorderDiff.diff(oldP, newP, 16);
		check(!d.capped() && d.nowShown().equals(List.of(idx(0, 1, 1))) && d.nowHidden().equals(List.of(idx(3, 3, 0), idx(0, 9, 9)).stream().sorted().toList()), "border positions only: " + d);
		check(BorderDiff.diff(new int[]{idx(5, 5, 5)}, new int[]{idx(6, 6, 6)}, 16).nowHidden().isEmpty(), "interior changes are not resent");
		int[] many = new int[300]; for (int i = 0; i < many.length; i++) many[i] = idx(0, i % 16, i / 16 % 16) | (i / 256) << 12;
		check(BorderDiff.diff(new int[0], many, 64).capped() && BorderDiff.diff(new int[0], many, 64).nowHidden().isEmpty(), "past the cap nothing is sent");
		check(!BorderDiff.diff(new int[0], many, 1000).capped(), "under the cap everything is sent");
		// ---- ViewerEpoch: respawn gets a new id, dimension change keeps it but the level differs ----
		ViewerEpoch e = new ViewerEpoch();
		check(e.observe(1, 100), "first sight starts a window (join, or a respawned entity)");
		check(!e.observe(1, 100) && !e.observe(1, 100), "same level: no restart");
		check(e.observe(1, 200), "dimension change starts a window");
		check(!e.observe(1, 200) && e.observe(1, 100), "and going back starts another");
		check(e.observe(2, 100), "a respawn is a new id and starts a window");
		for (int i = 3; i < 1000; i++) e.observe(i, 100);
		int dropped = e.retainOnly(new HashSet<>(List.of(1, 5))); check(dropped == 997 && e.size() == 2, "offline and old ids are dropped: " + dropped + " " + e.size());
		e.forget(1); check(e.observe(1, 100), "forgotten viewer is new again");
		// wiring through GraceTable: a window starts only when observe says so
		GraceTable g = new GraceTable(); ViewerEpoch e2 = new ViewerEpoch();
		for (long t = 1000; t < 1100; t++) { if (e2.observe(7, t < 1050 ? 1 : 2)) g.start(7, t); }
		check(g.active(7, 1060, 40) && !g.active(7, 1090, 40), "window after the change at 1050 covers 1060 and has ended by 1090");
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("VeilBorderTest ok");
	}
}
