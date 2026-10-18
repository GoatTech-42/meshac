package dev.meshac.veil;

/** Who is always shown to whom, as one pure decision. Veil.exempt gathers the facts from Minecraft and calls this. Every exempt pair is shown at once and never counted towards a cull. */
public final class ExemptRule {
	private ExemptRule() {}
	public static final class F {
		public boolean self, mountedTogether, sharesVehicle, boss, glowing, projectile, leashedToViewer, viewerSpectatorOrCreative, bedrockShowAll, sameTeam, joinGrace, targetIsPlayer, cullMobs;
	}
	public static boolean exempt(F f) {
		if (f.self || f.mountedTogether || f.sharesVehicle) return true;
		if (f.boss || f.glowing || f.projectile || f.leashedToViewer) return true;
		if (f.viewerSpectatorOrCreative || f.bedrockShowAll || f.sameTeam || f.joinGrace) return true;
		if (f.targetIsPlayer) return false;
		return !f.cullMobs; // item frames, armor stands, items, vehicles are not cullable at all, mobs only when cullMobs is on
	}
}
