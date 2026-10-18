import dev.meshac.CheckNames;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

/** javac -d /tmp/t src/main/java/dev/meshac/CheckNames.java sim/CheckNamesTest.java && java -cp /tmp/t CheckNamesTest   (run from the repo root) */
public final class CheckNamesTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) throws Exception {
		check(CheckNames.known("fly") && CheckNames.known("FLY") && CheckNames.known("Mining-LookRay"), "case-insensitive known");
		check(!CheckNames.known("flyy") && !CheckNames.known("") && !CheckNames.known(null), "typos and null are unknown");
		check(CheckNames.known("noclip") && CheckNames.known("glide") && CheckNames.known("boatfly"), "top-level names present");
		// every name the code can emit must be listed, or /mesh check refuses it
		Path src = Paths.get("src/main/java");
		Set<String> emitted = new TreeSet<>();
		try (Stream<Path> w = Files.walk(src)) {
			for (Path p : w.filter(x -> x.toString().endsWith(".java")).collect(Collectors.toList())) {
				String t = Files.readString(p);
				Matcher m = Pattern.compile("Verdict\\.signal\\(\\s*pl\\s*,\\s*\"([a-z-]+)\"").matcher(t); while (m.find()) emitted.add(m.group(1));
				m = Pattern.compile("(?:disabled|enforce|skips\\(pl,)\\s*\\(?\\s*\"([a-z-]+)\"").matcher(t); while (m.find()) emitted.add(m.group(1));
				if (p.getFileName().toString().equals("Movement.java")) { // movement sub-checks: first word of the hit message
					m = Pattern.compile("hit = (?:String\\.format\\()?\"([a-z]+)[ \"]").matcher(t); while (m.find()) emitted.add(m.group(1));
				}
			}
		}
		emitted.remove("none");
		System.out.println("emitted names: " + emitted);
		check(emitted.size() >= 15, "scan found names (got " + emitted.size() + ")");
		for (String e : emitted) check(CheckNames.known(e), "emitted check name missing from CheckNames.ALL: " + e);
		if (fails > 0) { System.out.println(fails + " failure(s)"); System.exit(1); }
		System.out.println("OK: CheckNames");
	}
}
