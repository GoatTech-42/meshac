package dev.meshac;
import net.minecraft.world.level.block.Block;
/** Living-player restitution for this compiled version family. */
public final class BounceRestitution {
 private BounceRestitution() {}
 public static double of(Block block) { return block.getBounceRestitution(); }
}
