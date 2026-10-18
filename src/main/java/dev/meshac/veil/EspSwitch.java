package dev.meshac.veil;

/** Decision and wording for /mesh veil esp on|off, pure so the runtime gate can be tested offline. */
public final class EspSwitch {
	private EspSwitch() {}

	/** Final state after a request. A refused "on" stays off; "off" always works. */
	public static boolean resulting(boolean want, String selfCheckFailure) { return want && selfCheckFailure == null; }

	public static String audit(boolean want, String selfCheckFailure) {
		if (want && selfCheckFailure != null) return selfCheckFailure.startsWith("veilEnabled") ? "on refused, master switch off" : "on refused, self-check failed";
		return want ? "on" : "off";
	}

	public static String message(boolean want, String selfCheckFailure) {
		if (want && selfCheckFailure != null) return selfCheckFailure.startsWith("veilEnabled") ? "Anti-ESP stays off: " + selfCheckFailure + "." : "Anti-ESP stays off, the self-check failed: " + selfCheckFailure + ". See the log.";
		return "Anti-ESP " + (want ? "on (self-check passed)" : "off") + " until reload or restart (config veilEsp is unchanged).";
	}

	/** Runs the check the way the command does. The params argument lets tests inject a broken setting. */
	public static String check(boolean want, EspCore.Params params) { return want ? VeilStats.selfCheck(params) : null; }
}
