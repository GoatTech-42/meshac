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
	private static final class S { boolean pairOpen; final long[] rt = new long[6]; final float[] ry = new float[6], rp = new float[6]; int ri; long windowAt; int uses; long lastUse; int burst; double lx, lz, chainDist; long lt, chainMs; int fwd; float pitch, prevPitch, yaw, prevYaw; long snapWin; int snaps; long flickAt, shotWin; int shots; long clickAt, entAt, entWin, swapAt, useWin; int swapUses; int entId = -1, entStrikes; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int MAX_PER_SECOND = 16; // fast bridging is about 8 a second

	/** Every move packet that carries a rotation. */
	public static void look(ServerPlayer pl, float yaw, float pitch) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		s.prevPitch = s.pitch; s.prevYaw = s.yaw; s.pitch = pitch; s.yaw = yaw;
		s.ri = (s.ri + 1) % s.rt.length; s.rt[s.ri] = System.currentTimeMillis(); s.ry[s.ri] = yaw; s.rp[s.ri] = pitch;
		if (Math.max(Math.abs(pitch - s.prevPitch), Math.abs(net.minecraft.util.Mth.wrapDegrees(yaw - s.prevYaw))) > 25) s.flickAt = System.currentTimeMillis();
	}

	/** A bow or crossbow let go. Aimbots swing onto the target in one packet while the bow is drawn. */
	public static void release(ServerPlayer pl) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.shotWin > 30000) { s.shotWin = now; s.shots = 0; }
		if (now - s.flickAt < 1500 && ++s.shots >= 3) { Verdict.signal(pl, "interact", "view snaps onto a target as the bow is let go", 1); }
	}

	/** A use-item packet (throw, eat, bow). */
	/** A hotbar slot change. */
	public static void swap(ServerPlayer pl) { STATE.computeIfAbsent(pl.getUUID(), k -> new S()).swapAt = System.currentTimeMillis(); }

	/** AutoPotion and other swap-throw-swap hacks: the item is used in the same tick the slot changes. Pressing a number key and then clicking takes a hand over 40 ms. Returns true when the use must be refused; repeats inside ten seconds are a signal. */
	public static boolean use(ServerPlayer pl) {
		Glide.rocket(pl); rate(pl);
		if (pl.isCreative() || pl.isSpectator()) return false;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.swapAt >= 40) return false;
		// Swap-and-use is how respawn anchors, totems and gapples are played, and a laggy link delivers the slot change and the click together. Only potions and throwables are worth refusing.
		var it = pl.getMainHandItem().getItem().toString();
		if (!(it.contains("potion") || it.contains("pearl") || it.contains("snowball") || it.contains("egg"))) return false;
		if (now - s.useWin > 10000) { s.useWin = now; s.swapUses = 0; }
		if (++s.swapUses >= 4) Verdict.signal(pl, "interact", "used an item " + (now - s.swapAt) + " ms after changing slot", 1);
		return true;
	}

	/** A use-item-on-block packet (placing). */
	/** Returns true when the click must be refused: it hit a block the player is not looking at. */
	/** FeedAura and other entity auras: a new target every tick. A hand needs time to turn to another animal; an interaction with a different entity inside a quarter second of the last one is refused, and repeats inside three seconds are a signal. */
	public static boolean entity(ServerPlayer pl, int id) {
		if (pl.isCreative() || pl.isSpectator()) return false;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (s.entId != -1 && s.entId != id && now - s.entAt < 250) {
			if (now - s.entWin > 3000) { s.entWin = now; s.entStrikes = 0; }
			if (++s.entStrikes >= 2) Verdict.signal(pl, "interact", "new target " + (now - s.entAt) + " ms after the last one", 1);
			return true;
		}
		s.entId = id; s.entAt = now;
		return false;
	}

	/** AutoSign: the editor opens when the sign is clicked or placed. Typing a line and pressing Done takes a hand longer than a second; the hack answers in the same tick. Text that arrives sooner is refused. */
	public static boolean sign(ServerPlayer pl, String[] lines) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		boolean typed = false;
		for (String l : lines) typed |= !l.isEmpty();
		if (!typed || System.currentTimeMillis() - s.clickAt > 1000) return false;
		Verdict.signal(pl, "interact", "sign text filled in " + (System.currentTimeMillis() - s.clickAt) + " ms after the click", 1);
		return true;
	}

	public static boolean place(ServerPlayer pl, ServerboundUseItemOnPacket p) {
		if (pl.isCreative() || pl.isSpectator()) return false;
		// AirPlace: a hand can only click a face of a block that is there. Vanilla places against thin air if the packet says so.
		if (pl.level().getBlockState(Packets.hit(p).getBlockPos()).isAir()) { Verdict.signal(pl, "interact", "placed against air", 1); rate(pl); return true; }
		AABB box = new AABB(Packets.hit(p).getBlockPos()).inflate(0.3);
		Vec3 eye = pl.getEyePosition();
		boolean seen = !box.clip(eye, eye.add(pl.getLookAngle().scale(8))).isEmpty();
		// The click carries the view the client has on screen right now, which can be a few packets newer than the last rotation the server saw. A view held within the last quarter second counts.
		S sv = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long tnow = System.currentTimeMillis();
		for (int i = 0; i < sv.rt.length && !seen; i++) {
			if (sv.rt[i] == 0 || tnow - sv.rt[i] > 250) continue;
			double yr = Math.toRadians(sv.ry[i]), pr = Math.toRadians(sv.rp[i]);
			Vec3 dir = new Vec3(-Math.sin(yr) * Math.cos(pr), -Math.sin(pr), Math.cos(yr) * Math.cos(pr));
			seen = !box.clip(eye, eye.add(dir.scale(8))).isEmpty();
		}
		if (!seen) { Verdict.signal(pl, "interact", "clicked a block it is not looking at", 1); rate(pl); return true; }
		// Human flicks and teleport aim corrections are legal; rotation jumps alone do not prove automation.
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		// Scaffold walks over air at full walking speed with the view pinned down, laying a block under each step. Even elite speed-bridgers crouch at the edge and stay under 4 blocks a second.
		double dist = Math.hypot(pl.getX() - s.lx, pl.getZ() - s.lz); long dt = now - s.lt;
		// Only blocks laid where the feet would be (over air) count as scaffolding; walking on solid ground while holding right click lays blocks ahead of you and is a legal play.
		net.minecraft.core.BlockPos laid = Packets.hit(p).getBlockPos().relative(Packets.hit(p).getDirection());
		boolean underFeet = laid.getY() == net.minecraft.util.Mth.floor(pl.getY()) - 1 && Math.abs(laid.getX() + 0.5 - pl.getX()) < 1.2 && Math.abs(laid.getZ() + 0.5 - pl.getZ()) < 1.2;
		boolean step = underFeet && pl.getXRot() > 70 && dist > 0.3 && dt < 700;
		if (step) { s.fwd++; s.chainDist += dist; s.chainMs += dt; } else { s.fwd = 0; s.chainDist = 0; s.chainMs = 0; }
		s.lx = pl.getX(); s.lz = pl.getZ(); s.lt = now;
		if (s.fwd >= 10) {
			double speed = s.chainDist / (s.chainMs / 1000.0);
			if (speed > 4.0) Verdict.signal(pl, "interact", String.format("bridging at %.1f blocks a second", speed), 1);
			s.fwd = 0; s.chainDist = 0; s.chainMs = 0;
		}
		s.clickAt = now;
		rate(pl);
		return false;
	}

	private static void rate(ServerPlayer pl) {
		if (pl.isCreative() || pl.isSpectator()) return;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		if (now - s.windowAt > 1000) { s.windowAt = now; s.uses = 0; }
		// One click on an item that can also be placed arrives as two packets in the same instant; that is one use.
		if (s.pairOpen && now - s.lastUse <= 5) { s.pairOpen = false; return; }
		s.pairOpen = true;
		if (now - s.lastUse < 20) s.burst++; else s.burst = 0; // several uses inside one tick
		s.lastUse = now;
		if (s.burst >= 5) { s.burst = 0; Verdict.signal(pl, "interact", "several uses in one tick", 1); } // a lag stall can release a few held clicks at once; six in one tick is not a hand
		else if (++s.uses > MAX_PER_SECOND) { s.uses = 0; Verdict.signal(pl, "interact", "more than " + MAX_PER_SECOND + " uses in a second", 1); }
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
