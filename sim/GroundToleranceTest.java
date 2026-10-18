import dev.meshac.GroundTolerance;
public final class GroundToleranceTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		check(GroundTolerance.partialTop(64.5) && GroundTolerance.partialTop(64.875) && GroundTolerance.partialTop(64.0625) && GroundTolerance.partialTop(64.9375), "partial tops");
		check(!GroundTolerance.partialTop(64.0) && !GroundTolerance.partialTop(65.0000001) && !GroundTolerance.partialTop(63.9999999) && !GroundTolerance.partialTop(-3.0), "whole tops");
		// player stands 0.125 above a chest top (client y desync): strip 0.1 misses, 0.13 hits, partial top: would pass
		check(GroundTolerance.wouldPass(true, true, 64.875), "0.125 over a chest should pass in compare mode");
		check(!GroundTolerance.wouldPass(true, true, 65.0), "over a full block the candidate adds nothing");
		check(!GroundTolerance.wouldPass(true, false, 64.875), "open air, nothing in the 0.13 strip: still a spoof");
		check(!GroundTolerance.wouldPass(false, true, 64.875), "not called air today: nothing to compare");
		// NoFall: claims ground 1.0 over a slab, strip never hits
		check(!GroundTolerance.wouldPass(true, false, 64.5), "a real NoFall gap over a slab stays a spoof");
		String l = GroundTolerance.line(".x", "minecraft:chest", 64.875, 65.0, 1);
		check(l.equals("[groundtol] would-have-passed .x support=minecraft:chest top=64.8750 feetY=65.0000 gap=0.1250 buf=1"), l);
		check(GroundTolerance.CANDIDATE > 0.125 && GroundTolerance.CANDIDATE < 0.2, "candidate covers the 0.125 offsets and stays small");
		// tolerated(): the gap bound on top of wouldPass
		check(GroundTolerance.tolerated(true, true, 64.875, 65.0), "chest stand (gap 0.125) must be tolerated");
		check(!GroundTolerance.tolerated(true, true, 64.875, 65.4), "floating 0.525 above a chest must not be tolerated");
		check(!GroundTolerance.tolerated(true, true, 65.0, 65.0), "full-block top must not be tolerated");
		check(!GroundTolerance.tolerated(true, false, 64.875, 65.0), "no collision in the 0.13 strip must not be tolerated");
		check(!GroundTolerance.tolerated(false, true, 64.875, 65.0), "strip-10 hit is plain support, not tolerance");
		check(GroundTolerance.tolerated(true, true, 65.5, 65.63), "fence-top stand with 0.13 gap is tolerated (fractional top admitted - battery row B5 watches this)");
		check(!GroundTolerance.tolerated(true, true, 65.5, 66.0), "floating 0.5 above a fence top must not be tolerated");
		String tl = GroundTolerance.toleratedLine(".x", "minecraft:snow", 64.25, 64.38, 1);
		check(tl.equals("[groundtol] tolerated .x support=minecraft:snow top=64.2500 feetY=64.3800 gap=0.1300 buf=1"), tl);
		check(!tl.contains("would-have-passed"), "tolerated line must never carry the compare-mode marker (reviewer NO-GO fix)");
		System.out.println(fails == 0 ? "GroundToleranceTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
