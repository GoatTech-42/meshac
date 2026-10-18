package dev.meshac.veil;

/** Who the per-viewer veil applies to. Pure, so it can be tested without Minecraft. */
public final class ViewerPolicy {
	private ViewerPolicy() {}

	/** The zero-high-half UUID rule only (unlinked Bedrock players). The server uses dev.meshac.BedrockIds, which adds Floodgate's answer for linked accounts, and passes its result to showAll(boolean, boolean). The name prefix is never used, a Java client can fake it. */
	public static boolean bedrock(long uuidMostSignificantBits) { return uuidMostSignificantBits == 0L; }

	/** True when anti-ESP must show every entity to this viewer. Bedrock viewers see everything unless veilBedrock is on: Geyser rebuilds entity state from the Java packets, and a cull that is wrong there is a vanished player nobody can explain. */
	public static boolean showAll(long uuidMostSignificantBits, boolean veilBedrock) { return showAll(bedrock(uuidMostSignificantBits), veilBedrock); }
	/** Same, with the Bedrock answer already worked out by BedrockIds (zero-high UUID or Floodgate says so). */
	public static boolean showAll(boolean isBedrock, boolean veilBedrock) { return isBedrock && !veilBedrock; }
}
