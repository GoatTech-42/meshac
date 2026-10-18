package dev.meshac.veil;

import java.util.*;

/** Reads the veil keys of meshac.json without Minecraft, so a typo is caught and reported instead of silently leaving the veil off. */
public final class VeilSettings {
	public static final List<String> KEYS = List.of("veilXray", "veilEsp", "veilBedrock", "veilEnabled");
	/** enabled is the veilEnabled master switch. xray and esp are what is really in force: off whenever the master switch is off, whatever veilXray and veilEsp say. */
	public final boolean enabled, xray, esp, bedrock;
	public VeilSettings(Boolean xray, Boolean esp, Boolean bedrock) { this(null, xray, esp, bedrock); }
	public VeilSettings(Boolean master, Boolean xray, Boolean esp, Boolean bedrock) { this.enabled = master == null || master; this.xray = enabled && xray != null && xray; this.esp = enabled && esp != null && esp; this.bedrock = bedrock != null && bedrock; }

	/** Keys in the file that look like a veil key but are not one, e.g. "veilESP" or "veil_esp". Each entry reads "found -> expected". */
	public static List<String> typos(Collection<String> fileKeys) {
		List<String> out = new ArrayList<>();
		for (String k : fileKeys) {
			if (k == null || KEYS.contains(k)) continue;
			String n = k.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "").replace(" ", "");
			for (String want : KEYS) if (n.equals(want.toLowerCase(Locale.ROOT))) { out.add(k + " -> " + want); break; }
		}
		return out;
	}

	/** One line for the log at start and reload. */
	public String describe() { return "veil: xray=" + (xray ? "on" : "off") + " esp=" + (esp ? "on" : "off") + " bedrockViewersCulled=" + (bedrock && esp ? "yes" : "no") + (enabled ? "" : " (veilEnabled is false: the veil is off whatever veilXray and veilEsp say)"); }
}
