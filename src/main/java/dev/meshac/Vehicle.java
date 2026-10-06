package dev.meshac;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.boat.AbstractBoat;
import net.minecraft.world.level.block.Blocks;

/** BoatFly: a boat that keeps climbing with no water, bubble column or ground under it. */
public final class Vehicle {
	private static final Map<UUID, Integer> RISING = new ConcurrentHashMap<>();
	private static final int RISE_AT = 5;

	/** True when the move should be refused. */
	public static boolean check(ServerPlayer pl, ServerboundMoveVehiclePacket p) {
		Entity v = pl.getRootVehicle();
		if (!(v instanceof AbstractBoat) || pl.isCreative() || pl.isSpectator()) { RISING.remove(pl.getUUID()); return false; }
		boolean lift = v.isInWater() || v.onGround() || v.level().getBlockState(v.blockPosition()).is(Blocks.BUBBLE_COLUMN)
			|| v.level().getBlockState(v.blockPosition().below()).is(Blocks.BUBBLE_COLUMN) || v.level().getFluidState(v.blockPosition().below()).is(FluidTags.WATER);
		if (lift || Packets.pos(p).y - v.getY() < 0.05) { RISING.remove(pl.getUUID()); return false; }
		int n = RISING.merge(pl.getUUID(), 1, Integer::sum);
		if (n < RISE_AT) return false;
		RISING.remove(pl.getUUID());
		Verdict.signal(pl, "boatfly", "boat climbing in open air", 1);
		return true;
	}

	public static void forget(UUID id) { RISING.remove(id); }
}
