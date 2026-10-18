import dev.meshac.veil.PacketPlan;

/** Review finding 5: when the veil returns null the write pass must get the object that was SIZED, not whatever the caller passes later. javac -d /tmp/t src/main/java/dev/meshac/veil/PacketPlan.java sim/veil/PacketPlanFallbackTest.java && java -cp /tmp/t PacketPlanFallbackTest */
public final class PacketPlanFallbackTest {
	public static void main(String[] a) {
		StringBuilder A = new StringBuilder("aaaa"), B = new StringBuilder("bbbbbbbb");
		PacketPlan<StringBuilder> p = new PacketPlan<>(2);
		int n = p.size(0, A, x -> null, StringBuilder::length);
		check(n == 4, "size counted live A");
		check(p.forWrite(0, B) == A, "write pass returns the sized live object, not the argument");
		StringBuilder V = new StringBuilder("vv");
		p.size(1, A, x -> V, StringBuilder::length);
		check(p.forWrite(1, B) == V, "veiled replacement is written");
		PacketPlan<StringBuilder> q = new PacketPlan<>(1);
		check(q.forWrite(0, B) == B, "never sized slot falls back to the argument");
		check(q.forWrite(5, B) == B, "out of range falls back");
		PacketPlan<StringBuilder> r = new PacketPlan<>(1);
		r.size(0, A, x -> { throw new RuntimeException("boom"); }, StringBuilder::length);
		check(r.forWrite(0, B) == A, "veil exception fails open to the sized live object");
		System.out.println("OK: PacketPlan fallback identity holds");
	}
	static void check(boolean ok, String m) { if (!ok) { System.err.println("FAIL: " + m); System.exit(1); } }
}
