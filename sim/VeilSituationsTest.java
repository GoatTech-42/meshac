import dev.meshac.veil.*;

/** Situations from the rig list as pure sequences: chunk border, dimension change, relog and respawn, mounts, spectator and creative viewers, Bedrock viewers, vanished targets. Each one drives the same pure classes the adapter drives, in the same order. */
public class VeilSituationsTest {
	static int fails = 0;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	static ExemptRule.F f() { ExemptRule.F x = new ExemptRule.F(); x.targetIsPlayer = true; return x; }
	static final EspCore.Params P = new EspCore.Params();

	/** One 4-tick cycle: exempt overrides LOS, blocked wall otherwise. */
	static EspCore.Action cycle(EspCore e, int v, int t, long tick, double dist, boolean exempt) { return e.update(v, t, tick, dist, exempt, false); }

	public static void main(String[] a) {
		// ---- exemptions ----
		check(!ExemptRule.exempt(f()), "a plain player target is cullable");
		ExemptRule.F x;
		x = f(); x.self = true; check(ExemptRule.exempt(x), "self");
		x = f(); x.mountedTogether = true; check(ExemptRule.exempt(x), "viewer rides the target or the target rides the viewer");
		x = f(); x.sharesVehicle = true; check(ExemptRule.exempt(x), "two riders of one boat or horse");
		x = f(); x.boss = true; check(ExemptRule.exempt(x), "boss");
		x = f(); x.glowing = true; check(ExemptRule.exempt(x), "glowing");
		x = f(); x.projectile = true; check(ExemptRule.exempt(x), "projectile");
		x = f(); x.leashedToViewer = true; check(ExemptRule.exempt(x), "leashed to the viewer");
		x = f(); x.viewerSpectatorOrCreative = true; check(ExemptRule.exempt(x), "spectator or creative viewer sees everyone");
		x = f(); x.bedrockShowAll = true; check(ExemptRule.exempt(x), "bedrock viewer with veilBedrock off");
		x = f(); x.sameTeam = true; check(ExemptRule.exempt(x), "team mate");
		x = f(); x.joinGrace = true; check(ExemptRule.exempt(x), "join, respawn or dimension grace");
		x = f(); x.targetIsPlayer = false; x.cullMobs = false; check(ExemptRule.exempt(x), "mob with mob culling off");
		x = f(); x.targetIsPlayer = false; x.cullMobs = true; check(!ExemptRule.exempt(x), "mob with mob culling on is cullable");
		// ---- viewer turns spectator while a player is hidden from them, then goes back ----
		EspCore e = new EspCore(P);
		for (int i = 0; i < 3; i++) cycle(e, 1, 2, 100 + i * 4, 40, false);
		check(e.hidden(1, 2), "setup: hidden");
		check(cycle(e, 1, 2, 120, 40, true) == EspCore.Action.SHOW && !e.hidden(1, 2), "spectator: shown on the next cycle");
		for (int i = 0; i < 50; i++) check(cycle(e, 1, 2, 124 + i * 4, 40, true) == EspCore.Action.NONE, "spectator: stays shown and never re-hides");
		EspCore.Action act = EspCore.Action.NONE; int cycles = 0;
		while (act != EspCore.Action.HIDE && cycles < 10) { act = cycle(e, 1, 2, 400 + cycles * 4, 40, false); cycles++; }
		check(cycles == P.cullAfterChecks, "back to survival: needs the full " + P.cullAfterChecks + " failed checks again, took " + cycles);
		// ---- mount: viewer gets on a boat with the hidden player ----
		EspCore m = new EspCore(P); for (int i = 0; i < 3; i++) cycle(m, 3, 4, i * 4, 30, false);
		check(m.hidden(3, 4), "setup");
		x = f(); x.sharesVehicle = true;
		check(cycle(m, 3, 4, 20, 30, ExemptRule.exempt(x)) == EspCore.Action.SHOW, "sharing a vehicle shows a hidden player");
		// ---- dimension change ----
		EspCore d = new EspCore(P); ViewerEpoch ep = new ViewerEpoch(); GraceTable g = new GraceTable();
		check(ep.observe(1, 100), "first sight of the viewer");
		check(!ep.observe(1, 100), "same level again: no new window");
		for (int i = 0; i < 3; i++) cycle(d, 1, 9, i * 4, 50, false);
		check(d.hidden(1, 9), "hidden in the overworld");
		check(ep.observe(1, 200), "level key changed (nether): new window");
		g.start(1, 12);
		x = f(); x.joinGrace = g.active(1, 16, 40);
		check(cycle(d, 1, 9, 16, 50, ExemptRule.exempt(x)) == EspCore.Action.SHOW, "pair left hidden in the old level is shown in the grace window, so it cannot stay hidden on return");
		check(ep.observe(1, 100), "back to the overworld: window again");
		// ---- relog ----
		EspCore r = new EspCore(P); ViewerEpoch re = new ViewerEpoch(); GraceTable rg = new GraceTable();
		re.observe(5, 100); for (int i = 0; i < 3; i++) cycle(r, 5, 6, i * 4, 50, false);
		check(r.hidden(5, 6) && r.anyHidden(), "setup");
		r.forgetViewer(5); rg.forget(5); re.forget(5);
		check(!r.hidden(5, 6) && !r.anyHidden() && r.size() == 0 && re.size() == 0 && rg.size() == 0, "disconnect drops every trace of the viewer");
		check(re.observe(5, 100), "same id comes back (or a new id joins): new window");
		// ---- the target logs out or dies while hidden ----
		EspCore gone = new EspCore(P);
		for (int i = 0; i < 3; i++) { cycle(gone, 1, 7, i * 4, 50, false); cycle(gone, 2, 7, i * 4, 50, false); cycle(gone, 1, 8, i * 4, 50, false); }
		check(gone.hiddenPairs() == 3, "three hidden pairs");
		check(gone.sweep(10000, 1200) == 0 && gone.hiddenPairs() == 3, "age sweep leaves hidden pairs alone (by design)");
		check(gone.dropGoneHidden(id -> id != 7) == 2, "entity 7 is gone: both of its hidden pairs dropped");
		check(gone.hiddenPairs() == 1 && gone.hidden(1, 8) && gone.size() == 1, "the live entity stays hidden and counted");
		check(gone.dropGoneHidden(id -> true) == 0, "nothing gone, nothing dropped");
		// ---- Bedrock viewers ----
		long bv = 0L, jv = 0x1234_0000_0000_4000L;
		for (boolean veilBedrock : new boolean[]{false, true}) {
			EspCore b = new EspCore(P); int hid = 0;
			for (int i = 0; i < 1000; i++) { x = f(); x.bedrockShowAll = ViewerPolicy.showAll(bv, veilBedrock); if (cycle(b, 8, 9, i * 4L, 60, ExemptRule.exempt(x)) == EspCore.Action.HIDE) hid++; }
			check(veilBedrock ? hid == 1 : hid == 0, "bedrock viewer, veilBedrock=" + veilBedrock + ": hides " + hid);
		}
		x = f(); x.bedrockShowAll = ViewerPolicy.showAll(jv, false); check(!ExemptRule.exempt(x), "a Java uuid is never treated as Bedrock");
		// ---- a Bedrock target is culled for a Java viewer like any player (the policy is about viewers only) ----
		EspCore bt = new EspCore(P); for (int i = 0; i < 3; i++) cycle(bt, 1, 99, i * 4, 40, false);
		check(bt.hidden(1, 99), "bedrock player as target is hidden from a Java viewer behind a wall");
		// ---- chunk border (anti-xray): every face of a section, one neighbour chunk not loaded at a time ----
		for (int face = 0; face < 6; face++) {
			final int ff = face;
			XrayCore.Blocks edge = new XrayCore.Blocks() {
				public boolean hidden(int x2, int y2, int z2) { return true; }
				public boolean transparent(int x2, int y2, int z2) { return ff == 0 ? x2 > 15 : ff == 1 ? x2 < 0 : ff == 2 ? z2 > 15 : ff == 3 ? z2 < 0 : ff == 4 ? y2 > 15 : y2 < 0; }
			};
			int[][] pos = {{15, 8, 8}, {0, 8, 8}, {8, 8, 15}, {8, 8, 0}, {8, 15, 8}, {8, 0, 8}};
			check(!XrayCore.shouldHide(edge, pos[face][0], pos[face][1], pos[face][2]), "border face " + face + " next to an unloaded or open neighbour shows the ore");
			check(XrayCore.shouldHide(edge, 8, 8, 8), "interior ore is hidden when only the edge is open, face " + face);
		}
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("VeilSituationsTest ok");
	}
}
