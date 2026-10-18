import dev.meshac.veil.*;

/** Offline harness for the runtime gate: every request path, with healthy and broken parameters. No server. */
public class EspSwitchTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		EspCore.Params good = new EspCore.Params();
		String ok = EspSwitch.check(true, good);
		check(ok == null, "healthy params pass: " + ok);
		check(EspSwitch.resulting(true, ok), "on request on healthy");
		check(EspSwitch.audit(true, ok).equals("on") && EspSwitch.message(true, ok).contains("self-check passed"), "on wording");
		EspCore.Params[] broken = new EspCore.Params[4];
		for (int i = 0; i < broken.length; i++) broken[i] = new EspCore.Params();
		broken[0].nearRadius = 0; broken[1].cullAfterChecks = 100000; broken[2].showAfterChecks = 100000; broken[3].recentCombatTicks = 0;
		int refused = 0;
		for (int i = 0; i < broken.length; i++) {
			String bad = EspSwitch.check(true, broken[i]);
			if (bad == null) { check(false, "param set " + i + " is not caught by the check"); continue; }
			refused++;
			check(!EspSwitch.resulting(true, bad), "broken set " + i + " stays off");
			check(EspSwitch.audit(true, bad).contains("refused") && EspSwitch.message(true, bad).contains(bad), "refusal wording " + i);
		}
		check(refused == 4, "all four behaviour-breaking settings are refused, got " + refused);
		// off never runs the check and always works, even with broken params
		check(EspSwitch.check(false, broken[0]) == null, "off skips the check");
		check(!EspSwitch.resulting(false, null) && EspSwitch.audit(false, null).equals("off") && EspSwitch.message(false, null).startsWith("Anti-ESP off"), "off wording");
		check(!EspSwitch.resulting(false, "whatever"), "off stays off whatever the reason");
		// repeated on/off cycles leave no state behind
		for (int i = 0; i < 1000; i++) { check(EspSwitch.check(true, good) == null, "cycle " + i); if (fails > 0) break; }
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("EspSwitchTest ok");
	}
}
