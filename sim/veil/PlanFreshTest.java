import dev.meshac.veil.PlanFresh;

/** javac -d /tmp/t src/main/java/dev/meshac/veil/PlanFresh.java sim/veil/PlanFreshTest.java && java -cp /tmp/t PlanFreshTest */
public final class PlanFreshTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		Object STONE = new Object(), ORE = new Object(), AIR = new Object();
		Object[] section = new Object[4096]; java.util.Arrays.fill(section, STONE);
		int[] idx = {5, 300, 4000}; Object[] real = {ORE, ORE, STONE};
		section[5] = ORE; section[300] = ORE;
		check(PlanFresh.fresh(3, i -> idx[i], i -> real[i], p -> section[p]), "unchanged section is fresh");
		section[300] = STONE; // ore mined without an invalidate
		check(!PlanFresh.fresh(3, i -> idx[i], i -> real[i], p -> section[p]), "a planned block that changed makes the plan stale");
		section[300] = ORE; section[4000] = AIR;
		check(!PlanFresh.fresh(3, i -> idx[i], i -> real[i], p -> section[p]), "a changed fake-ore position (real=stone) makes the plan stale");
		section[4000] = STONE;
		check(PlanFresh.fresh(3, i -> idx[i], i -> real[i], p -> section[p]), "restored section is fresh again");
		check(PlanFresh.fresh(0, i -> 0, i -> null, p -> null), "empty plan is always fresh (known limit: exposure changes are not seen)");
		// known limit documented by the review: a neighbour changing exposure does not touch any planned position
		section[1234] = AIR;
		check(PlanFresh.fresh(3, i -> idx[i], i -> real[i], p -> section[p]), "exposure-only change is NOT detected here, invalidate() must cover it");
		if (fails > 0) { System.out.println(fails + " failure(s)"); System.exit(1); }
		System.out.println("OK: PlanFresh");
	}
}
