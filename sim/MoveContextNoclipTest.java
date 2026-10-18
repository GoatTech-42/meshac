import dev.meshac.MoveContext;

// Pure test: noclip from/to ctx line format and the per-player rate gate. No Minecraft.
public class MoveContextNoclipTest {
	private static int failed = 0;
	private static void check(boolean ok, String what) { if (!ok) { System.out.println("FAIL " + what); failed++; } }

	public static void main(String[] a) {
		String line = MoveContext.noclipLine(".RulerofClubs (bedrock)", 10.5, 64.0, -3.25, "minecraft:stone", 13.5, 64.0, -3.25, "minecraft:piston_head");
		check(line.startsWith("[ctx] .RulerofClubs (bedrock) noclip "), "prefix/player: " + line);
		check(line.contains("from=(10.500,64.000,-3.250) in=minecraft:stone"), "from end with block: " + line);
		check(line.contains("to=(13.500,64.000,-3.250) in=minecraft:piston_head"), "to end with block: " + line);

		// rate gate: first line always due, then at most one per second per player
		check(MoveContext.noclipCtxDue(5_000, 0), "first line due");
		check(!MoveContext.noclipCtxDue(5_999, 5_000), "inside the 1s window suppressed");
		check(MoveContext.noclipCtxDue(6_000, 5_000), "due again at exactly 1s");
		check(!MoveContext.noclipCtxDue(6_001, 5_500), "window restarts from the last logged line");
		check(MoveContext.noclipCtxDue(6_500, 5_500), "due 1s after the last logged line");
		check(MoveContext.noclipCtxDue(100_000_000L, 5_000), "long gap due");

		if (failed > 0) throw new AssertionError(failed + " failures");
		System.out.println("MoveContextNoclipTest ok");
	}
}
