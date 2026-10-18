package dev.meshac;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlimeBlock;
import net.minecraft.world.level.block.BedBlock;
/** Exact living-player constants from the legacy SlimeBlock/BedBlock bounceUp handlers. */
public final class BounceRestitution {
 private BounceRestitution() {}
 public static double of(Block block) { return block instanceof SlimeBlock ? 1.0 : block instanceof BedBlock ? 0.6600000262260437 : 0.0; }
}
