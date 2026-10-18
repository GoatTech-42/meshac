package dev.meshac;

/** HoneyBlock.isSlidingDown without the velocity term (javap of 26.2): not on the ground, at or below the block's 0.9375 line, and the body centre at least 0.4375 + width/2 from the block centre on x or z (so the body is pressed against the side, not above it). The velocity test (falling faster than 0.08) needs the server velocity, which a packet does not give, so it is left out: slightly permissive. Pure. */
public final class HoneyRule {
	private HoneyRule() {}
	public static boolean sliding(boolean onGround, double entityY, int cellX, int cellY, int cellZ, double entityX, double entityZ, double width) {
		if (onGround) return false;
		if (entityY > cellY + 0.9375 - 1.0E-7) return false;
		double reach = 0.4375 + width / 2.0;
		return Math.abs(cellX + 0.5 - entityX) + 1.0E-7 > reach || Math.abs(cellZ + 0.5 - entityZ) + 1.0E-7 > reach;
	}
}
