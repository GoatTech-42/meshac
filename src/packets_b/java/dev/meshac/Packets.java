package dev.meshac;

import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Packet accessors that were renamed in 26.3. */
public final class Packets {
	public static BlockHitResult hit(ServerboundUseItemOnPacket p) { return p.hitResult(); }
	public static Vec3 pos(ServerboundMoveVehiclePacket p) { return p.movingTo().position(); }
	public static int seq(ServerboundUseItemOnPacket p) { return p.sequence(); }
	public static int seq(ServerboundUseItemPacket p) { return p.sequence(); }
	public static String[] lines(ServerboundSignUpdatePacket p) { return p.lines().toArray(new String[0]); }
}
