package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.phys.AABB;

/** Movement checks: speed, fly (hover/ascend), high jump. Runs on every position packet, before vanilla. */
public final class Movement {
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int FLAG_AT = 6;      // buffered violations before a setback
	private static final int SKIP_TICKS = 20;  // grace after teleport, damage, effects

	private static final class S {
		double px, py, pz, cx, cy, cz, dy, goodX, goodY, goodZ;
		boolean ground, cground, init;
		long lastNs; final java.util.concurrent.ConcurrentLinkedQueue<Long> arrivals = new java.util.concurrent.ConcurrentLinkedQueue<>(); double balMs; int bufGround, grace, clean, bufSpeed, bufFly, bufJump;
	}

	/** Netty thread: stamp when a position packet really arrived. The main thread only sees it at the next tick. */
	public static void arrive(ServerPlayer pl, ServerboundMovePlayerPacket p) {
		if (p.hasPosition()) STATE.computeIfAbsent(pl.getUUID(), k -> new S()).arrivals.add(System.nanoTime());
	}

	/** Returns a setback position, or null if the packet is fine. */
	public static double[] check(ServerPlayer pl, ServerboundMovePlayerPacket p) {
		if (!p.hasPosition()) return null;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		double x = p.getX(pl.getX()), y = p.getY(pl.getY()), z = p.getZ(pl.getZ());
		boolean ground = p.isOnGround();
		Long arrived = s.arrivals.poll(); // pairs 1:1 with the stamp from arrive()
		// The server position is the last accepted one (vanilla has already handled the previous packet).
		// If it is neither where the previous packet started nor where it claimed, the server moved the player.
		double sx = pl.getX(), sy = pl.getY(), sz = pl.getZ();
		boolean atStart = near(sx, sy, sz, s.px, s.py, s.pz), atClaim = near(sx, sy, sz, s.cx, s.cy, s.cz);
		if (!s.init || !(atStart || atClaim)) {
			s.init = true; s.grace = SKIP_TICKS; s.dy = 0; s.ground = ground; s.bufSpeed = s.bufFly = s.bufJump = 0;
			s.goodX = sx; s.goodY = sy; s.goodZ = sz;
			s.px = sx; s.py = sy; s.pz = sz; s.cx = x; s.cy = y; s.cz = z; s.cground = ground;
			return null;
		}
		if (atClaim) { s.dy = s.cy - s.py; s.ground = s.cground; }
		double dx = x - sx, dy = y - sy, dz = z - sz;
		boolean exempt = pl.isCreative() || pl.isSpectator() || pl.getAbilities().mayfly || pl.isPassenger()
			|| pl.isFallFlying() || pl.isInWater() || pl.isInLava() || pl.onClimbable() || pl.hurtTime > 0
			|| pl.hasEffect(MobEffects.LEVITATION) || pl.hasEffect(MobEffects.SLOW_FALLING);
		if (pl.hurtTime > 0 || pl.hasEffect(MobEffects.LEVITATION)) s.grace = SKIP_TICKS;
		String hit = null;
		if (s.grace > 0 || exempt) {
			if (s.grace > 0) s.grace--;
		} else {
			// Speed: horizontal blocks per packet vs a generous cap scaled by the speed attribute.
			double cap = 0.75 * (pl.getSpeed() / 0.1);
			double h = Math.hypot(dx, dz);
			if (h > cap) { s.bufSpeed++; s.clean = 0; } else if (++s.clean >= 10) { s.bufSpeed = Math.max(0, s.bufSpeed - 1); s.clean = 0; }
			if (s.bufSpeed >= FLAG_AT) hit = String.format("speed %.2f>%.2f", h, cap);
			// Fly: in the air, vanilla gravity gives dy = (lastDy - 0.08) * 0.98. Going above that is not vanilla.
			if (!s.ground && !ground) {
				double expect = (s.dy - 0.08) * 0.98;
				s.bufFly = dy > expect + 0.03 ? s.bufFly + 1 : Math.max(0, s.bufFly - 1);
				if (s.bufFly >= FLAG_AT && hit == null) hit = String.format("fly dy %.3f expect %.3f", dy, expect);
			} else s.bufFly = 0;
			// High jump / step: leaving the ground higher than a jump (0.42 + jump boost) or a step (0.6).
			if (s.ground && dy > 0.62 + 0.1 * (pl.hasEffect(MobEffects.JUMP_BOOST) ? 3 : 0)) {
				s.bufJump++;
				if (s.bufJump >= 2 && hit == null) hit = String.format("jump dy %.3f", dy);
			} else s.bufJump = Math.max(0, s.bufJump - 1);
		}
		// Timer: each move packet is worth 50 ms. Packets running ahead of the real clock = game speed hack.
		long now = arrived != null ? arrived : System.nanoTime();
		if (s.lastNs != 0) {
			// Teleports make the client send extra confirm packets, so no timing while in grace.
			s.balMs = s.grace > 0 ? 0 : s.balMs + 50 - (now - s.lastNs) / 1e6;
			s.balMs = Math.max(-300, Math.min(s.balMs, 600)); // lag may bank up to 300 ms of catch-up
			if (s.balMs > 450 && hit == null) hit = String.format("timer ahead %.0f ms", s.balMs);
		}
		s.lastNs = now;
		// NoFall / ground spoof: claims to stand on something with only air below.
		if (ground && !exempt && s.grace <= 0) {
			AABB b = pl.getBoundingBox().move(dx, dy, dz);
			boolean air = pl.level().noCollision(pl, new AABB(b.minX, b.minY - 0.1, b.minZ, b.maxX, b.minY, b.maxZ));
			s.bufGround = air ? s.bufGround + 1 : 0;
			if (s.bufGround >= 3 && hit == null) hit = "nofall ground spoof";
		} else s.bufGround = 0;
		if (hit != null) {
			Meshac.LOG.warn("[meshac] FLAG {} {}", pl.getGameProfile().name(), hit);
			s.bufSpeed = s.bufFly = s.bufJump = s.bufGround = 0; s.balMs = 0; s.grace = 5; s.dy = 0;
			s.px = s.cx = s.goodX; s.py = s.cy = s.goodY; s.pz = s.cz = s.goodZ;
			return new double[] {s.goodX, s.goodY, s.goodZ};
		}
		if (s.bufSpeed == 0 && s.bufFly == 0 && s.bufJump == 0) { s.goodX = x; s.goodY = y; s.goodZ = z; }
		s.px = sx; s.py = sy; s.pz = sz; s.cx = x; s.cy = y; s.cz = z; s.cground = ground;
		return null;
	}

	private static boolean near(double ax, double ay, double az, double bx, double by, double bz) {
		return Math.abs(ax - bx) < 0.01 && Math.abs(ay - by) < 0.01 && Math.abs(az - bz) < 0.01;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
