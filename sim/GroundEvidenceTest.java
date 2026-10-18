import dev.meshac.GroundEvidence;

/** javac -d /tmp/t src/main/java/dev/meshac/GroundEvidence.java sim/GroundEvidenceTest.java && java -cp /tmp/t GroundEvidenceTest */
public final class GroundEvidenceTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	static void feed(GroundEvidence e, String claims) { // 'U' = unsupported claim, 'S' = supported claim
		for (int i = 0; i < claims.length(); i++) e.add(claims.charAt(i) == 'U');
	}

	public static void main(String[] a) {
		// 3 consecutive unsupported claims flag (old behaviour preserved)
		GroundEvidence e = new GroundEvidence(5, 3);
		feed(e, "UU"); check(!e.flagged(), "flagged at 2 of 3");
		e.add(true); check(e.flagged(), "3 consecutive unsupported did not flag");

		// interleaved spoof (rig round-2 evasion): U S U S U flags at the 5th claim
		e = new GroundEvidence(5, 3);
		feed(e, "USUS"); check(!e.flagged(), "interleaved flagged early at 2 of 4");
		e.add(true); check(e.flagged() && e.unsupported() == 3, "interleaved U S U S U did not flag");

		// sparse jitter: never 3 of 5
		e = new GroundEvidence(5, 3);
		feed(e, "USSUSSUSSUSS"); check(!e.flagged(), "sparse jitter flagged");

		// supported-only never flags
		e = new GroundEvidence(5, 3);
		feed(e, "SSSSSSSS"); check(!e.flagged() && e.unsupported() == 0, "supported-only flagged");

		// sliding eviction: a flagged window drains as old unsupported claims age out
		e = new GroundEvidence(5, 3);
		feed(e, "UUU"); check(e.flagged(), "UUU did not flag");
		feed(e, "SS"); check(e.flagged(), "UUUSS should still hold 3 of 5");
		e.add(false); check(!e.flagged() && e.unsupported() == 2, "UUUSSS should drain to 2");

		// clear() resets
		e = new GroundEvidence(5, 3);
		feed(e, "UUU"); e.clear();
		check(!e.flagged() && e.unsupported() == 0, "clear did not reset");
		feed(e, "UU"); check(!e.flagged(), "flagged after clear with only 2");

		// window 1 threshold 1 degenerates to "this claim"
		e = new GroundEvidence(1, 1);
		e.add(true); check(e.flagged(), "1/1 window did not flag on U");
		e.add(false); check(!e.flagged(), "1/1 window stayed flagged on S");

		// bad constructor args rejected
		boolean threw = false;
		try { new GroundEvidence(0, 1); } catch (IllegalArgumentException x) { threw = true; }
		check(threw, "window 0 accepted");
		threw = false;
		try { new GroundEvidence(5, 6); } catch (IllegalArgumentException x) { threw = true; }
		check(threw, "threshold > window accepted");

		if (fails > 0) { System.out.println(fails + " FAILURES"); System.exit(1); }
		System.out.println("GroundEvidenceTest ok");
	}
}
