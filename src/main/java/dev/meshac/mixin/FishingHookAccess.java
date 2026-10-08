package dev.meshac.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Reads the bite timer of a fishing hook. VERIFY the field is named nibble in the Mojang mappings of every supported version. */
@Mixin(FishingHook.class)
public interface FishingHookAccess {
	@Accessor("nibble") int meshac$nibble();
}
