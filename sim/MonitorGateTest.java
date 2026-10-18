import dev.meshac.MonitorGate;

public final class MonitorGateTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		long t = 10000;
		// hold: enforced and running replays; monitor (not enforced) never replays, even a hold armed before the toggle
		check(MonitorGate.holdReplays(t, t + 1000, true), "running enforced hold replays");
		check(!MonitorGate.holdReplays(t, t + 1000, false), "monitor: a hold armed earlier does not replay");
		check(!MonitorGate.holdReplays(t + 2000, t + 1000, true), "expired hold does not replay");
		// nofall: arm only when enforced
		check(MonitorGate.armNoFall(true) && !MonitorGate.armNoFall(false), "nofall arms only when enforced");
		check(MonitorGate.processNoFall(5, true) && !MonitorGate.processNoFall(5, false) && !MonitorGate.processNoFall(0, true), "pending nofall processes only when enforced and armed");
		// damage: a 5 block drop with no vanilla count hurts only when enforced; vanilla already counted (fdMax>=3) never hurts
		check(MonitorGate.damage(true, 5, 0), "enforced 5 block hidden drop applies damage");
		check(!MonitorGate.damage(false, 5, 0), "monitor: no causeFallDamage");
		check(!MonitorGate.damage(true, 5, 4), "vanilla already counted the fall");
		check(!MonitorGate.damage(true, 2, 0), "small drop is not punished");
		// replay: enforced -> arm -> monitor toggled on -> tick: nothing survives
		boolean enforced = true; long nfAt = 0, hold = 0; double fd = 0;
		if (MonitorGate.armNoFall(enforced)) nfAt = t; hold = t + 1500;
		enforced = false; // /mesh monitor on: clearPendingActions() + gates
		nfAt = 0; hold = 0; fd = 0; // clearPendingActions
		check(!MonitorGate.processNoFall(nfAt, enforced) && !MonitorGate.holdReplays(t + 100, hold, enforced) && !MonitorGate.damage(enforced, 8, fd), "after monitor on: no setback, no damage, no pending action");
		// egregious listing keeps enforcement for that check only
		// absolute invariant: a check listed in egregiousChecks changes nothing in monitor mode
		check(!MonitorGate.acts(true, true) && !MonitorGate.acts(true, false), "monitor mode: nothing acts, egregious or not");
		check(MonitorGate.acts(false, false) && MonitorGate.acts(false, true), "outside monitor mode the gate is open");
		boolean act = MonitorGate.acts(true, true);
		check(!MonitorGate.holdReplays(t, t + 1000, act) && !MonitorGate.armNoFall(act) && !MonitorGate.processNoFall(5, act) && !MonitorGate.damage(act, 9, 0), "listed egregious check in monitor mode: no hold, no arming, no damage");
		if (fails > 0) { System.out.println(fails + " failed"); System.exit(1); }
		System.out.println("MonitorGateTest ok");
	}
}
