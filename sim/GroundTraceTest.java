import dev.meshac.GroundTrace;
public final class GroundTraceTest {
	public static void main(String[] a) {
		String l = GroundTrace.line(".x", true, "minecraft:air", 0, "minecraft:oak_slab", 0.5, "minecraft:stone", true, true, false, 3, -0.0784, 0.05, false);
		boolean ok = l.equals("[trace] groundctx .x flag=true feet=minecraft:air/0.0000 below=minecraft:oak_slab/0.5000 below2=minecraft:stone airBelow=true ground=true prevGround=false buf=3 dy=-0.078 dtS=0.050 exempt=false");
		if (!ok) { System.out.println("FAIL " + l); System.exit(1); }
		System.out.println("GroundTraceTest ok");
	}
}
