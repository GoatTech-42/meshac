import dev.meshac.veil.PacketPlan;
import java.util.Arrays;
import java.util.Random;
import java.util.function.UnaryOperator;

/**
 * Size/bytes invariant for chunkveil, no Minecraft needed.
 * Run from the repo root:
 *   javac -d /tmp/veiltest src/main/java/dev/meshac/veil/PacketPlan.java sim/veil/SizeBytesInvariantTest.java && java -cp /tmp/veiltest SizeBytesInvariantTest
 * Exit code 0 = all pass.
 *
 * Model: a section is 4096 block ids. Serialization is paletted like vanilla: byte bitsPerEntry, palette (distinct ids, 2 bytes each), packed data.
 * Veiling changes the number of distinct ids, so a veiled section serializes to a DIFFERENT size than the live one, which is exactly the situation where
 * a size pass and a write pass that disagree overflow the fixed buffer.
 */
public class SizeBytesInvariantTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; if (fails < 20) System.out.println("FAIL " + m); } }

	static final int STONE = 1, ORE_A = 10, ORE_B = 11, ORE_C = 12, FAKE_A = 20, FAKE_B = 21;
	static final class Sec { final int[] b; Sec(int[] b) { this.b = b; } }

	static int distinct(int[] b) { return (int) Arrays.stream(b).distinct().count(); }
	static int bits(int d) { return d <= 1 ? 0 : Math.max(4, 32 - Integer.numberOfLeadingZeros(d - 1)); }
	static int sizeOf(Sec s) { int d = distinct(s.b); return 1 + d * 2 + (4096 * bits(d) + 7) / 8; }
	static final class FixedBuf {
		final byte[] a; int pos; FixedBuf(int n) { a = new byte[n]; }
		void put(int v) { if (pos >= a.length) throw new IndexOutOfBoundsException("buffer overflow at " + pos); a[pos++] = (byte) v; }
	}
	static void write(Sec s, FixedBuf out) {
		int d = distinct(s.b), bt = bits(d);
		out.put(bt);
		int[] pal = Arrays.stream(s.b).distinct().sorted().toArray();
		for (int id : pal) { out.put(id >> 8); out.put(id); }
		int need = (4096 * bt + 7) / 8; for (int i = 0; i < need; i++) out.put(s.b[i % 4096] + i);
	}

	static Sec randomSection(Random r) {
		int[] b = new int[4096]; int kind = r.nextInt(4);
		for (int i = 0; i < 4096; i++) {
			int v = STONE;
			if (kind > 0 && r.nextInt(300) == 0) v = new int[] { ORE_A, ORE_B, ORE_C }[r.nextInt(3)];
			if (kind > 1 && r.nextInt(40) == 0) v = 100 + r.nextInt(60); // lots of distinct blocks pushes the palette up
			b[i] = v;
		}
		return new Sec(b);
	}
	/** The veil: ores to stone, plus a few fakes. Returns a COPY; null when nothing to do. May change the distinct count either way. */
	static Sec veil(Sec live, Random r) {
		int[] c = live.b.clone(); boolean any = false;
		for (int i = 0; i < 4096; i++) {
			if (c[i] == ORE_A || c[i] == ORE_B || c[i] == ORE_C) { c[i] = STONE; any = true; }
			else if (c[i] == STONE && r.nextInt(150) == 0) { c[i] = r.nextBoolean() ? FAKE_A : FAKE_B; any = true; }
		}
		return any ? new Sec(c) : null;
	}

	public static void main(String[] a) {
		Random r = new Random(1234);
		int veiledSections = 0, sizeDiffers = 0;
		for (int round = 0; round < 3000; round++) {
			int n = 4 + r.nextInt(21);
			Sec[] live = new Sec[n]; int[][] snap = new int[n][];
			for (int i = 0; i < n; i++) { live[i] = randomSection(r); snap[i] = live[i].b.clone(); }
			long vseed = r.nextLong();
			// scenario: 0 normal, 1 veil throws on some sections, 2 veil declines some sections (stale entries)
			int scenario = round % 3;
			Random vr = new Random(vseed);
			UnaryOperator<Sec> vf = s -> {
				if (scenario == 1 && vr.nextInt(5) == 0) throw new IllegalStateException("boom");
				if (scenario == 2 && vr.nextInt(3) == 0) return null;
				return veil(s, vr);
			};
			PacketPlan<Sec> plan = new PacketPlan<>(n);
			int total = 0;
			for (int i = 0; i < n; i++) total += plan.size(i, live[i], vf, SizeBytesInvariantTest::sizeOf);
			FixedBuf buf = new FixedBuf(total);
			try { for (int i = 0; i < n; i++) { Sec w = plan.forWrite(i, live[i]); if (w != live[i]) veiledSections++; write(w, buf); } }
			catch (IndexOutOfBoundsException e) { check(false, "round " + round + " overflow: " + e.getMessage()); }
			check(buf.pos == total, "round " + round + " wrote " + buf.pos + " of " + total);
			for (int i = 0; i < n; i++) { check(Arrays.equals(live[i].b, snap[i]), "round " + round + " live section " + i + " was modified"); if (plan.forWrite(i, live[i]) != live[i] && sizeOf(plan.forWrite(i, live[i])) != sizeOf(live[i])) sizeDiffers++; }
		}
		check(veiledSections > 1000, "test must actually veil sections, got " + veiledSections);
		check(sizeDiffers > 100, "test must hit sections whose veiled size differs from live, got " + sizeDiffers);

		// out-of-range and null handling
		PacketPlan<Sec> p = new PacketPlan<>(2); Sec s0 = randomSection(r);
		check(p.size(5, s0, x -> new Sec(new int[4096]), SizeBytesInvariantTest::sizeOf) == sizeOf(s0), "index beyond plan sizes the live section");
		check(p.forWrite(5, s0) == s0 && p.forWrite(-1, s0) == s0, "index beyond plan writes the live section");
		check(p.forWrite(0, s0) == s0, "section never sized writes live");

		// NEGATIVE CONTROL: the old design looked the replacement up separately in each pass and the lookup changed in between. It must be caught by this harness.
		int mismatches = 0; Random cr = new Random(99);
		for (int round = 0; round < 300; round++) {
			Sec s = randomSection(cr); Sec v1 = veil(s, new Random(1)), v2 = s; // size counted on the veiled copy, then the cache was invalidated and the write used the live section
			if (v1 == null) continue;
			int sized = sizeOf(v1); FixedBuf buf = new FixedBuf(sized);
			try { write(v2, buf); if (buf.pos != sized) mismatches++; } catch (IndexOutOfBoundsException e) { mismatches++; }
		}
		check(mismatches > 0, "negative control: independent lookups per pass should have produced a mismatch");
		System.out.println(fails == 0 ? "OK: size/bytes invariant holds (veiled sections " + veiledSections + ", size-differs " + sizeDiffers + ", control mismatches " + mismatches + ")" : fails + " FAILED");
		System.exit(fails == 0 ? 0 : 1);
	}
}
