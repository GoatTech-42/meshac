package dev.meshac;

/** Block cells a body box overlaps, the same overlap vanilla uses for entityInside (box deflated by 1e-5). Takes the real box, so crouching, swimming, crawling and scaled bodies are what they are. Pure so tests can drive it. */
public final class MediumFootprint {
	private MediumFootprint() {}
	private static final double EPS = 1.0E-5;
	/** {x0, x1, y0, y1, z0, z1}, inclusive. */
	public static int[] cells(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
		return new int[] {
			(int) Math.floor(minX + EPS), (int) Math.floor(maxX - EPS),
			(int) Math.floor(minY + EPS), (int) Math.floor(maxY - EPS),
			(int) Math.floor(minZ + EPS), (int) Math.floor(maxZ - EPS) };
	}
}
