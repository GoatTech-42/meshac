/**
 * Analysis model of the sneak-edge clamp and of the meshac ground-spoof support strip. Used only by sim tests; src/ never calls it.
 * The clamp mirrors, loop for loop, vanilla 26.2 Player#maybeBackOffFromEdge (decompiled MCP, Renekovski/26.2-mcp):
 * three while loops (x, then z, then both) stepping 0.05, stopping at the last offset whose probe still finds support
 * (canFallAtLeast: the box inset 1.0E-7 on x/z, probed down to minY - maxUpStep). The accepted position therefore always
 * has support, with overlap in (0, 0.05]. Boar (opencollab-incubator/Boar, Collider.java), the public Bedrock-movement
 * emulator for Geyser, runs the same three loops with the same 0.05 step and a 0.025-inset probe box - the available
 * evidence that Bedrock's own clamp has the same shape with an even wider margin. The real Bedrock client is closed
 * source, so that last part stays a rig question.
 */
public final class SneakEdge {
	public static final double STEP = 0.05, INSET = 1.0E-7, BOAR_INSET = 0.025, HALF_WIDTH = 0.3;

	private SneakEdge() {}

	/** True when moving by (dx, dz) would leave the inset box without support (vanilla canFallAtLeast). */
	public interface Support { boolean canFall(double dx, double dz); }

	/** The vanilla three-loop clamp, structure-verbatim. Returns the allowed {dx, dz}. */
	public static double[] backOff(double dx, double dz, Support s) {
		double stepX = Math.signum(dx) * STEP;
		double stepZ = Math.signum(dz) * STEP;
		while (dx != 0.0 && s.canFall(dx, 0.0)) { if (Math.abs(dx) <= STEP) { dx = 0.0; break; } dx -= stepX; }
		while (dz != 0.0 && s.canFall(0.0, dz)) { if (Math.abs(dz) <= STEP) { dz = 0.0; break; } dz -= stepZ; }
		while (dx != 0.0 && dz != 0.0 && s.canFall(dx, dz)) {
			if (Math.abs(dx) <= STEP) dx = 0.0; else dx -= stepX;
			if (Math.abs(dz) <= STEP) dz = 0.0; else dz -= stepZ;
		}
		return new double[] {dx, dz};
	}

	/** XZ overlap of a player box (center px,pz, the given inset applied) with a block footprint [x0,x1]x[z0,z1]; 0 when clear. */
	public static double overlap(double px, double pz, double inset, double x0, double x1, double z0, double z1) {
		double ox = Math.min(px + HALF_WIDTH - inset, x1) - Math.max(px - HALF_WIDTH + inset, x0);
		double oz = Math.min(pz + HALF_WIDTH - inset, z1) - Math.max(pz - HALF_WIDTH + inset, z0);
		return (ox > 0 && oz > 0) ? Math.min(ox, oz) : 0;
	}

	/** float32 ulp at a coordinate magnitude (Geyser relays Bedrock positions as floats). */
	public static double ulp32(double x) {
		if (x == 0) return 0;
		int e = (int) Math.floor(Math.log(Math.abs(x)) / Math.log(2));
		return Math.pow(2, e - 23);
	}
}
