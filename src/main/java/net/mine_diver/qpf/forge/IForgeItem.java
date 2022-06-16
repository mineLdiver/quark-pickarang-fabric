package net.mine_diver.qpf.forge;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;

public interface IForgeItem {

    /**
     * Checks whether an item can be enchanted with a certain enchantment. This
     * applies specifically to enchanting an item in the enchanting table and is
     * called when retrieving the list of possible enchantments for an item.
     * Enchantments may additionally (or exclusively) be doing their own checks in
     * {@link Enchantment#isAcceptableItem(ItemStack)};
     * check the individual implementation for reference. By default this will check
     * if the enchantment type is valid for this item type.
     *
     * @param stack       the item stack to be enchanted
     * @param enchantment the enchantment to be applied
     * @return true if the enchantment can be applied to this item
     */
    default boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.type.isAcceptableItem(stack.getItem());
    }
}
