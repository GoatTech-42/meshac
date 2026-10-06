package dev.meshac;

import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Packet accessors that were renamed in 26.3. */
final class Packets {
	static BlockHitResult hit(ServerboundUseItemOnPacket p) { return p.hitResult(); }
	static Vec3 pos(ServerboundMoveVehiclePacket p) { return p.movingTo().position(); }
}
