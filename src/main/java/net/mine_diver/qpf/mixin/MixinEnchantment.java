package net.mine_diver.qpf.mixin;

import net.mine_diver.qpf.forge.IForgeItem;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Enchantment.class)
public class MixinEnchantment {

    @Inject(
            method = "isAcceptableItem(Lnet/minecraft/item/ItemStack;)Z",
            at = @At("HEAD"),
            cancellable = true
    )
    private void forgeStuff(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        Item item = stack.getItem();
        if (item instanceof IForgeItem forgeItem)
            cir.setReturnValue(forgeItem.canApplyAtEnchantingTable(stack, Enchantment.class.cast(this)));
    }
}
