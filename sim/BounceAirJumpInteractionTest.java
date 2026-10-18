import dev.meshac.BounceTracker;
import dev.meshac.FpGuards;

// Air-jump guard (22aff82) x sneak/slime/honey/bed landing interactions. Vanilla 26.2 Entity.java, verbatim:
//   public boolean isSuppressingBounce() {
//      return this.isShiftKeyDown();
//   }
//   restituteMovementAfterCollisions: "double restitution = this.isSuppressingBounce() ? 0.0 : this.getEntityBounciness();"
// and the block's getBounceRestitution: slime 1.0, bed 0.66 (living entities), honey/ladder/ordinary 0.
// The Rig mirrors the Movement call path: land() hands the tracker (sneak ? 0 : block restitution) the way
// Movement.java does; packets run only the mid-air air-jump branch, and only that branch is skipped while the
// tracker covers the rise - the kick clock always advances, as in Movement. bouncing() is evaluated at the packet
// endpoint y, as Movement does (y = the packet's claimed position).
public class BounceAirJumpInteractionTest {
	private static int failed = 0;
	private static void check(boolean ok, String what) { if (!ok) { System.out.println("FAIL " + what); failed++; } }

	private static double impactSpeed(double h) { double v = 0, d = 0; while (d < h) { v = (v - 0.08) * 0.98; d -= v; if (v < -4) break; } return v; }

	private static final class Rig {
		final BounceTracker t = new BounceTracker();
		double y = 0, prevDy = -0.1;
		int ajN = 0, sinceJump = 99, flags = 0;
		long ajAt = 0, ms = 0;
		void land(double fallH, double blockR, boolean sneak) { t.land(y, fallH, sneak ? 0.0 : blockR, ms); } // Movement.java: sneaking suppresses the bounce, else the landed block's own restitution
		void packet(double dy) {
			ms += 50;
			double expect = (prevDy - 0.08) * 0.98;
			y += dy; prevDy = dy; // Movement reads y from the packet first: bouncing() sees the endpoint, not the start
			boolean covered = t.bouncing(y, ms);
			sinceJump++;
			if (!covered && FpGuards.airKick(dy, expect)) { ajN = FpGuards.airKickCount(ajN, ms, ajAt); ajAt = ms; if (FpGuards.airJumpFlag(sinceJump, ajN)) flags++; sinceJump = 0; }
		}
		double launch(double v0) { // vanilla re-launch and decay to the next landing; returns the impact speed
			double v = v0;
			while (true) {
				packet(v);
				v = (v - 0.08) * 0.98;
				if (y <= 0 && v < 0) { y = 0; return v; }
			}
		}
		void kicks(int n) { for (int i = 0; i < n; i++) { packet(0.5); packet(-0.1); } } // impulse hack: fresh kicks in mid-air
	}

	public static void main(String[] a) {
		// sneak-landing on slime: vanilla suppresses the bounce, so the player stops dead - no launches, no flags;
		// and the sneak opens no hole: kicks right after the sneaked landing flag
		{
			Rig r = new Rig();
			r.packet(impactSpeed(10)); // the fall
			r.land(10, 1.0, true); // sneaking: Movement passes restitution 0
			check(!r.t.armed(), "sneak-landing on slime does not arm");
			check(r.flags == 0, "sneak-landing produces no vanilla bounce and no flags");
			r.kicks(4);
			check(r.flags > 0, "impulse kicks after a sneak-landing flag (sneak opens no hole)");
		}
		// sneak mid-chain: covered while bouncing, disarmed at the sneaked re-land, kicks after flag
		{
			Rig r = new Rig();
			r.land(10, 1.0, false);
			double v = r.launch((0.08 - impactSpeed(10)) * 1.0);
			r.land(0, 1.0, false); // chaining re-land: fallH resets at contact
			v = r.launch((0.08 - v) * 1.0);
			check(r.flags == 0, "slime chain before the sneak stays clean");
			r.land(0, 1.0, true); // sneaked re-land: vanilla suppresses this landing's bounce
			check(!r.t.armed(), "sneaked re-land disarms the chain");
			check(r.flags == 0, "the sneaked re-land itself produces no vanilla bounce and no flags");
			r.kicks(4);
			check(r.flags > 0, "impulse kicks after the sneak-disarm flag");
		}
		// honey: restitution 0 even unsneaked - never arms, vanilla bounces nothing, kicks flag
		{
			Rig r = new Rig();
			r.packet(impactSpeed(10));
			r.land(10, 0.0, false); // honey block: getBounceRestitution 0
			check(!r.t.armed(), "honey landing never arms");
			check(r.flags == 0, "honey landing produces no vanilla bounce and no flags");
			r.kicks(4);
			check(r.flags > 0, "impulse kicks after a honey landing flag");
		}
		// bed chain (restitution 0.66): every decaying launch covered until the vanilla stop; the chain flags nothing
		{
			Rig r = new Rig();
			r.land(10, 0.66, false);
			double v = impactSpeed(10), v0 = (0.08 - v) * 0.66;
			int launches = 0;
			while (v0 >= 0.08 && launches < 60) {
				v = r.launch(v0);
				launches++;
				r.land(0, 0.66, false); // re-land: re-arms from the last apex while it stays above CHAIN_MIN
				v0 = (0.08 - v) * 0.66;
			}
			check(launches > 3, "bed chain produced a real chain: " + launches);
			check(r.flags == 0, "bed chain clean through decay (launches " + launches + ")");
		}
		// after the bed chain decays out, the tracker keeps only the accepted slack (decayed energy + 1.0, 10 s window,
		// per BounceTracker's documented cap); enough kicks run past the slack and flag
		{
			Rig r = new Rig();
			r.land(10, 0.66, false);
			double v = impactSpeed(10), v0 = (0.08 - v) * 0.66;
			int launches = 0;
			while (v0 >= 0.08 && launches < 60) { v = r.launch(v0); launches++; r.land(0, 0.66, false); v0 = (0.08 - v) * 0.66; }
			check(r.flags == 0, "setup: chain clean");
			r.kicks(8);
			check(r.flags > 0, "impulse kicks after the decayed chain run past the accepted slack and flag");
		}
		// cross-block: slime launch, re-land on honey - the honey landing arms nothing, the chain cover ends there
		{
			Rig r = new Rig();
			r.land(10, 1.0, false);
			r.launch((0.08 - impactSpeed(10)) * 1.0);
			r.land(0, 0.0, false); // walked onto honey for the next landing: restitution 0
			check(!r.t.armed(), "re-landing on honey disarms the slime chain");
			check(r.flags == 0, "vanilla bounces nothing off honey: no flags");
			r.kicks(4);
			check(r.flags > 0, "impulse kicks after the honey re-land flag");
		}
		// sneak on bed: same suppression, bed restitution never seen
		{
			Rig r = new Rig();
			r.packet(impactSpeed(6));
			r.land(6, 0.66, true);
			check(!r.t.armed(), "sneak-landing on bed does not arm");
			r.kicks(4);
			check(r.flags > 0, "impulse kicks after a sneak-landing on bed flag");
		}
		if (failed > 0) throw new AssertionError(failed + " failures");
		System.out.println("BounceAirJumpInteractionTest ok");
	}
}
