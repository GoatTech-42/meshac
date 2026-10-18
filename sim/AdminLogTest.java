import dev.meshac.AdminLog;

/** Admin-tool command logging: one line, unforgeable fields, bounded. */
public class AdminLogTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		check(AdminLog.line("Luke_42", "veil.xray", "off").equals("[meshac-admin] actor=Luke_42 action=veil.xray detail=off"), "plain line");
		String forged = AdminLog.line("Eve", "ban", "x\n[meshac-admin] actor=Console action=pardon detail=all");
		check(forged.indexOf('\n') < 0 && forged.indexOf('\r') < 0, "no line break survives");
		check(forged.split(" actor=").length == 2, "player text cannot add an actor field: " + forged);
		check(AdminLog.line("a=b", "k", "v").contains("actor=a:b"), "equals sign replaced");
		check(AdminLog.line("x", "k", "\u0000\u001b[31mred\u2028next").indexOf('\u001b') < 0, "escape and control characters dropped");
		check(AdminLog.line("x", "k", "\u2028\u2029\u0085").endsWith("detail=-"), "only separators gives -");
		check(AdminLog.clean(null).equals("-") && AdminLog.clean("   ").equals("-"), "null and blank");
		check(AdminLog.clean("a    b\t\tc").equals("a b c"), "whitespace collapsed");
		check(AdminLog.clean("x".repeat(5000)).length() == AdminLog.MAX_FIELD, "length capped");
		check(AdminLog.line("console", "reload", "ok").startsWith("[meshac-admin] "), "stable prefix for grep");
		check(AdminLog.change("off", "on").equals("off -> on"), "change format");
		check(AdminLog.change("a=b", "x\ny").equals("a:b -> x y"), "change cleans both sides: " + AdminLog.change("a=b", "x\ny"));
		String rd = AdminLog.reloadDetail(false, true, 2, "veil: xray=off esp=off bedrockViewersCulled=no");
		check(rd.contains("NOT read") && rd.contains("monitor on") && rd.contains("2 checks disabled") && rd.contains("xray:off esp:off"), "reload detail failed read: " + rd);
		check(AdminLog.reloadDetail(true, false, 0, "v").startsWith("config read, monitor off, 0 checks"), "reload detail ok");
		check(AdminLog.line("c", "reload", rd).lines().count() == 1, "reload line stays one line");
  String persistence = AdminLog.reloadDetail(AdminLog.ReloadResult.APPLIED_NOT_SAVED, false, 0, "v");
  check(persistence.contains("config applied") && persistence.contains("persistence FAILED") && !persistence.contains("previous settings kept"), "persistence state is not parse failure");
  check(AdminLog.ReloadResult.APPLIED_NOT_SAVED.applied(), "persistence failure retains new settings");
  check(!AdminLog.ReloadResult.PARSE_FAILED.applied(), "parse failure does not apply");
  check(AdminLog.ReloadResult.DEFAULTS_CREATED.applied(), "absent-file branch reports active settings");
  check(!AdminLog.ReloadResult.PARSE_FAILED.feedback().equals(AdminLog.ReloadResult.APPLIED_NOT_SAVED.feedback()), "distinct command feedback");
		if (fails > 0) { System.out.println(fails + " FAIL"); System.exit(1); }
		System.out.println("AdminLogTest ok");
	}
}
