package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Nuker, Kaboom, FastBreak and friends: breaking blocks no hand could reach, see or break that fast. */
public final class Mining {
	private static final class S { long windowAt, lastBreak, missWin; int misses; int breaks; long lastAt, turnWin; int turns; Vec3 lastDir; BlockPos dig; int digTick; boolean early; long digSample; long off; boolean lookOff; Vec3 look = null; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MAX_PER_SECOND = 8; // a hasted, efficiency V player on soft blocks tops out near 5

	/** A hand mines by holding the button with the crosshair on the block; the client drops the dig the moment the crosshair leaves. PacketMine keeps the dig alive while the view is elsewhere and turns to the block only for the final packet. Time spent aiming away during a dig is counted. */
	public static void digStart(ServerPlayer pl, BlockPos pos) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		s.dig = pos; s.digTick = pl.tickCount; s.early = false; s.digSample = System.currentTimeMillis(); s.off = 0; s.lookOff = !aims(pl, pos, pl.getLookAngle());
	}
	/** A client sends STOP only when its own progress reached 1. A STOP in the same tick as the START, with the block far from done, is PacketMine: the server keeps digging on its own after the START and breaks the block with no hand on it. */
	public static void digStop(ServerPlayer pl, BlockPos pos) {
		S s = STATE.get(pl.getUUID());
		if (Trace.ON) Meshac.LOG.info("[trace] digStop dig={} pos={}", s == null ? null : s.dig, pos);
		if (s == null || s.dig == null || !s.dig.equals(pos)) return;
		float delta = pl.level().getBlockState(pos).getDestroyProgress(pl, pl.level(), pos);
		int ticks = pl.tickCount - s.digTick;
		if (delta < 1f && delta * (ticks + 1) < 0.5f) { s.early = true; Verdict.signal(pl, "mining", String.format("stopped digging after %d ticks, progress %.2f", ticks, delta * (ticks + 1)), 1); }
	}
	public static void digAbort(ServerPlayer pl) { S s = STATE.get(pl.getUUID()); if (s != null) s.dig = null; }
	/** Every rotation packet: yaw and pitch the client is now holding. */
	public static void look(ServerPlayer pl, float yaw, float pitch) {
		S s = STATE.get(pl.getUUID());
		if (s == null || s.dig == null) return;
		long now = System.currentTimeMillis();
		if (s.lookOff) s.off += now - s.digSample;
		s.digSample = now;
		double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
		s.lookOff = !aims(pl, s.dig, new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr)));
	}
	private static boolean aims(ServerPlayer pl, BlockPos pos, Vec3 dir) {
		Vec3 eye = pl.getEyePosition();
		return !new net.minecraft.world.phys.AABB(pos).inflate(0.3).clip(eye, eye.add(dir.scale(8))).isEmpty();
	}

	/** True when any part of the block can be seen from the eye: its centre, a point inside each corner, or the middle of each face. A hand can aim at a block along the edge of a tunnel wall where the centre line grazes a neighbour. */
	private static boolean reachable(ServerPlayer pl, Vec3 eye, BlockPos pos) {
		double e = 0.03;
		double[] lo = {pos.getX() + e, pos.getY() + e, pos.getZ() + e};
		double[] hi = {pos.getX() + 1 - e, pos.getY() + 1 - e, pos.getZ() + 1 - e};
		java.util.List<Vec3> pts = new java.util.ArrayList<>();
		pts.add(Vec3.atCenterOf(pos));
		for (int i = 0; i < 8; i++) pts.add(new Vec3((i & 1) == 0 ? lo[0] : hi[0], (i & 2) == 0 ? lo[1] : hi[1], (i & 4) == 0 ? lo[2] : hi[2]));
		for (int a = 0; a < 3; a++) for (int sgn = 0; sgn < 2; sgn++) {
			double[] c = {pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5};
			c[a] = sgn == 0 ? lo[a] : hi[a];
			pts.add(new Vec3(c[0], c[1], c[2]));
		}
		for (Vec3 p : pts) {
			BlockHitResult h = pl.level().clip(new ClipContext(eye, p, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pl));
			if (h.getType() != HitResult.Type.BLOCK || h.getBlockPos().equals(pos)) return true;
		}
		return false;
	}

	/** True when this break should be refused. */
	public static boolean refuse(ServerPlayer pl, BlockPos pos) {
		if (pl.isCreative() || pl.isSpectator()) return false;
		Vec3 eye = pl.getEyePosition();
		Vec3 centre = Vec3.atCenterOf(pos);
		if (!reachable(pl, eye, pos)) {
			Verdict.signal(pl, "mining", "block broken through another block", 1);
			return true;
		}
		// A hand breaks what the crosshair is on. Nuker breaks blocks all around without turning.
		boolean instant = pl.level().getBlockState(pos).getDestroySpeed(pl.level(), pos) == 0f; // grass, ferns, flowers, torches: no hand-timing to judge, and the view often lags the click
		if (!instant && new net.minecraft.world.phys.AABB(pos).inflate(0.3).clip(eye, eye.add(pl.getLookAngle().scale(8))).isEmpty()) {
			Verdict.signal(pl, "mining", "broke a block it is not looking at", 1);
			return true;
		}
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (Trace.ON) Meshac.LOG.info("[trace] refuse? dig={} pos={} early={}", s.dig, pos, s.early);
		if (s.dig != null && s.dig.equals(pos)) {
			if (s.lookOff) s.off += now - s.digSample;
			boolean early = s.early; s.dig = null;
			if (early) return true;
		}
		// Nuker turns the view to each block in turn. A tunnel miner keeps pointing the same way; fast successive breaks more than 40 degrees apart are not a hand.
		Vec3 dir = centre.subtract(eye).normalize();
		if (!instant && s.lastDir != null && now - s.lastAt < 600 && dir.dot(s.lastDir) < 0.766) {
			if (now - s.turnWin > 3000) { s.turnWin = now; s.turns = 0; }
			if (++s.turns >= 4) { s.turns = 0; Verdict.signal(pl, "mining", "breaks blocks in every direction", 1); return true; }
		}
		if (!instant) { s.lastDir = dir; s.lastAt = now; }
		if (Trace.ON) Meshac.LOG.info("[trace] break {} {} gap={} lookdeg={} instant={} turnsInWin={}", pl.getGameProfile().name(), pos, s.lastBreak == 0 ? -1 : now - s.lastBreak, Math.round(Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dir.dot(pl.getLookAngle())))))), instant, s.turns); s.lastBreak = now;
		if (now - s.windowAt > 1000) { s.windowAt = now; s.breaks = 0; }
		if (++s.breaks > MAX_PER_SECOND) {
			Verdict.signal(pl, "mining", "more than " + MAX_PER_SECOND + " blocks in a second", 1);
			return true;
		}
		return false;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
