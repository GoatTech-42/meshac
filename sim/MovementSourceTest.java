import java.nio.file.*;

/** Source guards for rules the pure classes cannot cover. Run from the repo root. */
public final class MovementSourceTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) throws Exception {
		String t = Files.readString(Paths.get("src/main/java/dev/meshac/Movement.java"));
		check(t.contains("TRACE = System.getenv(\"MESHAC_TRACE\") != null"), "TRACE must stay env-gated: a production jar must not log every packet");
		int i = t.indexOf("boolean baseExempt");
		check(i > 0 && t.substring(i, i + 600).contains("isAutoSpinAttack"), "riptide (Java trident spin) must be exempt like elytra: it flings the player at 20+ bps");
		check(t.contains("SustainBudget.step(") && !t.contains("s.spBal + h - sustain"), "budget arithmetic belongs in SustainBudget so the tests cover the real code");
		check(t.contains("BlinkOrigin") && !t.contains("s.haveB"), "blink origin freshness belongs in BlinkOrigin");
		System.out.println(fails == 0 ? "MovementSourceTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
