package dev.meshac.mixin;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The dragon egg, or a shulker box or bundle holding one, cannot be placed in an ender chest (click, shift-click, hotkey and drag all ask the slot first). */
@Mixin(Slot.class)
public abstract class SlotMixin {
	private static boolean hasEgg(ItemStack st, int depth) {
		if (st.isEmpty() || depth > 8) return false;
		if (st.is(Items.DRAGON_EGG)) return true;
		var c = st.get(net.minecraft.core.component.DataComponents.CONTAINER);
		if (c != null && c.nonEmptyItemCopyStream().anyMatch(i -> hasEgg(i, depth + 1))) return true;
		var b = st.get(net.minecraft.core.component.DataComponents.BUNDLE_CONTENTS);
		if (b != null) for (var i : b.items()) if (hasEgg(i.create(), depth + 1)) return true;
		return false;
	}

	@Inject(method = "mayPlace", at = @At("HEAD"), cancellable = true)
	private void meshac$noEgg(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
		if (hasEgg(stack, 0) && ((Slot) (Object) this).container instanceof net.minecraft.world.inventory.PlayerEnderChestContainer) cir.setReturnValue(false);
	}
}
