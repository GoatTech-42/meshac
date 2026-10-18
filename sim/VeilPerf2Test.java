import dev.meshac.veil.EspCore;
import java.util.Random;

/** EspCore pair table: behaviour of the open-addressing map, counters, sweep and viewer forget. */
public class VeilPerf2Test {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	static void hide(EspCore e, int v, int en, long t) { for (int i = 0; i < 3; i++) e.update(v, en, t + i * 4, 30, false, false); }
	public static void main(String[] a) {
		EspCore.Params p = new EspCore.Params();
		// map correctness against a reference under growth and removals (backward shift delete)
		EspCore e = new EspCore(p); Random r = new Random(5); java.util.HashSet<Long> ref = new java.util.HashSet<>();
		for (int i = 0; i < 20000; i++) { int v = r.nextInt(60), en = r.nextInt(900) - 100; hide(e, v, en, 0); ref.add(((long) v << 32) ^ (en & 0xFFFFFFFFL)); }
		check(e.size() == ref.size(), "size " + e.size() + " vs " + ref.size());
		int cnt = 0; for (long k : ref) if (e.hidden((int) (k >> 32), (int) k)) cnt++;
		check(cnt == ref.size(), "all hidden after growth, got " + cnt);
		java.util.ArrayList<Long> list = new java.util.ArrayList<>(ref); java.util.Collections.shuffle(list, r);
		for (int i = 0; i < list.size() / 2; i++) { long k = list.get(i); e.forget((int) (k >> 32), (int) k); }
		boolean ok = true; for (int i = 0; i < list.size(); i++) { long k = list.get(i); boolean should = i >= list.size() / 2; ok &= e.hidden((int) (k >> 32), (int) k) == should; }
		check(ok, "after removing half, the other half is still found (probe runs intact)");
		check(e.size() == list.size() - list.size() / 2, "size after removal");
		check(e.anyHidden(), "anyHidden true while pairs hidden");
		// counter tracks hide, show, forget, clear
		EspCore c = new EspCore(p); check(!c.anyHidden(), "empty");
		hide(c, 1, 1, 0); check(c.anyHidden(), "after hide");
		c.update(1, 1, 20, 30, false, true); check(!c.anyHidden(), "after show");
		hide(c, 1, 2, 100); hide(c, 1, 3, 100); c.forget(1, 2); check(c.anyHidden(), "one still hidden after forgetting one"); c.forget(1, 3); check(!c.anyHidden(), "none after forgetting both");
		hide(c, 2, 2, 200); c.clear(); check(!c.anyHidden() && c.size() == 0, "clear resets counter and table");
		// forgetViewer: only that viewer's pairs go
		EspCore f = new EspCore(p); for (int en = 0; en < 50; en++) { hide(f, 1, en, 0); hide(f, 2, en, 0); }
		f.forgetViewer(1); check(f.size() == 50 && !f.hidden(1, 3) && f.hidden(2, 3), "forgetViewer removes one viewer only");
		// sweep: old visible pairs go, recent ones and hidden ones stay
		EspCore s = new EspCore(p);
		s.update(1, 1, 0, 30, false, true);      // visible, old
		s.update(1, 2, 1000, 30, false, true);   // visible, recent
		hide(s, 1, 3, 0);                         // hidden, old: must stay
		int removed = s.sweep(1500, 1200);
		check(removed == 1 && s.size() == 2 && s.hidden(1, 3), "sweep keeps hidden and recent pairs, removed " + removed);
		// a swept visible pair that returns starts fresh and behaves normally
		for (int i = 0; i < 3; i++) s.update(1, 1, 1600 + i * 4, 30, false, false);
		check(s.hidden(1, 1), "swept pair can be hidden again");
		// explicit key edge: viewer and entity ids at the extremes do not collide
		EspCore k = new EspCore(p); hide(k, Integer.MAX_VALUE, -1, 0); hide(k, -1, Integer.MAX_VALUE, 0); hide(k, 0, 0, 0);
		check(k.size() == 3 && k.hidden(Integer.MAX_VALUE, -1) && k.hidden(-1, Integer.MAX_VALUE) && k.hidden(0, 0), "extreme ids stay separate");
		if (fails > 0) { System.out.println(fails + " FAIL"); System.exit(1); }
		System.out.println("VeilPerf2Test ok");
	}
}
