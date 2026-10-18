import dev.meshac.veil.VeilSettings;
import java.util.*;

public class VeilSettingsTest {
	static int fails;
	static void check(boolean c, String m) { if (!c) { fails++; System.out.println("FAIL " + m); } }
	public static void main(String[] a) {
		VeilSettings d = new VeilSettings(null, null, null);
		check(!d.xray && !d.esp && !d.bedrock, "missing keys mean everything off");
		check(d.describe().equals("veil: xray=off esp=off bedrockViewersCulled=no"), "default line: " + d.describe());
		VeilSettings x = new VeilSettings(true, false, null);
		check(x.xray && !x.esp, "xray only");
		check(new VeilSettings(true, true, true).describe().equals("veil: xray=on esp=on bedrockViewersCulled=yes"), "all on line");
		check(new VeilSettings(false, false, true).describe().contains("bedrockViewersCulled=no"), "bedrock flag alone culls nothing when esp is off");
		check(VeilSettings.typos(List.of("veilXray", "veilEsp", "veilBedrock", "preset", "monitorOnly")).isEmpty(), "valid and unrelated keys give no warning");
		List<String> t = VeilSettings.typos(Arrays.asList("veilESP", "veil_xray", "Veil-Bedrock", "veil esp", null, "veilXray"));
		check(t.size() == 4, "four typos found: " + t);
		check(t.contains("veilESP -> veilEsp") && t.contains("veil_xray -> veilXray") && t.contains("Veil-Bedrock -> veilBedrock") && t.contains("veil esp -> veilEsp"), "suggestions: " + t);
		check(VeilSettings.typos(List.of("veilEspp", "xray")).isEmpty(), "only case and separator slips are guessed, not new names");
		check(VeilSettings.typos(Collections.emptyList()).isEmpty(), "empty file");
		// veilEnabled master switch
		check(new VeilSettings(null, true, true, true).describe().equals("veil: xray=on esp=on bedrockViewersCulled=yes"), "no veilEnabled key: master on, line unchanged");
		check(new VeilSettings(true, true, true, false).xray, "master on keeps xray");
		VeilSettings off = new VeilSettings(false, true, true, true);
		check(!off.enabled && !off.xray && !off.esp, "master off forces xray and esp off");
		check(off.describe().startsWith("veil: xray=off esp=off bedrockViewersCulled=no") && off.describe().contains("veilEnabled is false"), "master off line says why: " + off.describe());
		check(new VeilSettings(false, false, false, false).describe().contains("veilEnabled is false"), "master off with nothing else set still says so");
		if (fails > 0) { System.out.println(fails + " failures"); System.exit(1); }
		System.out.println("VeilSettingsTest ok");
	}
}
