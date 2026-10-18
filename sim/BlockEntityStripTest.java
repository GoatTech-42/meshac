import dev.meshac.veil.BlockEntityStrip;
import java.util.ArrayList;
import java.util.List;

public class BlockEntityStripTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		check(BlockEntityStrip.packed(16 + 3, 64 + 5, -16 + 7) == (3 | 5 << 4 | 7 << 8), "packed uses the low 4 bits of each axis, negatives included");
		check(BlockEntityStrip.packed(-1, -1, -1) == (15 | 15 << 4 | 15 << 8), "negative coordinates wrap like the section index");
		// walk of 5 block entities, entries 1 and 3 hidden
		List<String> list = new ArrayList<>(List.of("bed", "chest", "sign", "spawner", "hopper"));
		int[] d = BlockEntityStrip.drops(5, 5, i -> i == 1 || i == 3);
		check(d.length == 2 && d[0] == 3 && d[1] == 1, "highest first: " + java.util.Arrays.toString(d));
		for (int i : d) list.remove(i);
		check(list.equals(List.of("bed", "sign", "hopper")), "the right entries are gone: " + list);
		check(BlockEntityStrip.drops(5, 4, i -> true).length == 0, "size mismatch: nothing dropped");
		check(BlockEntityStrip.drops(4, 5, i -> true).length == 0, "size mismatch the other way: nothing dropped");
		check(BlockEntityStrip.drops(0, 0, i -> true).length == 0, "empty list");
		check(BlockEntityStrip.drops(3, 3, i -> false).length == 0, "nothing hidden");
		check(BlockEntityStrip.drops(3, 3, i -> true).length == 3, "all hidden");
		// the plan lookup
		int[] idx = {BlockEntityStrip.packed(1, 2, 3), BlockEntityStrip.packed(4, 5, 6), BlockEntityStrip.packed(7, 8, 9)};
		boolean[] be = {true, false, true}; // second entry is a fake ore over plain stone: no block entity to strip
		check(BlockEntityStrip.masked(idx, be, BlockEntityStrip.packed(1, 2, 3)), "masked chest");
		check(!BlockEntityStrip.masked(idx, be, BlockEntityStrip.packed(4, 5, 6)), "masked block without an entity");
		check(!BlockEntityStrip.masked(idx, be, BlockEntityStrip.packed(0, 0, 0)), "a position the plan does not mask (visible chest) keeps its entry");
		check(!BlockEntityStrip.masked(new int[0], new boolean[0], 5), "empty plan");
		// ---- the order assumption: a fake chunk with a shuffled map, a "constructor" that walks entrySet() and a strip that walks keySet() ----
		java.util.Random rnd = new java.util.Random(42);
		for (int round = 0; round < 3000; round++) {
			java.util.Map<Long, String> chunk = new java.util.HashMap<>(); // same kind of map the chunk uses
			int n = 1 + rnd.nextInt(60);
			java.util.Set<Long> hidden = new java.util.HashSet<>();
			for (int i = 0; i < n; i++) {
				long pos = ((long) (rnd.nextInt(16)) << 40) | ((long) (rnd.nextInt(384) - 64) << 20) | rnd.nextInt(16);
				chunk.put(pos, "be" + pos);
				if (rnd.nextInt(3) == 0) hidden.add(pos);
			}
			if (rnd.nextBoolean()) { // churn: remove and re-add some so the table is not in first-insert order
				java.util.List<Long> ks = new ArrayList<>(chunk.keySet()); java.util.Collections.shuffle(ks, rnd);
				for (int i = 0; i < ks.size() / 2; i++) { String v = chunk.remove(ks.get(i)); chunk.put(ks.get(i), v); }
			}
			List<String> packet = new ArrayList<>(); for (java.util.Map.Entry<Long, String> e : chunk.entrySet()) packet.add(e.getValue()); // the constructor's loop
			List<Long> order = new ArrayList<>(chunk.keySet());
			for (int i : BlockEntityStrip.drops(packet.size(), order.size(), i -> hidden.contains(order.get(i)))) packet.remove(i);
			List<String> want = new ArrayList<>(); for (long k : chunk.keySet()) if (!hidden.contains(k)) want.add("be" + k);
			check(packet.equals(want), "round " + round + ": the entries removed are exactly the hidden positions");
			if (fails > 0) break;
		}
		// a block entity added between the constructor's loop and the strip: sizes differ, nothing is dropped
		{
			java.util.Map<Long, String> chunk = new java.util.HashMap<>(); chunk.put(1L, "a"); chunk.put(2L, "b");
			List<String> packet = new ArrayList<>(chunk.values());
			chunk.put(3L, "c");
			List<Long> order = new ArrayList<>(chunk.keySet());
			check(BlockEntityStrip.drops(packet.size(), order.size(), i -> true).length == 0, "map changed after the packet list was built: no drops");
		}
  // Serialized-plan gate: no copy or no successful write means no stripping.
  BlockEntityStrip.Encoded encoded = new BlockEntityStrip.Encoded(3);
  encoded.wrote(0); // veiled() fell back unveiled: no copied mask
  check(!encoded.hidden(0, 12), "unveiled fallback keeps block entity despite write completion");
  encoded.copied(1, new int[]{12});
  check(!encoded.hidden(1, 12), "sized copy alone cannot strip before a successful write");
  encoded.wrote(1);
  check(encoded.hidden(1, 12) && !encoded.hidden(1, 13), "only exact applied positions from completed write strip");
  check(!encoded.hidden(2, 12), "another section plan does not leak");
  dev.meshac.veil.PacketPlan<Object> fallbackPlan = new dev.meshac.veil.PacketPlan<>(1);
  Object live = new Object(); BlockEntityStrip.Encoded fallbackMask = new BlockEntityStrip.Encoded(1);
  fallbackPlan.size(0, live, x -> null, x -> 8);
  check(fallbackPlan.forWrite(0, live) == live, "copy fallback serializes the live section");
  fallbackMask.wrote(0);
  check(!fallbackMask.hidden(0, 12), "copy fallback never records a strip mask");
  fallbackPlan.size(0, live, x -> { throw new IllegalStateException("copy failed"); }, x -> 8);
  check(fallbackPlan.forWrite(0, live) == live && !fallbackMask.hidden(0, 12), "copy exception also sends live and keeps entity");
  // A failure halfway through filtering cannot partly mutate the packet list.
  List<String> original = new ArrayList<>(List.of("chest", "sign", "spawner", "bed"));
  List<String> outgoing = original;
  boolean threw = false;
  try { outgoing = BlockEntityStrip.filtered(original, 4, j -> { if (j == 2) throw new IllegalStateException("mid-filter"); return j == 0; }); }
  catch (IllegalStateException expected) { threw = true; }
  check(threw && outgoing == original && original.equals(List.of("chest", "sign", "spawner", "bed")), "failure midway leaves original reference and every entry intact");
  List<String> committed = BlockEntityStrip.filtered(original, 4, j -> j == 0 || j == 2);
  check(committed.equals(List.of("sign", "bed")) && original.size() == 4, "success builds replacement privately for atomic reference swap");
  check(BlockEntityStrip.filtered(original, 3, j -> true) == original, "mismatch retains original reference");

		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("BlockEntityStripTest ok");
	}
}
