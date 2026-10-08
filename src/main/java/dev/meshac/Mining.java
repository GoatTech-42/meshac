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
	private static final class S { long windowAt, lastBreak, missWin; int misses; int breaks; long lastAt, turnWin; int turns; Vec3 lastDir; BlockPos dig; int digTick; boolean early; long digSample; long off; boolean lookOff; Vec3 look = null; long brokeCt = -100; Vec3 brokeDir; BlockPos brokePos, nukePos; Vec3 lastLook; int sweep; long retWin; int retStrikes; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MAX_PER_SECOND = 8; // a hasted, efficiency V player on soft blocks tops out near 5

	/** A hand mines by holding the button with the crosshair on the block; the client drops the dig the moment the crosshair leaves. PacketMine keeps the dig alive while the view is elsewhere and turns to the block only for the final packet. Time spent aiming away during a dig is counted. */
	public static void digStart(ServerPlayer pl, BlockPos pos) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		// Nuker: after a block breaks, the next dig starts on a block more than 40 degrees away within one client tick. A hand has to turn the view and click again,
		// and held mining waits 5 client ticks between blocks; the hack calls the dig packet itself. Counted in client ticks (Ticks), not milliseconds, so a lag burst
		// that delivers several ticks of packets together still reads as separate ticks. Off for clients with no tick-end packets.
		if (Ticks.active(pl) && s.brokeDir != null && !pos.equals(s.brokePos) && Ticks.n(pl) - s.brokeCt <= 1
			&& pl.level().getBlockState(pos).getDestroySpeed(pl.level(), pos) != 0f
			&& Vec3.atCenterOf(pos).subtract(pl.getEyePosition()).normalize().dot(s.brokeDir) < 0.766) {
			long t0 = System.currentTimeMillis();
			if (t0 - s.retWin > 10000) { s.retWin = t0; s.retStrikes = 0; }
			if (++s.retStrikes >= 3) { s.nukePos = pos; Verdict.signal(pl, "mining", "starts digging a block 40+ degrees away within one tick of the last break", 1); }
		}
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

	/** True when this break should be refused. */
	public static boolean refuse(ServerPlayer pl, BlockPos pos) {
		if (pl.isCreative() || pl.isSpectator()) return false;
		Vec3 eye = pl.getEyePosition();
		Vec3 centre = Vec3.atCenterOf(pos);
		BlockHitResult hit = pl.level().clip(new ClipContext(eye, centre, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, pl));
		if (hit.getType() == HitResult.Type.BLOCK && !hit.getBlockPos().equals(pos)) {
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
		if (pos.equals(s.nukePos)) { s.nukePos = null; return true; }
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
		// Sweep mining (Wurst Nuker, survival). Rig trace of the real hack: one block every 550-650 ms around a sphere, the view turning 30-170 degrees between every pair of breaks
		// (it faces each block by packet, so the turn costs it nothing). A hand needs time to flick and settle on each new block, and mining a wall at close range turns only about
		// 25-35 degrees per block. Count breaks where the view turned 60+ degrees since the last break and the last break was at most 15 client ticks (750 ms) ago: +1; any other break -1.
		// 8 net is a signal and the break is refused. Client ticks, not milliseconds, so a lag burst that lands several breaks together does not count as speed; clients with no tick-end
		// packets use 750 ms. A hand sweeping at that rate for 8 breaks in a row, every one a 60+ degree flick, does not exist; wall sweeps (25-35) and tunnels turn well under 60.
		Vec3 lookNow = pl.getLookAngle();
		if (!instant && s.lastLook != null && now - s.lastAt < 3000) {
			double lookTurn = Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, lookNow.dot(s.lastLook)))));
			boolean fast = Ticks.active(pl) ? Ticks.n(pl) - s.brokeCt <= 15 : now - s.lastAt < 750;
			if (lookTurn >= 60 && fast) s.sweep = Math.min(12, s.sweep + 1); else s.sweep = Math.max(0, s.sweep - 1);
			if (Trace.ON) Meshac.LOG.info("[trace] sweep {} {} turn={} gapMs={} gapTicks={} sweep={}", pl.getGameProfile().name(), pos, String.format("%.1f", lookTurn), now - s.lastAt, Ticks.n(pl) - s.brokeCt, s.sweep);
			if (s.sweep >= 8) { Verdict.signal(pl, "mining", "breaks blocks one after another with a 60+ degree view turn each time", 1); return true; }
		}
		if (!instant) s.lastLook = lookNow;
		if (!instant) { s.lastDir = dir; s.lastAt = now; s.brokeDir = dir; s.brokeCt = Ticks.n(pl); s.brokePos = pos; }
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
