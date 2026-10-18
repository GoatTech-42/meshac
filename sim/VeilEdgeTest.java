import dev.meshac.veil.EspCore;
import dev.meshac.veil.XrayCore;
import java.util.HashSet;
import java.util.Set;

/** Chunkveil edge cases (anti-xray, anti-ESP, freecam geometry). Pure Java, no Minecraft. */
public class VeilEdgeTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }

	static final class W implements XrayCore.Blocks {
		final Set<Long> hidden = new HashSet<>(), air = new HashSet<>(), unknown = new HashSet<>();
		static long k(int x, int y, int z) { return ((long) x << 40) ^ ((long) y << 20) ^ z; }
		public boolean hidden(int x, int y, int z) { return hidden.contains(k(x, y, z)); }
		public boolean transparent(int x, int y, int z) { return air.contains(k(x, y, z)) || unknown.contains(k(x, y, z)); }
	}

	public static void main(String[] a) {
		// ---- anti-xray ----
		for (int[] d : XrayCore.NEIGHBOURS) {
			W w = new W(); w.hidden.add(W.k(0, 0, 0)); w.air.add(W.k(d[0], d[1], d[2]));
			check(!XrayCore.shouldHide(w, 0, 0, 0), "ore exposed through neighbour " + d[0] + "," + d[1] + "," + d[2] + " stays real");
		}
		W diag = new W(); diag.hidden.add(W.k(0, 0, 0)); diag.air.add(W.k(1, 1, 0)); diag.air.add(W.k(-1, 0, 1));
		check(XrayCore.shouldHide(diag, 0, 0, 0), "air only on edge or corner is not exposure, ore stays hidden");
		W unk = new W(); unk.hidden.add(W.k(15, 4, 15)); unk.unknown.add(W.k(16, 4, 15));
		check(!XrayCore.shouldHide(unk, 15, 4, 15), "ore at a chunk border next to an unloaded chunk fails open (shown)");
		W two = new W(); two.hidden.add(W.k(0, 0, 0)); two.hidden.add(W.k(1, 0, 0));
		check(XrayCore.shouldHide(two, 0, 0, 0) && XrayCore.shouldHide(two, 1, 0, 0), "adjacent buried ores (a vein) are both hidden");
		two.air.add(W.k(2, 0, 0));
		check(XrayCore.shouldHide(two, 0, 0, 0) && !XrayCore.shouldHide(two, 1, 0, 0), "vein: only the exposed end shows");
		check(!XrayCore.shouldHide(new W(), 3, 3, 3), "a block that is not on the hidden list is never hidden");

		// fake ore determinism and bounds, including negative and very large coordinates
		for (int[] p : new int[][] {{-1, -64, -1}, {-30000000, 319, 29999999}, {0, 0, 0}, {Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE}, {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE}}) {
			boolean f1 = XrayCore.fakeOreAt(99L, p[0], p[1], p[2], 0.3), f2 = XrayCore.fakeOreAt(99L, p[0], p[1], p[2], 0.3);
			check(f1 == f2, "fake ore deterministic at " + p[0] + "," + p[1] + "," + p[2]);
		}
		check(!XrayCore.fakeOreAt(1, 1, 1, 1, 0) && !XrayCore.fakeOreAt(1, 1, 1, 1, -0.5), "ratio <= 0 never fakes");
		int all = 0; for (int i = 0; i < 2000; i++) if (XrayCore.fakeOreAt(5, i, -i, i * 3, 1.0)) all++;
		check(all == 2000, "ratio 1 fakes every position, got " + all);
		int low = 0; for (int i = 0; i < 200000; i++) if (XrayCore.fakeOreAt(5, i % 997 - 500, i % 311 - 100, i / 7, 0.02)) low++;
		check(low > 3000 && low < 5000, "2% default ratio holds near 2%, got " + low);
		// two worlds (different salt) differ at some positions: a swapped dimension must not reuse the pattern
		int diff = 0; for (int i = 0; i < 2000; i++) if (XrayCore.fakeOreAt(1, i, 40, i, 0.5) != XrayCore.fakeOreAt(2, i, 40, i, 0.5)) diff++;
		check(diff > 500, "different salts give different patterns, differing at " + diff);
		// exposed stone is never faked by the adapter rule: shouldHide on a non-hidden block is false and exposed() covers the adapter's second condition
		W cave = new W(); cave.air.add(W.k(0, 1, 0));
		check(XrayCore.exposed(cave, 0, 0, 0) && !XrayCore.exposed(new W(), 0, 0, 0), "exposed() used for the fake-ore rule");

		// ---- anti-ESP ----
		EspCore.Params p = new EspCore.Params();
		// combat window boundary: 60 ticks inclusive
		EspCore e = new EspCore(p); e.attacked(1, 1, 1000);
		check(e.update(1, 1, 1060, 30, false, false) == EspCore.Action.NONE && !e.hidden(1, 1), "combat window inclusive at 60 ticks");
		EspCore.Action act = EspCore.Action.NONE; for (int i = 0; i < 3; i++) act = e.update(1, 1, 1061 + i * 4, 30, false, false);
		check(act == EspCore.Action.HIDE, "after the window it hides on the third failed check");
		// near radius boundary inclusive
		EspCore n = new EspCore(p); for (int i = 0; i < 10; i++) check(n.update(1, 2, i * 4, 8.0, false, false) == EspCore.Action.NONE, "exactly 8.0 blocks never culled");
		EspCore n2 = new EspCore(p); act = EspCore.Action.NONE; for (int i = 0; i < 3; i++) act = n2.update(1, 2, i * 4, 8.01, false, false);
		check(act == EspCore.Action.HIDE, "8.01 blocks can be culled");
		// hidden entity comes within 8 blocks or becomes exempt: shown at once
		EspCore h = new EspCore(p); for (int i = 0; i < 3; i++) h.update(1, 3, i * 4, 30, false, false);
		check(h.hidden(1, 3), "setup hidden");
		check(h.update(1, 3, 20, 7, false, false) == EspCore.Action.SHOW && !h.hidden(1, 3), "walking up shows it");
		EspCore x = new EspCore(p); for (int i = 0; i < 3; i++) x.update(1, 4, i * 4, 30, false, false);
		check(x.update(1, 4, 20, 30, true, false) == EspCore.Action.SHOW, "becoming exempt (glowing, riding, spectator viewer) shows it");
		// re-hide cycle works again after a show
		EspCore r = new EspCore(p); for (int i = 0; i < 3; i++) r.update(1, 5, i * 4, 30, false, false); r.update(1, 5, 12, 30, false, true);
		act = EspCore.Action.NONE; for (int i = 0; i < 3; i++) act = r.update(1, 5, 16 + i * 4, 30, false, false);
		check(act == EspCore.Action.HIDE, "hides again after being shown");
		// viewers are independent for the same entity, including negative ids
		EspCore v = new EspCore(p);
		for (int i = 0; i < 3; i++) { v.update(7, 9, i * 4, 30, false, false); v.update(-3, 9, i * 4, 30, false, true); }
		check(v.hidden(7, 9) && !v.hidden(-3, 9), "viewer 7 has it hidden, viewer -3 sees it");
		v.forget(7, 9); check(!v.hidden(7, 9) && !v.anyHidden(), "forget drops the pair");
		for (int i = 0; i < 3; i++) v.update(7, 9, 100 + i * 4, 30, false, false);
		v.clear(); check(!v.anyHidden(), "clear drops everything");
		// key collisions: many (viewer, entity) pairs stay separate
		EspCore k = new EspCore(p); Set<Long> seen = new HashSet<>();
		for (int vi = 0; vi < 40; vi++) for (int en = 0; en < 40; en++) { for (int i = 0; i < 3; i++) k.update(vi * 9973, en * 7919 + 1, i * 4, 30, false, false); }
		int hid = 0; for (int vi = 0; vi < 40; vi++) for (int en = 0; en < 40; en++) if (k.hidden(vi * 9973, en * 7919 + 1)) hid++;
		check(hid == 1600, "1600 distinct pairs all hidden, got " + hid);

		// ---- freecam geometry ----
		// A freecam camera outside a wall sees a victim behind it, the real eye does not: the server tests from the real eye, so a wall box between eye and target blocks.
		double[] eye = {0.5, 1.62, 0.5}, cam = {0.5, 1.62, 14.5}, tgt = {0.5, 1.0, 20.5};
		boolean realBlocked = EspCore.segmentHitsBox(eye[0], eye[1], eye[2], tgt[0], tgt[1], tgt[2], -5, 0, 9, 6, 4, 10);
		boolean camBlocked = EspCore.segmentHitsBox(cam[0], cam[1], cam[2], tgt[0], tgt[1], tgt[2], -5, 0, 9, 6, 4, 10);
		check(realBlocked && !camBlocked, "real eye is behind the wall (blocked), freecam would not be: server must use the real eye");
		// sample points: a target peeking one corner past the wall edge is visible from at least one sample
		double[] pts = EspCore.samplePoints(2.9, 0, 20, 3.5, 1.8, 20.6);
		boolean any = false; for (int i = 0; i < pts.length; i += 3) any |= !EspCore.segmentHitsBox(eye[0], eye[1], eye[2], pts[i], pts[i + 1], pts[i + 2], -5, 0, 9, 1.0, 4, 10);
		check(any, "a target whose corner clears the wall edge is not culled (multi-point test)");
		double[] fully = EspCore.samplePoints(-1, 0, 20, 1, 1.8, 22); boolean vis = false;
		for (int i = 0; i < fully.length; i += 3) vis |= !EspCore.segmentHitsBox(eye[0], eye[1], eye[2], fully[i], fully[i + 1], fully[i + 2], -5, 0, 9, 6, 4, 10);
		check(!vis, "a target fully behind a wide wall has no visible sample point");
		check(EspCore.segmentHitsBox(0, 0, 0, 0, 0, 0, -1, -1, -1, 1, 1, 1), "zero length segment inside a box counts as hit");
		check(!EspCore.segmentHitsBox(5, 5, 5, 5, 5, 5, -1, -1, -1, 1, 1, 1), "zero length segment outside a box is not a hit");

		if (fails > 0) { System.out.println(fails + " FAIL"); System.exit(1); }
		System.out.println("VeilEdgeTest ok");
	}
}
