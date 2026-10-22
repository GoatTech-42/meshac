import java.nio.file.*;
import java.util.*;

/** Source witness for the nofall / ground-spoof live wiring in Movement (flatseries 0124, r2).
 *  Pins the WIRING the pure tests cannot see: which branch feeds the live window, what may clear
 *  it, what arms the delayed damage, and that every draft/shadow path stays log-only.
 *
 *  Commented-out-call counterexample: all STRUCTURAL checks run on a lexed copy of the source - line
 *  comments, block comments, string/char literal interiors and text blocks are blanked by a
 *  state-machine lexer (not regex), same length, newlines preserved - so a commented-out call
 *  or a brace inside a comment/string cannot satisfy or corrupt a structural pin. Comment and
 *  string-literal pins (PROPOSAL markers, log-only wording, trace markers) run on the RAW text,
 *  with literal-vs-comment distinction from the lexer's recorded literal spans.
 *  Run from the repo root. */
public final class GroundSpoofSourceWitnessTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	/** Lexed source: comment and literal interiors blanked to spaces (newlines kept), quote
	 *  characters retained so code shape survives. literalSpans holds (start,end) of every
	 *  string/char/text-block literal INCLUDING its quotes, for literal-content pins. */
	static final class Lexed {
		final String stripped; final List<int[]> literalSpans;
		Lexed(String s, List<int[]> l) { stripped = s; literalSpans = l; }
	}
	static Lexed lex(String src) {
		char[] out = src.toCharArray();
		List<int[]> spans = new ArrayList<>();
		int i = 0, n = src.length();
		while (i < n) {
			char c = src.charAt(i);
			if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') { // line comment
				int j = i;
				while (j < n && src.charAt(j) != '\n') { out[j++] = ' '; }
				i = j;
			} else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') { // block comment
				out[i] = ' '; out[i + 1] = ' '; int j = i + 2;
				while (j < n && !(src.charAt(j) == '*' && j + 1 < n && src.charAt(j + 1) == '/')) { if (src.charAt(j) != '\n') out[j] = ' '; j++; }
				if (j < n) { out[j] = ' '; out[j + 1] = ' '; j += 2; }
				i = j;
			} else if (c == '"' && i + 2 < n && src.charAt(i + 1) == '"' && src.charAt(i + 2) == '"') { // text block
				int start = i, j = i + 3;
				while (j < n && !(src.charAt(j) == '"' && j + 2 < n && src.charAt(j + 1) == '"' && src.charAt(j + 2) == '"')) { if (src.charAt(j) != '\n') out[j] = ' '; j++; }
				int end = Math.min(j + 3, n);
				for (int k = start + 3; k < end; k++) if (src.charAt(k) != '\n') out[k] = ' ';
				spans.add(new int[] {start, end});
				i = end;
			} else if (c == '"' || c == '\'') { // string / char literal
				int start = i; char q = c; i++;
				while (i < n) {
					char d = src.charAt(i);
					if (d == '\\') { if (src.charAt(i) != '\n') out[i] = ' '; if (i + 1 < n && src.charAt(i + 1) != '\n') out[i + 1] = ' '; i += 2; continue; }
					if (d == q) { i++; break; }
					if (d != '\n') out[i] = ' ';
					i++;
				}
				spans.add(new int[] {start, i});
			} else i++;
		}
		return new Lexed(new String(out), spans);
	}

	/** Balanced-brace block starting at the first occurrence of head, run on STRIPPED text.
	 *  Returns [start, endExclusive] or null. Offsets are valid in both stripped and raw (same length). */
	static int[] blockAt(String stripped, String head) {
		int i = stripped.indexOf(head);
		if (i < 0) return null;
		int open = stripped.indexOf('{', i);
		if (open < 0) return null;
		int depth = 0;
		for (int k = open; k < stripped.length(); k++) {
			char c = stripped.charAt(k);
			if (c == '{') depth++;
			else if (c == '}' && --depth == 0) return new int[] {i, k + 1};
		}
		return null;
	}
	static List<Integer> occurrences(String t, String needle) {
		List<Integer> out = new ArrayList<>();
		int i = 0;
		while ((i = t.indexOf(needle, i)) >= 0) { out.add(i); i += needle.length(); }
		return out;
	}
	/** pre ... post in order within maxGap chars (anchors that span a blanked string literal). */
	static boolean contains2(String t, String pre, String post, int maxGap) {
		int i = t.indexOf(pre);
		if (i < 0) return false;
		int j = t.indexOf(post, i + pre.length());
		return j >= 0 && j - (i + pre.length()) <= maxGap;
	}
	static String rawLine(String raw, int offset) {
		int s = raw.lastIndexOf('\n', offset) + 1;
		int e = raw.indexOf('\n', offset);
		return raw.substring(s, e < 0 ? raw.length() : e);
	}
	/** True when some string literal whose span intersects [from,to) contains needle (literal content, not a comment). */
	static boolean literalContains(Lexed lx, String raw, int from, int to, String needle) {
		for (int[] sp : lx.literalSpans)
			if (sp[0] < to && sp[1] > from && raw.substring(sp[0], sp[1]).contains(needle)) return true;
		return false;
	}

	/** All structural wiring pins, run against an arbitrary Movement source (real or mutated).
	 *  Returns failure messages; empty = the wiring satisfies every pin. */
	static List<String> wiringFailures(String raw) {
		List<String> F = new ArrayList<>();
		Lexed lx = lex(raw);
		String t = lx.stripped;
		java.util.function.BiConsumer<Boolean, String> ck = (ok, m) -> { if (!ok) F.add(m); };

		// 1. Live evidence window: exactly one add site in CODE, on the live ground-claim branch only.
		ck.accept(occurrences(t, "s.gndEv.add(").size() == 1, "gndEv.add must have exactly one code call site (commented-out adds do not count)");
		int[] live = blockAt(t, "if (ground && !exempt && s.grace <= 0) {");
		ck.accept(live != null, "live ground-claim branch not found");
		if (live != null) {
			String liveCode = t.substring(live[0], live[1]);
			ck.accept(liveCode.contains("s.gndEv.add(claimAir);"), "live branch feeds the evidence window");
			ck.accept(liveCode.contains("s.gndEv.flagged() && hit == null"), "flag wiring: window flag gates the nofall hit");
			ck.accept(literalContains(lx, raw, live[0], live[1], "nofall ground spoof"), "flag produces the nofall hit (literal)");
			ck.accept(!liveCode.contains("s.gndEv.clear()"), "live branch must never clear the window (air packets neither add nor erase)");
			ck.accept(liveCode.contains("if (!claimTol) {"), "tolerated partial-shape claims bypass the evidence window (classify mode)");
		}

		// 2. The window clears ONLY on recognized full-state-reset paths, and every clear line carries
		//    a gndreset/gndclear marker INSIDE A STRING LITERAL (GroundReplay K4 capture depends on them).
		List<Integer> clears = occurrences(t, "s.gndEv.clear()");
		ck.accept(clears.size() == 4, "gndEv.clear must have exactly four code call sites (blink-setback, exempt/grace, soft-return, post-flag), found " + clears.size());
		for (int off : clears) {
			int ls = raw.lastIndexOf('\n', off) + 1, le = raw.indexOf('\n', off);
			if (le < 0) le = raw.length();
			ck.accept(literalContains(lx, raw, ls, le, "gndreset") || literalContains(lx, raw, ls, le, "gndclear"),
				"a live-window clear without a trace marker STRING LITERAL on its line (GroundReplay capture would miss the reset)");
		}
		int[] graceBranch = blockAt(t, "} else if (ground) {");
		ck.accept(graceBranch != null && t.substring(graceBranch[0], graceBranch[1]).contains("s.gndEv.clear();"), "exempt/grace ground claim clears the window (fresh start)");
		ck.accept(graceBranch != null && t.substring(graceBranch[0], graceBranch[1]).contains("s.gndEvDecay.decay()"), "decay shadow forgives one at the grace boundary (log-only candidate)");
		int[] softReturn = blockAt(t, "if (step == Verdict.Step.LOG || !acts(ch[0])) {");
		ck.accept(softReturn != null && t.substring(softReturn[0], softReturn[1]).contains("s.gndEv.clear();") && t.substring(softReturn[0], softReturn[1]).contains("return null;"), "soft-return clears all evidence incl. the live window");

		// 3. Shadow path (non-displacing damage grace) never touches the live window.
		int[] shadow = blockAt(t, "} else if (ground && softGrace) {");
		ck.accept(shadow != null && t.substring(shadow[0], shadow[1]).contains("s.gndShadow.add(claimAir);"), "softGrace records shadow evidence");
		ck.accept(shadow != null && !t.substring(shadow[0], shadow[1]).contains("s.gndEv.add(") && !t.substring(shadow[0], shadow[1]).contains("s.gndEv.clear"), "shadow path must not add to or clear the live window");

		// 4. An unsupported ground claim is not a landing: the air-chain reset is guarded by !claimAir (CODE).
		ck.accept(t.contains("} else if (!claimAir) {"), "air chains reset only when the ground claim is supported (rig round-2 human high jump fix)");
		ck.accept(t.contains("if (!s.lastClaimAir) { s.sinceJump = 99; s.airY0 = s.ground ? sy : y; }"), "the packet AFTER a fake landing must not re-anchor the air chains either (lastClaimAir gate)");
		ck.accept(occurrences(t, "s.lastClaimAir = claimAir;").size() == 1, "exactly one lastClaimAir assignment, on the accepted-packet tail");

		// 5. Arming and damage sit behind the monitor gate (CODE).
		int chEq = t.indexOf("if (ch[0].equals(\"", t.indexOf("if (hit != null) {"));
		int[] nfArm = chEq < 0 ? null : blockAt(t, t.substring(chEq, t.indexOf(")) {", chEq) + 4));
		String nfArmCode = nfArm == null ? "" : t.substring(nfArm[0], nfArm[1]);
		ck.accept(nfArm != null, "nofall flag-handling block not found");
		ck.accept(nfArm != null && nfArmCode.contains("MonitorGate.armNoFall(acts(") && nfArmCode.indexOf("MonitorGate.armNoFall(acts(") < nfArmCode.indexOf("s.nfAt = System.currentTimeMillis()"), "nofall arm goes through MonitorGate.armNoFall");
		ck.accept(contains2(t, "if (s.nfAt != 0 && !acts(\"", ")) { s.nfAt = 0; s.fdMax = 0; }", 20), "monitor mode drops a pending NoFall record");
		ck.accept(contains2(t, "MonitorGate.processNoFall(s.nfAt, acts(\"", "))", 20), "pending NoFall processing gated");
		ck.accept(contains2(t, "MonitorGate.damage(acts(\"", "), drop, s.fdMax)", 20), "delayed fall damage gated");

		// 6. Status-only packet path: real support probe, exemption set, consecutive buffer, gated arm (CODE + literals).
		int[] so = blockAt(t, "private static double[] statusOnly(");
		ck.accept(so != null, "statusOnly path missing");
		if (so != null) {
			String soCode = t.substring(so[0], so[1]);
			ck.accept(soCode.contains("pl.isFallFlying()") && soCode.contains("pl.isInWater()") && soCode.contains("pl.getAbilities().mayfly") && soCode.contains("pl.isPassenger()"), "statusOnly keeps its exemption set");
			ck.accept(soCode.contains("s.bufStatus = air ? s.bufStatus + 1 : 0;") && soCode.contains("s.bufStatus < 3"), "statusOnly consecutive-3 buffer intact");
			ck.accept(soCode.contains("MonitorGate.armNoFall(acts("), "statusOnly arm gated");
			ck.accept(literalContains(lx, raw, so[0], so[1], "ground flag in mid-air"), "statusOnly signal detail (literal)");
			ck.accept(contains2(soCode, "if (step == Verdict.Step.HOLD) { s.freezeUntil = System.currentTimeMillis() + 1500; s.freezeCheck = \"", "; }", 20), "statusOnly HOLD records its check");
		}

		// 7. Drafts stay drafts: window/threshold pinned (CODE); strong-arm and decay shadows log-only.
		ck.accept(t.contains("GROUND_WINDOW = 5;") && t.contains("GROUND_FLAG_AT = 3;"), "window 5 / flag-at 3 pinned; a silent retune must trip this test");
		int[] strong = blockAt(t, "if (s.nfStrongAt == 0 && s.gndEv.unsupported() >= STRONG_ARM_AT");
		ck.accept(strong != null && literalContains(lx, raw, strong[0], strong[1], "log-only"), "P3-strong arm is marked log-only (literal)");
		ck.accept(strong != null && !t.substring(strong[0], strong[1]).contains("hit = \""), "P3-strong arm never produces a hit (stripped probe hit=quote)");
		ck.accept(literalContains(lx, raw, 0, raw.length(), "nofall strong-handback WOULD fire"), "strong handback is a WOULD-fire log, not an action");
		ck.accept(occurrences(t, "s.gndEvDecay.add(").size() == 1 && t.contains("if (TRACE) { s.gndEvDecay.add(claimAir);"), "decay shadow feeds only under TRACE");
		ck.accept(occurrences(t, "decay()").size() == 1, "GroundEvidence.decay() has exactly one (shadow) caller in Movement; promotion to live is a separate reviewed patch");

		// 8. Tolerance machinery is env-gated and off by default (CODE).
		ck.accept(contains2(t, "private static final boolean GROUND_TOL_COMPARE = \"", "\".equals(System.getenv(", 12) && contains2(t, "private static final boolean GROUND_TOL_COMPARE = \"", "\"));", 60), "GROUND_TOL compare mode env-gated, default off");
		ck.accept(contains2(t, "private static final boolean GROUND_TOL_CLASSIFY = \"", "\".equals(System.getenv(", 12) && contains2(t, "private static final boolean GROUND_TOL_CLASSIFY = \"", "\"));", 60), "GROUND_TOL classify mode env-gated, default off");
		// the exact env var name and mode words are literal pins on RAW:
		ck.accept(literalContains(lx, raw, 0, raw.length(), "MESHAC_GROUND_TOL") && literalContains(lx, raw, 0, raw.length(), "compare") && literalContains(lx, raw, 0, raw.length(), "classify"), "GROUND_TOL env name and mode literals present");
		return F;
	}

	/** Comment pins, on RAW text by design (the pins are comments). */
	static List<String> commentFailures(String raw) {
		List<String> F = new ArrayList<>();
		if (!raw.contains("PROPOSAL: sliding window over the last ground claims")) F.add("window is still marked PROPOSAL pending the legit battery");
		if (!raw.contains("an unsupported ground claim is not a landing")) F.add("round-2 invariant comment missing - check the !claimAir guard did not move");
		return F;
	}

	public static void main(String[] a) throws Exception {
		String raw = Files.readString(Paths.get("src/main/java/dev/meshac/Movement.java"));
		for (String m : wiringFailures(raw)) check(false, m);
		for (String m : commentFailures(raw)) check(false, m);

		// Self-test of the checker against counterexamples built from the REAL source.
		// Base-relative: under an externally mutated file the same assertions must still behave
		// (a "must reject" variant adds failures; a "must ignore" variant changes none).
		List<String> base = wiringFailures(raw);
		// M6: the live add commented out must fail - both comment styles.
		String m6a = raw.replace("s.gndEv.add(claimAir);", "// s.gndEv.add(claimAir);");
		check(wiringFailures(m6a).size() > base.size(), "checker must reject M6a (line-commented live add)");
		String m6b = raw.replace("s.gndEv.add(claimAir);", "/* s.gndEv.add(claimAir); */");
		check(wiringFailures(m6b).size() > base.size(), "checker must reject M6b (block-commented live add)");
		// braces inside comments and strings must not corrupt block extraction
		String bracey = raw.replace("if (ground && !exempt && s.grace <= 0) {",
			"if (ground && !exempt && s.grace <= 0) { // }}}} decoys\n\t\t\tString decoy = \"{{{ }} }\";");
		check(wiringFailures(bracey).equals(base), "checker must ignore braces in comments/strings");
		// a trace marker in a COMMENT (not a literal) must not satisfy the marker pin:
		// removing the real marker literal must add a failure even when a comment decoy stays.
		String noLiteral = raw.replace("[trace] gndreset {} cause=blink-setback count={} decayCount={}", "x");
		check(wiringFailures(noLiteral).size() > base.size(), "checker must reject a clear whose marker literal was removed");
		String fakeMarker = raw.replaceFirst("s\\.gndEv\\.clear\\(\\);", "s.gndEv.clear(); // gndreset");
		check(wiringFailures(fakeMarker).equals(base), "a real literal marker + a comment decoy on one line still passes (literal present)");

		System.out.println(fails == 0 ? "GroundSpoofSourceWitnessTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
