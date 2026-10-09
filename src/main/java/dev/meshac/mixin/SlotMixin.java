package dev.meshac.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The dragon egg cannot be placed in an ender chest (click, shift-click, hotkey and drag all ask the slot first). */
@Mixin(Slot.class)
public abstract class SlotMixin {
	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void meshac$noEgg(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (stack.is(Items.DRAGON_EGG) && ((Slot) (Object) this).container instanceof net.minecraft.world.inventory.PlayerEnderChestContainer) cir.setReturnValue(false);
	}
}
