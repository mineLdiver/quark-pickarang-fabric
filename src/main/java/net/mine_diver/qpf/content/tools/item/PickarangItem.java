package net.mine_diver.qpf.content.tools.item;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import net.mine_diver.qpf.QuarkPickarangFabric;
import net.mine_diver.qpf.content.tools.entity.Pickarang;
import net.mine_diver.qpf.forge.IForgeItem;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.stat.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.HashSet;

public class PickarangItem extends Item implements IForgeItem {

    public final boolean isNetherite;

    public PickarangItem(Settings settings, boolean isNetherite) {
        super(settings);
        this.isNetherite = isNetherite;
    }

    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        stack.damage(2, attacker, player -> player.sendToolBreakStatus(Hand.MAIN_HAND));
        return true;
    }

    @Override
    public boolean isSuitableFor(BlockState state) {
        return switch (isNetherite ? QuarkPickarangFabric.netheriteHarvestLevel : QuarkPickarangFabric.harvestLevel) {
            case 0 -> Items.WOODEN_PICKAXE.isSuitableFor(state) ||
                    Items.WOODEN_AXE.isSuitableFor(state) ||
                    Items.WOODEN_SHOVEL.isSuitableFor(state);
            case 1 -> Items.STONE_PICKAXE.isSuitableFor(state) ||
                    Items.STONE_AXE.isSuitableFor(state) ||
                    Items.STONE_SHOVEL.isSuitableFor(state);
            case 2 -> Items.IRON_PICKAXE.isSuitableFor(state) ||
                    Items.IRON_AXE.isSuitableFor(state) ||
                    Items.IRON_SHOVEL.isSuitableFor(state);
            case 3 -> Items.DIAMOND_PICKAXE.isSuitableFor(state) ||
                    Items.DIAMOND_AXE.isSuitableFor(state) ||
                    Items.DIAMOND_SHOVEL.isSuitableFor(state);
            default -> Items.NETHERITE_PICKAXE.isSuitableFor(state) ||
                    Items.NETHERITE_AXE.isSuitableFor(state) ||
                    Items.NETHERITE_SHOVEL.isSuitableFor(state);
        };
    }

    @Override
    public int getMaxUseTime(ItemStack stack) {
        return Math.max(isNetherite ? QuarkPickarangFabric.netheriteDurability : QuarkPickarangFabric.durability, 0);
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        if (state.getHardness(world, pos) != 0)
            stack.damage(1, miner, player -> player.sendToolBreakStatus(Hand.MAIN_HAND));
        return true;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemstack = user.getStackInHand(hand);
        user.setStackInHand(hand, ItemStack.EMPTY);
        int eff = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemstack);
        Vec3d pos = user.getPos();
        world.playSound(null, pos.x, pos.y, pos.z, QuarkPickarangFabric.ENTITY_PICKARANG_THROW, SoundCategory.NEUTRAL, 0.5F + eff * 0.14F, 0.4F / (world.random.nextFloat() * 0.4F + 0.8F));

        if(!world.isClient) {
            PlayerInventory inventory = user.getInventory();
            int slot = hand == Hand.OFF_HAND ? inventory.size() - 1 : inventory.selectedSlot;
            Pickarang entity = new Pickarang(world, user);
            entity.setThrowData(slot, itemstack, isNetherite);
            entity.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F + eff * 0.325F, 0F);
            world.spawnEntity(entity);
        }

        if(!user.getAbilities().creativeMode && (isNetherite ? QuarkPickarangFabric.netheriteNoCooldown : QuarkPickarangFabric.noCooldown)) {
            int cooldown = 10 - eff;
            if (cooldown > 0)
                user.getItemCooldownManager().set(this, cooldown);
        }

        user.incrementStat(Stats.USED.getOrCreateStat(this));
        return new TypedActionResult<>(ActionResult.SUCCESS, ItemStack.EMPTY);
    }

    @Nonnull
    @Override
    public Multimap<EntityAttribute, EntityAttributeModifier> getAttributeModifiers(@Nonnull EquipmentSlot slot) {
        Multimap<EntityAttribute, EntityAttributeModifier> multimap = Multimaps.newSetMultimap(new HashMap<>(), HashSet::new);

        if (slot == EquipmentSlot.MAINHAND) {
            multimap.put(EntityAttributes.GENERIC_ATTACK_DAMAGE, new EntityAttributeModifier(ATTACK_DAMAGE_MODIFIER_ID, "Weapon modifier", isNetherite ? 3 : 2, EntityAttributeModifier.Operation.ADDITION));
            multimap.put(EntityAttributes.GENERIC_ATTACK_SPEED, new EntityAttributeModifier(ATTACK_SPEED_MODIFIER_ID, "Weapon modifier", -2.8, EntityAttributeModifier.Operation.ADDITION));
        }

        return multimap;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        return 0;
    }

    @Override
    public boolean canRepair(ItemStack stack, ItemStack ingredient) {
        return ingredient.getItem() == (isNetherite ? Items.NETHERITE_INGOT : Items.DIAMOND);
    }

    @Override
    public int getEnchantability() {
        return isNetherite ? Items.NETHERITE_PICKAXE.getEnchantability() : Items.DIAMOND_PICKAXE.getEnchantability();
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return IForgeItem.super.canApplyAtEnchantingTable(stack, enchantment) || ImmutableSet.of(Enchantments.FORTUNE, Enchantments.SILK_TOUCH, Enchantments.EFFICIENCY).contains(enchantment);
    }
}
