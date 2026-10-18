import dev.meshac.veil.ViewerPolicy;

public class ViewerPolicyTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		long jv = java.util.UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5").getMostSignificantBits();
		long bv = new java.util.UUID(0L, 2535412345L).getMostSignificantBits();
		check(!ViewerPolicy.bedrock(jv), "java uuid is not bedrock");
		check(ViewerPolicy.bedrock(bv), "zero high half is bedrock");
		check(ViewerPolicy.showAll(bv, false), "bedrock viewer sees all by default");
		check(!ViewerPolicy.showAll(bv, true), "opt-in veils bedrock");
		check(!ViewerPolicy.showAll(jv, false) && !ViewerPolicy.showAll(jv, true), "java viewers always veiled");
		// offline-mode UUIDs are v3 hashes: the version nibble makes the high half nonzero for every name, including a faked dot prefix
		for (String n : new String[]{".Steve", ".", "", "Notch"}) check(!ViewerPolicy.bedrock(java.util.UUID.nameUUIDFromBytes(("OfflinePlayer:" + n).getBytes()).getMostSignificantBits()), "offline name " + n + " not bedrock");
		// the highest bit pattern close to zero is still java
		check(!ViewerPolicy.bedrock(1L) && !ViewerPolicy.bedrock(Long.MIN_VALUE), "near zero is java");
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("ViewerPolicyTest ok");
	}
}
