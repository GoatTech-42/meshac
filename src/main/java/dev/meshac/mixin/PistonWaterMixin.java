package dev.meshac.mixin;

import dev.meshac.Config;
import net.minecraft.world.level.block.piston.PistonMovingBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Patches MC-130183: a sticky piston on a short pulse finishes the move through finalTick(), which
 * (unlike the normal tick() path) keeps the moved block waterlogged. Flood machines use that to carry
 * water for free. With fixPistonWater on, finalTick clears waterlogged exactly like the normal path does.
 * Pistons and machines are untouched, only the carried water is dropped.
 */
@Mixin(PistonMovingBlockEntity.class)
public abstract class PistonWaterMixin {
	@Shadow private BlockState movedState;
	@Shadow private boolean isSourcePiston;

	@Inject(method = "finalTick", at = @At("HEAD"))
	private void meshac$dryShortPulse(CallbackInfo ci) {
		Boolean on = Config.get().fixPistonWater;
		if (on == null || !on || isSourcePiston) return;
		BlockState s = movedState;
		if (s != null && s.hasProperty(BlockStateProperties.WATERLOGGED) && s.getValue(BlockStateProperties.WATERLOGGED)) {
			movedState = s.setValue(BlockStateProperties.WATERLOGGED, false);
		}
	}
}
