import dev.meshac.SeenNames;
import java.nio.file.*;
import java.util.*;

public class SeenNamesTest {
	static int fails = 0;
	static void check(boolean ok, String what) { if (!ok) { fails++; System.out.println("FAIL: " + what); } }
	public static void main(String[] a) throws Exception {
		Path d = Files.createTempDirectory("seen");
		Path uc = d.resolve("usercache.json");
		String u1 = "11111111-2222-3333-4444-555555555555", u2 = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee";
		Files.writeString(uc, "[{\"name\":\"Steve\",\"uuid\":\"" + u1 + "\",\"expiresOn\":\"2026-12-01 00:00:00 -0700\"},{\"name\":\"Alex\",\"uuid\":\"" + u2 + "\",\"expiresOn\":\"x\"}]");
		String before = Files.readString(uc);
		List<SeenNames.Past> none = List.of();
		check(SeenNames.find(uc, none, "Steve").id().toString().equals(u1), "cache hit");
		check(SeenNames.find(uc, none, "sTEVE").name().equals("Steve"), "case-insensitive, real casing returned");
		check(SeenNames.find(uc, none, "NeverSeenReviewX") == null, "never seen -> null");
		check(Files.readString(uc).equals(before), "usercache.json untouched");
		check(SeenNames.find(d.resolve("missing.json"), none, "Steve") == null, "missing cache, no history -> null");
		Files.createFile(d.resolve("empty.json")); check(SeenNames.find(d.resolve("empty.json"), none, "Steve") == null, "empty file -> null");
		Files.writeString(d.resolve("bad.json"), "{{not json"); check(SeenNames.find(d.resolve("bad.json"), none, "Steve") == null, "garbage -> null, no throw");
		List<SeenNames.Past> hist = List.of(new SeenNames.Past("Gone", "00000000-0000-0000-0000-000000000001"), new SeenNames.Past("Gone", u2), new SeenNames.Past("Bad", "nope"));
		check(SeenNames.find(uc, hist, "gone").id().toString().equals(u2), "history fallback uses newest case");
		check(SeenNames.find(uc, hist, "Bad") == null, "unparseable history uuid ignored");
		check(SeenNames.find(uc, hist, "Steve").id().toString().equals(u1), "cache wins over history");
		check(SeenNames.find(uc, none, " ") == null && SeenNames.find(uc, none, null) == null, "blank/null");
		Files.writeString(d.resolve("nodash.json"), "[{\"name\":\"Nd\",\"uuid\":\"11111111222233334444555555555555\"}]");
		check(SeenNames.find(d.resolve("nodash.json"), none, "Nd").id().toString().equals(u1.substring(0, 24) + "555555555555"), "dashless uuid");
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("OK: SeenNames");
	}
}
