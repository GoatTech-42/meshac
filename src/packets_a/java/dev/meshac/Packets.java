package dev.meshac;

import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Packet accessors that were renamed in 26.3. */
public final class Packets {
	public static BlockHitResult hit(ServerboundUseItemOnPacket p) { return p.getHitResult(); }
	public static Vec3 pos(ServerboundMoveVehiclePacket p) { return p.position(); }
	public static int seq(ServerboundUseItemOnPacket p) { return p.getSequence(); }
	public static net.minecraft.world.InteractionHand hand(ServerboundUseItemOnPacket p) { return p.getHand(); }
	public static int seq(ServerboundUseItemPacket p) { return p.getSequence(); }
	public static String[] lines(ServerboundSignUpdatePacket p) { return p.getLines(); }
}
