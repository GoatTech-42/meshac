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
		System.out.println(fails == 0 ? "GroundToleranceTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
