import dev.meshac.BounceTracker;

/** 9n rig repeats (26.2, fp-9n-bounce-slime-25 / bed-10): the restitution block must be found under the claimed landing, not under the previous server position. */
public final class BounceLandingProbeTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	static int oldBlockY(double prevServerY) { return (int) Math.floor(prevServerY) - 1; } // pl.blockPosition().below() at the pre-move position
	static int newBlockY(double claimedLandingY) { return BounceTracker.probeBlockY(claimedLandingY); }
	static boolean padHit(java.util.Set<String> pad, double x, double z) {
		int[] f = BounceTracker.footprint(x, z);
		for (int cx = f[0]; cx <= f[1]; cx++) for (int cz = f[2]; cz <= f[3]; cz++) if (pad.contains(cx + "," + cz)) return true;
		return false;
	}
	public static void main(String[] a) {
		// slime-25: slime block y=70 (top 71.0). rig samples: previous position 74.1665, landing claim 71.0
		check(oldBlockY(74.16649117765083) == 73, "old lookup read the air block at y=73 (restitution 0, tracker never armed)");
		check(newBlockY(71.0) == 70, "new lookup finds the slime block at y=70");
		// slime-10: 10-block fall is slower, previous position often within one block, which is why it was clean
		check(oldBlockY(71.6) == 70, "slime-10 got lucky when the previous position was within a block");
		// bed-10: bed block y=70 (top 70.5625). rig landing 70.5625; previous positions 72.0423 / 72.41
		check(oldBlockY(72.0423091326934) == 71 && oldBlockY(72.41564586078748) == 71, "old lookup missed the bed (air at y=71)");
		check(oldBlockY(70.5625) == 69, "old lookup standing on the bed read the floor under it");
		check(newBlockY(70.5625) == 70, "new lookup finds the bed block at y=70");
		check(newBlockY(71.0) == 70 && newBlockY(70.7) == 70 && newBlockY(71.19) == 70, "slime top and landing jitter below 0.2 stay on the slime block");
		// contact gate: a bounce apex 0.05 above the surface with ground=false and no real fall does not re-arm (and so cannot break a chain)
		check(BounceTracker.contact(true, 0.0) && BounceTracker.contact(false, 25.0) && !BounceTracker.contact(false, 0.05), "contact gate");
		// the chain survives the apex packet: land, bounce, apex packet is ignored, next landing re-arms
		BounceTracker t = new BounceTracker(); long ms = 1000;
		t.land(71.0, 25.0, 1.0, ms); check(t.armed(), "armed by the 25-block landing");
		if (BounceTracker.contact(false, 0.05)) t.land(71.05, 0.05, 1.0, ms + 300); // not called: no contact
		check(t.armed() && t.bouncing(71.0 + 24.0, ms + 400), "chain still armed at the apex packet; the armed tracker still covers a launch from the 25-block landing (tracker helper only, not the Movement callback)");
		// footprint: centered, edge-pad and negative coordinates. pad = set of "x,z" cells at the probe row.
		java.util.Set<String> pad = new java.util.HashSet<>(); pad.add("0,0");
		check(padHit(pad, 0.5, 0.5), "centered on the pad");
		check(padHit(pad, 1.1, 0.5), "edge: center x=1.1, pad ends at 1.0, the AABB still lands on it");
		check(!padHit(pad, 1.31, 0.5), "center x=1.31: footprint starts at 1.01, off the pad");
		check(padHit(pad, 0.5, 1.29), "edge on z");
		java.util.Set<String> neg = new java.util.HashSet<>(); neg.add("-1,-1");
		check(padHit(neg, -0.5, -0.5) && padHit(neg, -0.1, -0.5) && padHit(neg, -0.5, -0.1), "negative coordinates: pad at cell -1 found from x=-0.1 (footprint -0.4..0.2)");
		check(!padHit(neg, 0.31, -0.5), "negative: x=0.31 footprint starts at 0.01, off the pad");
		check(BounceTracker.probeBlockY(-59.0) == -60 && BounceTracker.probeBlockY(-59.5625) == -60, "negative y rows");
		// flat ground, no fall: a claimed 1.58 jump must stay a flag. Even on a slime block, with no fall nothing arms (MIN_FALL).
		BounceTracker flat = new BounceTracker();
		flat.land(64.0, 0.0, 1.0, ms); check(!flat.armed() && !flat.bouncing(65.58, ms + 50), "no fall on a slime block: nothing armed, a claimed 1.58 jump still flags");
		BounceTracker none = new BounceTracker();
		if (BounceTracker.contact(true, 0.0)) none.land(64.0, 0.0, 0.0, ms); check(!none.armed() && !none.bouncing(65.58, ms + 50), "flat ordinary ground: not armed");
		// 9n slime-25 trace: land 71.0 after a 25-block fall, tick 1 dy=+0.621 (g, sg=true, absorbed), tick 2 dy=+1.579 g=false sg=true -> was SIGNAL jump dy 1.579.
		BounceTracker tr = new BounceTracker(); long t0 = 5000;
		tr.land(71.0, 25.0, 1.0, t0);
		check(tr.armed() && tr.bouncing(71.0 + 0.621, t0 + 50), "slime-25 tick 1 (dy 0.621) absorbed");
		// tick 2: supported by the sg probe, claims ground=false; lastFallH = max(0, fallPeak - y) = 0 while rising, so contact() is false and land() is not called
		check(!BounceTracker.contact(false, 0.0), "tick 2 (g=false, rising) does not re-land");
		check(tr.armed() && tr.bouncing(71.0 + 0.621 + 1.579, t0 + 100), "slime-25 tick 2 (dy 1.579 after the absorbed 0.621) stays covered");
		// same with the ground claim true on a rising tick: land(fall 0) must not drop an armed chain (would re-open the FP)
		BounceTracker tg = new BounceTracker(); tg.land(71.0, 25.0, 1.0, t0); tg.bouncing(71.621, t0 + 50);
		if (BounceTracker.contact(true, 0.0)) tg.land(72.0, 0.0, 1.0, t0 + 100);
		check(tg.armed() && tg.bouncing(73.2, t0 + 120), "ground=true rising tick with no fall keeps the chain armed");
		// water/ladder cancel (restitution 0) must still drop an armed chain even at the apex
		BounceTracker tw = new BounceTracker(); tw.land(71.0, 25.0, 1.0, t0); tw.land(71.0, 0.0, 0.0, t0 + 50);
		check(!tw.armed() && !tw.bouncing(72.0, t0 + 60), "restitution 0 cancels the chain");
		// reviewer trace: land(71,25,1,1000); bouncing(71.621,1050); land(72,0,0,1100) leaves it unarmed, so 1.579 is no longer covered
		BounceTracker tr2 = new BounceTracker(); tr2.land(71.0, 25.0, 1.0, 1000); tr2.bouncing(71.621, 1050); tr2.land(72.0, 0.0, 0.0, 1100);
		check(!tr2.armed() && !tr2.bouncing(73.2, 1150), "restitution 0 (sneak/liquid/ladder/ordinary block) cancels, 1.579 not covered");
		// positive restitution early return must not refresh the window
		BounceTracker tr3 = new BounceTracker(); tr3.land(71.0, 25.0, 1.0, 1000); tr3.land(71.0, 0.0, 1.0, 5000);
		check(!tr3.bouncing(72.0, 11_500), "ignored rising ground claim does not extend the 10s window");
		// Movement trace lines and the cancel dispatch are pinned in source
		try {
			String src2 = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/dev/meshac/Movement.java")));
			check(src2.contains("s.bounce.land(y, 0, 0, System.currentTimeMillis())") && src2.contains("pl.isShiftKeyDown() ? 0.0") && src2.contains("[trace] bounce touchdown") && src2.contains("[trace] bounce land"), "Movement: liquid/ladder cancel dispatch, sneak zero restitution, trace lines present");
		} catch (java.io.IOException e) { check(false, "cannot read Movement.java"); }
		// native-unwitnessed: Movement itself needs a ServerPlayer (not buildable in the sim). The source witness below pins the wiring only.
		try {
			String src = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/java/dev/meshac/Movement.java")));
			check(src.contains("BounceTracker.contact(ground, s.lastFallH)") && src.contains("BounceBlocks.restitution(pl.level(), x, y, z)") && src.contains("pl.isShiftKeyDown() ? 0.0"), "Movement wiring: contact gate, footprint restitution, sneak suppression");
		} catch (java.io.IOException e) { check(false, "cannot read Movement.java"); }
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("BounceLandingProbeTest ok");
	}
}
