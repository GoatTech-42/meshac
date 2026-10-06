package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Scaffold, AirPlace, AutoBuild, Throw: using blocks and items faster, or somewhere else, than a hand does. */
public final class Interact {
	private static final class S { long windowAt; int uses; long lastUse; int burst; double lx, lz, chainDist; long lt, chainMs; int fwd; float pitch, prevPitch, yaw, prevYaw; long snapWin; int snaps; long flickAt, shotWin; int shots; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MAX_PER_SECOND = 16; // fast bridging is about 8 a second

	/** Every move packet that carries a rotation. */
	public static void look(ServerPlayer pl, float yaw, float pitch) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		s.prevPitch = s.pitch; s.prevYaw = s.yaw; s.pitch = pitch; s.yaw = yaw;
		if (Math.max(Math.abs(pitch - s.prevPitch), Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - s.prevYaw))) > 25) s.flickAt = System.currentTimeMillis();
	}

	/** A bow or crossbow let go. Aimbots swing onto the target in one packet while the bow is drawn. */
	public static void release(ServerPlayer pl) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.shotWin > 30000) { s.shotWin = now; s.shots = 0; }
		if (now - s.flickAt < 1500 && ++s.shots >= 3) { s.shots = 0; Verdict.signal(pl, "interact", "view snaps onto a target as the bow is let go", 1); }
	}

	/** A use-item packet (throw, eat, bow). */
	public static void use(ServerPlayer pl) { rate(pl); }

	/** A use-item-on-block packet (placing). */
	public static void place(ServerPlayer pl, ServerboundUseItemOnPacket p) {
		if (pl.isCreative() || pl.isSpectator()) return;
		AABB box = new AABB(p.getHitResult().getBlockPos()).inflate(0.3);
		Vec3 eye = pl.getEyePosition();
		if (box.clip(eye, eye.add(pl.getLookAngle().scale(8))).isEmpty()) Verdict.signal(pl, "interact", "clicked a block it is not looking at", 1);
		// Scaffold: the view jumps to the block just before each click. A hand turns over many packets.
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.snapWin > 5000) { s.snapWin = now; s.snaps = 0; }
		float jump = Math.max(Math.abs(s.pitch - s.prevPitch), Math.abs(net.minecraft.util.Mth.wrapDegrees(s.yaw - s.prevYaw)));
		if (jump > 35 && ++s.snaps >= 3) { s.snaps = 0; Verdict.signal(pl, "interact", "view snaps to the block before placing", 1); }
		// Scaffold walks over air at full walking speed with the view pinned down, laying a block under each step. Even elite speed-bridgers crouch at the edge and stay under 4 blocks a second.
		double dist = Math.hypot(pl.getX() - s.lx, pl.getZ() - s.lz); long dt = now - s.lt;
		boolean step = p.getHitResult().getBlockPos().getY() < pl.getY() && pl.getXRot() > 70 && dist > 0.3 && dt < 700;
		if (step) { s.fwd++; s.chainDist += dist; s.chainMs += dt; } else { s.fwd = 0; s.chainDist = 0; s.chainMs = 0; }
		s.lx = pl.getX(); s.lz = pl.getZ(); s.lt = now;
		if (s.fwd >= 10) {
			double speed = s.chainDist / (s.chainMs / 1000.0);
			if (speed > 4.0) Verdict.signal(pl, "interact", String.format("bridging at %.1f blocks a second", speed), 1);
			s.fwd = 0; s.chainDist = 0; s.chainMs = 0;
		}
		rate(pl);
	}

	private static void rate(ServerPlayer pl) {
		if (pl.isCreative() || pl.isSpectator()) return;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.windowAt > 1000) { s.windowAt = now; s.uses = 0; }
		if (now - s.lastUse < 20) s.burst++; else s.burst = 0; // several uses inside one tick
		s.lastUse = now;
		if (s.burst >= 3) { s.burst = 0; Verdict.signal(pl, "interact", "several uses in one tick", 1); }
		else if (++s.uses > MAX_PER_SECOND) { s.uses = 0; Verdict.signal(pl, "interact", "more than " + MAX_PER_SECOND + " uses in a second", 1); }
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
