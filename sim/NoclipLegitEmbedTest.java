import dev.meshac.MoveContext;
import dev.meshac.MovePath;

/**
 * C9 (FP-class-map pass 2): legit-embed scenarios against the noclip rules' pure surface.
 * Noclip has no repro row (FP-RISK-TABLE top gap 2); this is the sim half of proposal N01.
 * Two pure surfaces are exercised:
 *  - MovePath (the vanilla axis-aligned route behind Movement.blocked, Movement.java:425-427):
 *    legit launch/pearl/settle geometry must clear it, walls must not.
 *  - the two gate SHAPES in Movement.java, mirrored locally with line cites (a mirror, not
 *    the wired code - the wired path is server-coupled): the moved-through gate
 *    (d2 > 100 || d2 > 2.25 && blocked, Movement.java:141) and the inBlock embed counter
 *    (flags at >= 10 consecutive inside-solid packets, Movement.java:131-134).
 * The rig rows (real pearl/piston/teleport/settle/dismount runs with [ctx] lines captured)
 * stay proposal N01; these are scripted geometry stand-ins, not captures.
 */
public final class NoclipLegitEmbedTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	/** Same body model as MovePathTest: 0.6 wide, 1.8 tall, deflated 0.1 like the server test. */
	static MovePath.Solid world(double px, double py, double pz, double[]... cells) {
		return (ox, oy, oz) -> {
			double x0 = px + ox - 0.2, x1 = px + ox + 0.2, y0 = py + oy + 0.1, y1 = py + oy + 1.7, z0 = pz + oz - 0.2, z1 = pz + oz + 0.2;
			for (double[] c : cells) if (x1 > c[0] && x0 < c[3] && y1 > c[1] && y0 < c[4] && z1 > c[2] && z0 < c[5]) return true;
			return false;
		};
	}

	/** Mirror of the moved-through gate, Movement.java:141: d2 > 100 || (d2 > 2.25 && blocked). */
	static boolean movedThroughGate(double d2, boolean blocked) { return d2 > 100 || (d2 > 2.25 && blocked); }

	/** Mirror of the embed counter, Movement.java:131-134: flags at >= 10 consecutive inside-solid packets. */
	static boolean embedFlags(int consecutiveInsidePackets) { return consecutiveInsidePackets >= 10; }

	public static void main(String[] a) {
		// 1. pearl over a wall: rise 3, across 3, over a 2-high wall. The vanilla route (Y first,
		// then X) clears it; the old straight-line test clips the wall.
		double[] wall = {1, 0, -5, 2, 2, 5};
		MovePath.Solid w1 = world(0.5, 0, 0.5, wall);
		check(MovePath.chordBlocked(w1, 3, 3, 0, 0.5), "setup: straight line clips the wall the pearl clears");
		check(!MovePath.blocked(w1, 3, 3, 0, 0.5), "pearl up-and-over a wall must clear the vanilla route");
		check(!movedThroughGate(18, MovePath.blocked(w1, 3, 3, 0, 0.5)), "pearl over a wall signalled");

		// 2. piston push: 1.2 blocks sideways, possibly into a tight corner. Under the 1.5-block
		// gate (d2 > 2.25) the moved-through test does not even run.
		check(!movedThroughGate(1.44, true), "piston-push distance must stay under the moved-through gate");
		check(!movedThroughGate(2.24, true), "just under the 1.5-block gate signalled");

		// 3. chunk-settle burst shape (FP24 01:30 pattern: 3.2-3.3 block moves): over open ground
		// the move is fast but blocked=false, so no signal; through a wall it MUST signal.
		MovePath.Solid open = world(0.5, 64, 0.5);
		check(!MovePath.blocked(open, 3.25, 0, 0, 0.5), "3.25-block settle move over open ground blocked");
		check(!movedThroughGate(3.25 * 3.25, false), "3.25-block settle move over open ground signalled");
		MovePath.Solid walled = world(0.5, 64, 0.5, new double[] {1, 60, -5, 2, 67, 5});
		check(MovePath.blocked(walled, 3.25, 0, 0, 0.5), "3.25-block move THROUGH a wall not blocked");
		check(movedThroughGate(3.25 * 3.25, true), "3.25-block move through a wall must signal");

		// 4. boat dismount into a tight space: short move, under the gate, like the piston push.
		check(!movedThroughGate(0.81, true), "boat dismount (0.9 blocks) must stay under the gate");

		// 5. the 10-block absolute refusal: over 10 blocks signals whatever is in between.
		check(movedThroughGate(121, false), "an 11-block packet must signal even over open ground");

		// 6. embed tolerance (Movement.java:131-134 mirror): vanilla lets a player who starts
		// inside a wall walk on through; the check tolerates up to 9 consecutive inside-solid
		// packets. A pearl that embeds you for a few packets while you step out must not flag;
		// living inside blocks must.
		check(!embedFlags(9), "a 9-packet embed (stepping out of a pearl landing) flagged");
		check(embedFlags(10), "10 consecutive inside-solid packets missed");

		// 7. attribution: when a wall pass DOES flag, the paired [ctx] line must carry both
		// endpoints and both block names so a later log window can tell a pearl/piston embed
		// from a cheat (MoveContext.java:20-22).
		String ctx = MoveContext.noclipLine("mutant319", 10.5, 64.0, -3.25, "minecraft:air", 13.75, 64.0, -3.25, "minecraft:stone_bricks");
		check(ctx.contains("from=(10.500,64.000,-3.250) in=minecraft:air"), "ctx from-end attribution: " + ctx);
		check(ctx.contains("to=(13.750,64.000,-3.250) in=minecraft:stone_bricks"), "ctx to-end attribution: " + ctx);

		System.out.println(fails == 0 ? "NoclipLegitEmbedTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
