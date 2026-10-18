package dev.meshac;

/**
 * Pure formatter for the persistent staff-command audit line. Commands that change what meshac enforces (veil toggles, monitor mode, check switches, reload,
 * bans, pardons) were only answered to the staff member (and to ops through command feedback, which a gamerule can silence). This line goes to the server log.
 * Rules: one line per change, no player-controlled text can forge a second line or a fake field, bounded length.
 */
public final class AdminLog {
	private AdminLog() {}
	public static final int MAX_FIELD = 120;

	/** Strip control characters and line breaks, collapse spaces, replace '=' (field separator) and cap the length. */
	public static String clean(String s) {
		if (s == null) return "-";
		StringBuilder b = new StringBuilder();
		boolean sp = false;
		for (int i = 0; i < s.length() && b.length() < MAX_FIELD; i++) {
			char c = s.charAt(i);
			if (c == '=') c = ':';
			if (Character.isISOControl(c) || Character.isWhitespace(c) || c == '\u2028' || c == '\u2029' || c == '\u0085') { if (!sp && b.length() > 0) b.append(' '); sp = true; continue; }
			sp = false; b.append(c);
		}
		String r = b.toString().trim();
		return r.isEmpty() ? "-" : r;
	}

	/** actor is the command source name (console, a player name); action a fixed key like veil.xray; detail the argument or result. */
	public static String line(String actor, String action, String detail) {
		return "[meshac-admin] actor=" + clean(actor) + " action=" + clean(action) + " detail=" + clean(detail);
	}

	/** "was -> now" for a change of state, so the log shows what a command replaced. */
	public static String change(String was, String now) { return clean(was) + " -> " + clean(now); }

 public enum ReloadResult {
  APPLIED("config read and applied", "meshac reloaded.", true),
  DEFAULTS_CREATED("config absent, current settings active and defaults file created", "meshac reloaded; defaults file created.", true),
  PARSE_FAILED("config NOT read, previous settings kept", "meshac config read/parse failed; previous settings kept. See the log.", false),
  APPLIED_NOT_SAVED("config applied, persistence FAILED; active settings kept but file not saved", "meshac settings applied, but config file could not be saved. See the log.", true);
  private final String detail, feedback; private final boolean applied;
  ReloadResult(String detail, String feedback, boolean applied) { this.detail=detail; this.feedback=feedback; this.applied=applied; }
  public String detail() { return detail; }
  public String feedback() { return feedback; }
  public boolean applied() { return applied; }
 }
 /** Result of /mesh reload: distinguish active configuration from persistence and parse failure. */
 public static String reloadDetail(ReloadResult result, boolean monitor, int disabledChecks, String veilState) {
  return result.detail() + ", monitor " + (monitor ? "on" : "off") + ", " + disabledChecks + " checks disabled, " + clean(veilState);
 }
 /** Compatibility formatter for older callers/tests; runtime reload uses the richer result above. */
 public static String reloadDetail(boolean read, boolean monitor, int disabledChecks, String veilState) {
  return (read ? "config read" : "config NOT read, previous settings kept") + ", monitor " + (monitor ? "on" : "off") + ", " + disabledChecks + " checks disabled, " + clean(veilState);
 }
}
