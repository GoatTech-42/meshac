import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/** Bounded doc-drift SMOKE (substring pins), NOT command-tree equivalence: it proves the named literals
 *  and audit-action strings appear in both files, and that documented /mesh tokens exist in the code. It does
 *  NOT parse the brigadier tree, prove argument shapes, or catch a command whose docs use the wrong argument
 *  type beyond the pinned "esp on" case. Run from the repo root. */
public final class MeshCommandsDocSmokeTest {
	static int fails;
	static void check(boolean ok, String m) { if (!ok) { fails++; System.out.println("FAIL " + m); } }

	/** First-level subcommands under the /mesh root (the command tree as of 0072; adding or renaming one must update this set AND docs/ADMIN.md). */
	static final String[] TOP = {"cases", "case", "ban", "kick", "pardon", "unban", "status", "watch", "monitor", "check", "veil", "capture", "reload"};
	/** Nested literals the docs rely on. */
	static final String[] NESTED = {"start", "clear", "dump", "xray", "esp", "off", "on"};
	/** Change actions the docs list; each must be an audit() action string in the code (check.<name> is a dynamic prefix; ban/tempban are created via Cases.add, pinned separately). */
	static final String[] AUDIT_ACTIONS = {"veil.xray", "veil.esp", "veil.off", "monitor", "check.", "reload", "pardon", "unban", "kick", "capture.start", "capture.clear", "capture.dump", "case.view", "status.view"};

	public static void main(String[] a) throws Exception {
		String code = Files.readString(Paths.get("src/main/java/dev/meshac/MeshCommands.java"));
		String doc = Files.readString(Paths.get("docs/ADMIN.md"));

		for (String c : TOP) {
			check(code.contains("Commands.literal(\"" + c + "\")"), "code keeps the /mesh " + c + " literal");
			check(doc.contains("/mesh " + c), "ADMIN.md documents /mesh " + c);
		}
		for (String c : NESTED)
			check(code.contains("Commands.literal(\"" + c + "\")"), "code keeps the nested literal " + c);

		// capture sub-commands documented individually
		for (String row : new String[]{"/mesh capture <player> start", "/mesh capture <player> clear", "/mesh capture <player> dump <page>"})
			check(doc.contains(row), "ADMIN.md documents " + row);

		// every audit action string in the code must be named in the docs' actions line
		Set<String> codeActions = new TreeSet<>();
		Matcher m = Pattern.compile("audit\\([^;]*?\"([a-z.]+)\"").matcher(code);
		while (m.find()) codeActions.add(m.group(1));
		for (String act : codeActions)
			check(act.endsWith(".") ? doc.contains("`" + act + "<name>`") : doc.contains("`" + act + "`"), "ADMIN.md names the logged action " + act);
		for (String act : AUDIT_ACTIONS) check(code.contains("\"" + act + "\""), "code keeps the audit action " + act);
		check(code.contains("minutes == 0 ? \"ban\" : \"tempban\""), "ban/tempban stay case-creating actions (the case id is the log line)");

		// no documented command may be absent from the code: every /mesh <token> the docs name must be a literal in the code
		Matcher dm = Pattern.compile("/mesh ([a-z]+)").matcher(doc);
		Set<String> docCmds = new TreeSet<>();
		while (dm.find()) docCmds.add(dm.group(1));
		Set<String> known = new TreeSet<>(Arrays.asList(TOP)); known.addAll(Arrays.asList(NESTED));
		for (String c : docCmds) check(known.contains(c), "documented /mesh " + c + " exists in the code's command tree");

		// the esp toggle is a boolean; the docs must not show the invalid "esp on" form
		check(!doc.contains("/mesh veil esp on"), "ADMIN.md must not show `/mesh veil esp on` - the argument is <true|false>");

		System.out.println(fails == 0 ? "MeshCommandsDocSmokeTest ok" : fails + " failures");
		if (fails > 0) System.exit(1);
	}
}
