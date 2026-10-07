import dev.meshac.veil.*;

/** javac -d out src/main/java/dev/meshac/veil/XrayCore.java src/main/java/dev/meshac/veil/EspCore.java test/VeilSelfTest.java && java -cp out VeilSelfTest */
public class VeilSelfTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		// grid world: solid everywhere except air pocket at (5,5,5); ore at (5,5,6) exposed, ore at (10,10,10) buried
		XrayCore.Blocks w = new XrayCore.Blocks() {
			public boolean hidden(int x, int y, int z) { return (x == 5 && y == 5 && z == 6) || (x == 10 && y == 10 && z == 10); }
			public boolean transparent(int x, int y, int z) { return x == 5 && y == 5 && z == 5; }
		};
		check(!XrayCore.shouldHide(w, 5, 5, 6), "exposed ore stays real");
		check(XrayCore.shouldHide(w, 10, 10, 10), "buried ore hidden");
		int n = 0; for (int i = 0; i < 100000; i++) if (XrayCore.fakeOreAt(42, i, i / 7, i / 13, 0.15)) n++;
		check(n > 14000 && n < 16000, "fake ratio ~15% got " + n);
		check(XrayCore.fakeOreAt(7, 1, 2, 3, 0.5) == XrayCore.fakeOreAt(7, 1, 2, 3, 0.5), "fake deterministic");
		EspCore.Params p = new EspCore.Params(); EspCore e = new EspCore(p);
		// behind wall, far: hides on 3rd failed check, shows on first pass
		check(e.update(1, 2, 0, 20, false, false) == EspCore.Action.NONE, "fail1");
		check(e.update(1, 2, 4, 20, false, false) == EspCore.Action.NONE, "fail2");
		check(e.update(1, 2, 8, 20, false, false) == EspCore.Action.HIDE, "hide on fail3");
		check(e.update(1, 2, 12, 20, false, true) == EspCore.Action.SHOW, "show on pass");
		// a flickering LOS (fail, pass, fail, pass...) never hides
		for (int i = 0; i < 50; i++) check(e.update(1, 3, i * 4, 20, false, i % 2 == 1) == EspCore.Action.NONE, "flicker never hides");
		// near entity never culled, combat exempt, exempt flag
		for (int i = 0; i < 10; i++) check(e.update(1, 4, i * 4, 5, false, false) == EspCore.Action.NONE, "near never culled");
		e.attacked(1, 5, 100); for (int i = 0; i < 10; i++) check(e.update(1, 5, 100 + i * 4, 30, false, false) != EspCore.Action.HIDE || 100 + i * 4 - 100 > 60, "recent combat not culled");
		for (int i = 0; i < 10; i++) check(e.update(1, 6, i * 4, 30, true, false) == EspCore.Action.NONE, "exempt never culled");
		// ray
		check(EspCore.segmentHitsBox(0, 0, 0, 10, 0, 0, 4, -1, -1, 5, 1, 1), "segment through box");
		check(!EspCore.segmentHitsBox(0, 0, 0, 3, 0, 0, 4, -1, -1, 5, 1, 1), "segment short of box");
		check(EspCore.samplePoints(0, 0, 0, 1, 2, 1).length == 13 * 3, "13 sample points");
		System.out.println(fails == 0 ? "ALL OK" : fails + " FAILURES");
		System.exit(fails == 0 ? 0 : 1);
	}
}
