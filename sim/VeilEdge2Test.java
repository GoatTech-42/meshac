import dev.meshac.veil.*;
import java.util.*;

/** Edge cases found by reading the adapter: reveal of fake ores, which block changes matter, grace windows after respawn and dimension change. */
public class VeilEdge2Test {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		// ---- reveal: a fake ore next to a removed block must be resent as real stone ----
		int fakeSeen = 0, fakeResent = 0, plainSeen = 0, plainResent = 0;
		for (int i = 0; i < 20000; i++) {
			boolean fake = XrayCore.fakeOreAt(9, i, 12, 3, 0.1);
			boolean r = XrayCore.resendOnReveal(false, true, 9, i, 12, 3, 0.1);
			if (fake) { fakeSeen++; if (r) fakeResent++; } else { plainSeen++; if (r) plainResent++; }
		}
		check(fakeSeen > 1000 && fakeResent == fakeSeen, "every fake position is resent: " + fakeResent + "/" + fakeSeen);
		check(plainResent == 0, "plain stone is not resent: " + plainResent);
		check(XrayCore.resendOnReveal(true, false, 9, 1, 2, 3, 0.1), "a hidden real ore is still resent");
		check(!XrayCore.resendOnReveal(false, false, 9, 1, 2, 3, 1.0), "air, dirt and other non-stone are never resent");
		check(!XrayCore.resendOnReveal(false, true, 9, 1, 2, 3, 0) && !XrayCore.resendOnReveal(false, true, 9, 1, 2, 3, -1), "fake ore off: stone is not resent");
		// the reveal decision must use the same roll as the plan, or an exposed fake stays
		for (int i = 0; i < 5000; i++) { boolean planned = XrayCore.fakeOreAt(77, i, -20, i * 3, 0.02); check(XrayCore.resendOnReveal(false, true, 77, i, -20, i * 3, 0.02) == planned, "same roll as the plan at " + i); if (fails > 0) break; }
		// ---- which changes matter ----
		check(!ChangeFilter.affectsPlan(false, false, false, false, false, false), "crop, door-state, lamp, redstone: skip");
		check(!ChangeFilter.affectsPlan(false, false, true, true, false, false), "water or glass staying see-through: skip");
		check(ChangeFilter.affectsPlan(false, false, false, true, false, false), "a solid block removed: matters");
		check(ChangeFilter.affectsPlan(false, false, true, false, false, false), "a block placed into air: matters (exposure shrinks)");
		check(ChangeFilter.affectsPlan(true, false, false, true, false, false), "ore mined: matters");
		check(ChangeFilter.affectsPlan(false, true, true, false, false, false), "ore placed: matters");
		check(ChangeFilter.affectsPlan(false, false, false, true, true, false), "stone mined: matters");
		check(ChangeFilter.affectsPlan(false, false, false, false, false, true), "stone placed: matters");
		check(ChangeFilter.affectsPlan(true, true, false, false, false, false), "ore replaced by other ore: matters (the plan stores the real state)");
		// ---- grace after respawn / dimension change ----
		GraceTable g = new GraceTable();
		g.start(5, 1000);
		check(g.active(5, 1000, 40) && g.active(5, 1039, 40) && !g.active(5, 1040, 40), "window is exactly 40 ticks");
		check(!g.active(6, 1000, 40), "a viewer with no entry has no grace");
		g.start(5, 2000); check(g.active(5, 2001, 40), "a second start (dimension change) restarts the window");
		check(!g.active(5, 10, 40), "a tick counter that went backwards is expired, not eternal");
		// respawn: new id gets a window, the old id is forgotten
		g.forget(5); g.start(8, 3000); check(!g.active(5, 3001, 40) && g.active(8, 3001, 40), "old id gone, new id covered");
		// no pile-up: many respawns, sweep keeps the table small
		GraceTable big = new GraceTable(); for (int i = 0; i < 10000; i++) big.start(i, i);
		int removed = big.sweep(10000, 40); check(removed >= 9960 && big.size() <= 40, "sweep drops finished windows: removed " + removed + " left " + big.size());
		check(big.sweep(10000, 40) == 0, "sweep twice removes nothing more");
		// ---- chunk border: an unloaded neighbour is transparent, so a border block is shown, never hidden ----
		XrayCore.Blocks edge = new XrayCore.Blocks() {
			public boolean hidden(int x, int y, int z) { return x == 15 && y == 5 && z == 5; }
			public boolean transparent(int x, int y, int z) { return x >= 16; } // chunk to the east is not loaded
		};
		check(!XrayCore.shouldHide(edge, 15, 5, 5), "ore on the border of an unloaded chunk stays visible");
		XrayCore.Blocks loaded = new XrayCore.Blocks() { public boolean hidden(int x, int y, int z) { return x == 15 && y == 5 && z == 5; } public boolean transparent(int x, int y, int z) { return false; } };
		check(XrayCore.shouldHide(loaded, 15, 5, 5), "once the neighbour is loaded and solid, it hides");
		XrayCore.Blocks top = new XrayCore.Blocks() { public boolean hidden(int x, int y, int z) { return y == 319; } public boolean transparent(int x, int y, int z) { return false; } };
		check(XrayCore.shouldHide(top, 4, 319, 4), "world top has nothing to see through (adapter treats out of range as opaque)");
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("VeilEdge2Test ok");
	}
}
