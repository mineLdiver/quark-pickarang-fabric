package net.mine_diver.qpf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.mine_diver.qpf.content.tools.entity.Pickarang;
import net.mine_diver.qpf.content.tools.item.PickarangItem;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;

public class QuarkPickarangFabric implements ModInitializer {

    public static final String MOD_ID = "qpf";

    public static int timeout = 20;
    public static int netheriteTimeout = 20;

    public static int harvestLevel = 3;
    public static int netheriteHarvestLevel = 4;

    public static int durability = 800;

    public static int netheriteDurability = 1040;

    public static double maxHardness = 20.0;

    public static double netheriteMaxHardness = 20.0;

    public static boolean neverUseHeartOfDiamond = false;

    public static boolean noCooldown = false;
    public static boolean netheriteNoCooldown = false;

    public static final EntityType<Pickarang> PICKARANG_TYPE = FabricEntityTypeBuilder.<Pickarang>create(SpawnGroup.MISC, Pickarang::new)
            .dimensions(EntityDimensions.fixed(.4F, .4F))
            .trackRangeChunks(4)
            .trackedUpdateRate(10)
            .build();

    public static final Item PICKARANG = new PickarangItem(propertiesFor(durability, false), false);
    public static final Item FLAMARANG = new PickarangItem(propertiesFor(netheriteDurability, true), true);

    public static final SoundEvent ENTITY_PICKARANG_THROW = new SoundEvent(of("entity.pickarang.throw"));
    public static final SoundEvent ENTITY_PICKARANG_CLANK = new SoundEvent(of("entity.pickarang.clank"));
    public static final SoundEvent ENTITY_PICKARANG_SPARK = new SoundEvent(of("entity.pickarang.spark"));
    public static final SoundEvent ENTITY_PICKARANG_PICKUP = new SoundEvent(of("entity.pickarang.pickup"));

    @Override
    public void onInitialize() {
        registerEntity(PICKARANG_TYPE, "pickarang");
        registerItem(PICKARANG, "pickarang");
        registerItem(FLAMARANG, "flamerang");
        registerSound(ENTITY_PICKARANG_THROW);
        registerSound(ENTITY_PICKARANG_CLANK);
        registerSound(ENTITY_PICKARANG_SPARK);
        registerSound(ENTITY_PICKARANG_PICKUP);
    }

    private static Item.Settings propertiesFor(int durability, boolean netherite) {
        Item.Settings properties = new FabricItemSettings()
                .maxCount(1)
                .group(ItemGroup.TOOLS);

        if (durability > 0)
            properties.maxDamage(durability);

        if(netherite)
            properties.fireproof();

        return properties;
    }

    private static void registerEntity(EntityType<?> entity, String id) {
        Registry.register(Registry.ENTITY_TYPE, of(id), entity);
    }

    private static void registerItem(Item item, String id) {
        Registry.register(Registry.ITEM, of(id), item);
    }

    private static void registerSound(SoundEvent sound) {
        Registry.register(Registry.SOUND_EVENT, sound.getId(), sound);
    }

    private static final ThreadLocal<Pickarang> ACTIVE_PICKARANG = new ThreadLocal<>();

    public static void setActivePickarang(Pickarang pickarang) {
        ACTIVE_PICKARANG.set(pickarang);
    }

    public static DamageSource createDamageSource(PlayerEntity player) {
        Pickarang pickarang = ACTIVE_PICKARANG.get();

        if (pickarang == null)
            return null;

        return new ProjectileDamageSource("player", pickarang, player).setProjectile();
    }

    public static Identifier of(String id) {
        return new Identifier(MOD_ID, id);
    }
}
