package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.Items;

/**
 * Elytra (ExtraElytra, ElytraFly): gliding only trades height for speed. Energy per unit weight, v squared over 64 plus height,
 * cannot go up without a firework rocket, a hit or an explosion. A hack that holds forward and keeps speeding up breaks that.
 */
public final class Glide {
	private static final class S { int tick0 = -1; double x0, y0, z0, e0 = Double.NaN; long rocketAt, windAt; int hits; double prevE = Double.NaN, winE = Double.NaN, inputScore; int noLoss; long[] st = new long[6]; int sti; }
	private static final Map<UUID, S> STATE = new ConcurrentHashMap<>();
	private static final int WINDOW = 10;          // ticks per sample
	private static final double SLACK = 10.0;      // blocks of energy gained over the lowest point so far. Air drag only loses energy, this covers rounding and packet timing
	private static final long ROCKET_MS = 7000;    // a rocket boost lasts up to about 3 s; wait it out

	/** A firework rocket was used while a player may be gliding. */
	public static void rocket(ServerPlayer pl) {
		if (pl.getMainHandItem().is(Items.FIREWORK_ROCKET) || pl.getOffhandItem().is(Items.FIREWORK_ROCKET)) STATE.computeIfAbsent(pl.getUUID(), k -> new S()).rocketAt = System.currentTimeMillis();
	}

	/** A wind charge burst or a mace smash is near the player: a legal vanilla launch, same exemption as a rocket. */
	public static void wind(ServerPlayer pl) { STATE.computeIfAbsent(pl.getUUID(), k -> new S()).windAt = System.currentTimeMillis(); }

	/** A start-gliding command. A real glide starts once per take-off. ElytraFly Packet mode re-sends it with every move packet to keep the server flag up (the flag flaps, so the glide checks above rarely run). Six starts inside three seconds while airborne is that. */
	public static void startFly(ServerPlayer pl) {
		if (pl.isCreative() || pl.isSpectator() || pl.onGround()) return;
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		long now = System.currentTimeMillis();
		s.st[s.sti] = now; s.sti = (s.sti + 1) % s.st.length;
		long oldest = Long.MAX_VALUE; for (long v : s.st) oldest = Math.min(oldest, v);
		if (oldest > 0 && now - oldest < 3000) { java.util.Arrays.fill(s.st, 0L); Verdict.signal(pl, "glide", "restarted gliding 6 times in 3 s while airborne (packet elytra)", 1); }
	}

	/** Every position packet. Returns a setback position, or null. */
	public static double[] check(ServerPlayer pl, ServerboundMovePlayerPacket p) {
		S s = STATE.computeIfAbsent(pl.getUUID(), k -> new S());
		if (Trace.ON && (pl.tickCount % 20) == 0) Meshac.LOG.info("[trace] glidein ff={} pos={} cr={} sp={} g={}", pl.isFallFlying(), p.hasPosition(), pl.isCreative(), pl.isSpectator(), pl.onGround());
		if (!pl.isFallFlying() || !p.hasPosition() || pl.isCreative() || pl.isSpectator()) { s.tick0 = -1; s.e0 = Double.NaN; s.hits = 0; return null; }
		int now = pl.tickCount;
		double x = p.getX(pl.getX()), y = p.getY(pl.getY()), z = p.getZ(pl.getZ());
		if (s.tick0 < 0) { s.tick0 = now; s.x0 = x; s.y0 = y; s.z0 = z; return null; }
		if (now - s.tick0 < WINDOW) return null;
		double secs = (now - s.tick0) * 0.05, d = Math.sqrt((x - s.x0) * (x - s.x0) + (y - s.y0) * (y - s.y0) + (z - s.z0) * (z - s.z0));
		double v = d / secs, e = v * v / 64.0 + y;
		if (!pl.level().getEntitiesOfClass(FireworkRocketEntity.class, pl.getBoundingBox().inflate(3)).isEmpty()) s.rocketAt = System.currentTimeMillis(); // a real rocket pushes from the server side; a client-made one (Meteor ElytraBoost) does not exist here
		boolean boosted = System.currentTimeMillis() - s.rocketAt < ROCKET_MS || System.currentTimeMillis() - s.windAt < ROCKET_MS || pl.hurtTime > 0;
		// Vanilla gliding always loses energy to air drag (motion x0.99/x0.98 every tick). A flight that holds its speed and height (Meteor/Wurst ElytraFly control, packet and hover modes) never does.
		boolean clear = !boosted && !pl.horizontalCollision && !pl.verticalCollision && !pl.onGround() && !pl.isInWater() && !pl.isInLava() && pl.level().getBlockState(pl.blockPosition()).isAir() && pl.getY() > pl.level().getMinY() + 2;
		// Energy input with no rocket. A fast vanilla glide loses about 1.5 blocks of energy per 10 ticks at 31 blocks a second (measured on the rig); real rockets, wind charges, hits and explosions are excluded above. A client-side boost keeps the energy up while no rocket exists on the server.
		if (!clear || Double.isNaN(s.winE)) s.inputScore = 0;
		else if (v >= 28 && e - s.winE > -0.6) { if ((s.inputScore += 1) >= 4) { s.inputScore = 0; Verdict.signal(pl, "glide", String.format("flying at %.0f blocks a second with no rocket and no energy loss (boost hack)", v), 3); } }
		else s.inputScore = Math.max(0, s.inputScore - 0.5);
		s.winE = clear ? e : Double.NaN;
		if (!clear || Double.isNaN(s.prevE)) s.noLoss = 0;
		else if (e >= s.prevE - 0.05) s.noLoss++;
		else if (e < s.prevE - 0.4) s.noLoss = Math.max(0, s.noLoss - 3);
		s.prevE = clear ? e : Double.NaN;
		if (s.noLoss >= 8) { s.noLoss = 0; Verdict.signal(pl, "glide", String.format("no air drag in flight, holding %.0f blocks a second", v), 1); }
		double before = s.e0; s.e0 = boosted ? Double.NaN : Double.isNaN(before) ? e : Math.min(before, e); // lowest energy seen since the glide or the last boost
		s.tick0 = now; s.x0 = x; s.y0 = y; s.z0 = z;
		if (Double.isNaN(before) || boosted) { s.hits = 0; return null; }
		if (Trace.ON) Meshac.LOG.info("[trace] glide v={} e={} before={}", Math.round(v * 10) / 10.0, Math.round(e * 10) / 10.0, Math.round(before * 10) / 10.0);
		if (e - before > SLACK) {
			if (++s.hits >= 2) { s.hits = 0; Verdict.signal(pl, "glide", String.format("speeding up in flight without a rocket (%.0f blocks a second)", v), e - before > 40 ? 3 : 1); }
		} else s.hits = 0;
		return null;
	}

	public static void forget(UUID id) { STATE.remove(id); }
}
