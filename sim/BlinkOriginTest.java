import dev.meshac.BlinkOrigin;

/** javac -d /tmp/t src/main/java/dev/meshac/BlinkOrigin.java sim/BlinkOriginTest.java && java -cp /tmp/t BlinkOriginTest */
public final class BlinkOriginTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		// normal order: first burst packet reaches the main thread, snapshot taken, later the hit comes
		BlinkOrigin b = new BlinkOrigin();
		b.snapshot(111L, 111L, true, 10, 70, 20);
		check(b.onHit(111L, true) == BlinkOrigin.Act.ORIGIN, "fresh snapshot gives ORIGIN");
		check(b.x == 10 && b.y == 70 && b.z == 20, "origin position kept");
		check(b.onHit(111L, true) != BlinkOrigin.Act.ORIGIN, "origin is single-use");
		// non-first packets never move the snapshot
		b = new BlinkOrigin();
		b.snapshot(111L, 111L, true, 10, 70, 20);
		b.snapshot(112L, 111L, true, 99, 99, 99);
		b.onHit(111L, true);
		check(b.x == 10, "later packets overwrote the origin");
		// FINDING: main thread is behind. The hit is seen before it processed the first burst packet; an OLD snapshot from an aborted candidate burst must not be used
		b = new BlinkOrigin();
		b.snapshot(50L, 50L, true, -300, 64, 500); // candidate 50 never became a blink
		check(b.onHit(222L, true) == BlinkOrigin.Act.DEFER, "stale snapshot used instead of waiting");
		b.snapshot(222L, 222L, true, 10, 70, 20); // main thread reaches the first packet of the real burst
		check(b.onHit(222L, true) == BlinkOrigin.Act.ORIGIN && b.x == 10 && b.z == 20, "after deferral the fresh origin is used");
		// give up after MAX_DEFER position packets: signal only, no setback
		b = new BlinkOrigin();
		int n = 0; BlinkOrigin.Act act;
		do { act = b.onHit(333L, true); n++; } while (act == BlinkOrigin.Act.DEFER && n < 20);
		check(act == BlinkOrigin.Act.SIGNAL_ONLY && n == BlinkOrigin.MAX_DEFER + 1, "deferral not bounded: " + act + " after " + n);
		// status-only packet cannot take a snapshot, so no deferral
		b = new BlinkOrigin();
		check(b.onHit(444L, false) == BlinkOrigin.Act.SIGNAL_ONLY, "non-position packet deferred");
		// no snapshot before the player's position is initialised, null stamp, zero mark
		b = new BlinkOrigin(); b.snapshot(555L, 555L, false, 1, 2, 3); check(b.onHit(555L, false) == BlinkOrigin.Act.SIGNAL_ONLY, "snapshot taken before init");
		b = new BlinkOrigin(); b.snapshot(null, 555L, true, 1, 2, 3); check(b.onHit(555L, false) == BlinkOrigin.Act.SIGNAL_ONLY, "null stamp snapshot");
		b = new BlinkOrigin(); b.snapshot(0L, 0L, true, 1, 2, 3); check(b.onHit(0L, false) == BlinkOrigin.Act.SIGNAL_ONLY, "mark 0 snapshot");
		System.out.println(fails == 0 ? "BlinkOriginTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
