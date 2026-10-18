import dev.meshac.BounceTracker;

// Sneak-on-slime. Vanilla 26.2 Entity.java, verbatim:
//   public boolean isSuppressingBounce() {
//      return this.isShiftKeyDown();
//   }
//   restituteMovementAfterCollisions: "double restitution = this.isSuppressingBounce() ? 0.0 : this.getEntityBounciness();"
//   and the vertical branch requires "!this.isSuppressingBounce()" before applying the block's restitution.
// So a sneaking landing produces no bounce. The Movement call site mirrors this by passing restitution 0
// when the landing player is sneaking.
public class SneakBounceTest {
	private static int failed = 0;
	private static void check(boolean ok, String what) { if (!ok) { System.out.println("FAIL " + what); failed++; } }

	public static void main(String[] a) {
		// sneak-landing after a real fall: vanilla suppresses the bounce, so the tracker must not arm
		BounceTracker t = new BounceTracker();
		t.land(71, 10, 0.0, 1000); // Movement passes 0 for a sneaking landing
		check(!t.armed(), "sneak-landing on slime does not arm (vanilla suppresses the bounce)");

		// sneak at the NEXT landing mid-chain: vanilla suppresses that landing's bounce, so the chain cover ends there
		t = new BounceTracker();
		t.land(71, 10, 1.0, 1000);
		check(t.bouncing(75, 1500), "chain covered before the sneak");
		t.land(71, 0, 0.0, 3000); // the next landing is sneaked: restitution 0
		check(!t.armed(), "a sneaked next landing disarms the chain");
		check(!t.bouncing(72, 3050), "a rise after the sneaked landing is not covered (vanilla produced no bounce there)");

		// letting go of sneak and falling again re-arms normally
		t.land(71, 6, 1.0, 6000);
		check(t.armed(), "unsneaked fall re-arms");

		if (failed > 0) throw new AssertionError(failed + " failures");
		System.out.println("SneakBounceTest ok");
	}
}
