package net.mine_diver.qpf.content.tools.entity;

import com.google.common.collect.Multimap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import net.mine_diver.qpf.QuarkPickarangFabric;
import net.mine_diver.qpf.mixin.LivingEntityAccessor;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.*;
import net.minecraft.entity.damage.ProjectileDamageSource;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.network.Packet;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class Pickarang extends ProjectileEntity {

    private static final TrackedData<ItemStack> STACK = DataTracker.registerData(Pickarang.class, TrackedDataHandlerRegistry.ITEM_STACK);
    private static final TrackedData<Boolean> RETURNING = DataTracker.registerData(Pickarang.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> NETHERITE_SYNCED = DataTracker.registerData(Pickarang.class, TrackedDataHandlerRegistry.BOOLEAN);

    protected LivingEntity owner;
    private UUID ownerId;

    private int liveTime;
    private int slot;
    private int blockHitCount;
    public boolean netherite;

    private IntOpenHashSet entitiesHit;

    private static final String TAG_RETURNING = "returning";
    private static final String TAG_LIVE_TIME = "liveTime";
    private static final String TAG_BLOCKS_BROKEN = "hitCount";
    private static final String TAG_RETURN_SLOT = "returnSlot";
    private static final String TAG_ITEM_STACK = "itemStack";
    private static final String TAG_NETHERITE = "netherite";

    public Pickarang(EntityType<? extends Pickarang> entityType, World world) {
        super(entityType, world);
    }

    public Pickarang(World worldIn, LivingEntity throwerIn) {
        super(QuarkPickarangFabric.PICKARANG_TYPE, worldIn);
        Vec3d pos = throwerIn.getPos();
        this.setPos(pos.x, pos.y + throwerIn.getStandingEyeHeight(), pos.z);
        ownerId = throwerIn.getUuid();
    }

    @Override
    public boolean shouldRender(double distance) {
        double d0 = this.getBoundingBox().getAverageSideLength() * 4;
        if (Double.isNaN(d0)) d0 = 4;

        d0 = d0 * 64;
        return distance < d0 * d0;
    }

    public void setVelocity(Entity entityThrower, float rotationPitchIn, float rotationYawIn, float pitchOffset, float velocity, float inaccuracy) {
        float f = -MathHelper.sin(rotationYawIn * ((float) Math.PI / 180F)) * MathHelper.cos(rotationPitchIn * ((float) Math.PI / 180F));
        float f1 = -MathHelper.sin((rotationPitchIn + pitchOffset) * ((float) Math.PI / 180F));
        float f2 = MathHelper.cos(rotationYawIn * ((float) Math.PI / 180F)) * MathHelper.cos(rotationPitchIn * ((float) Math.PI / 180F));
        this.setVelocity(f, f1, f2, velocity, inaccuracy);
        Vec3d Vector3d = entityThrower.getVelocity();
        this.setVelocity(this.getVelocity().add(Vector3d.x, entityThrower.isOnGround() ? 0.0D : Vector3d.y, Vector3d.z));
    }

    @Override
    public void setVelocity(double x, double y, double z, float velocity, float inaccuracy) {
        Vec3d vec = (new Vec3d(x, y, z)).normalize().add(this.random.nextGaussian() * 0.0075F * inaccuracy, this.random.nextGaussian() * 0.0075F * inaccuracy, this.random.nextGaussian() * 0.0075F * inaccuracy).multiply(velocity);
        this.setVelocity(vec);
        float f = (float) vec.horizontalLength();
        //noinspection SuspiciousNameCombination
        setYaw((float) (MathHelper.atan2(vec.x, vec.z) * (180F / (float) Math.PI)));
        setPitch((float) (MathHelper.atan2(vec.y, f) * (180F / (float) Math.PI)));
        this.prevYaw = this.getYaw();
        this.prevPitch = this.getPitch();
    }

    @Override
    public void setVelocityClient(double x, double y, double z) {
        this.setVelocity(x, y, z);
        if (this.prevPitch == 0.0F && this.prevYaw == 0.0F) {
            float f = (float) Math.sqrt(x * x + z * z);
            //noinspection SuspiciousNameCombination
            setYaw((float) (MathHelper.atan2(x, z) * (180F / (float) Math.PI)));
            setPitch((float) (MathHelper.atan2(y, f) * (180F / (float) Math.PI)));
            this.prevYaw = this.getYaw();
            this.prevPitch = this.getPitch();
        }
    }

    public void setThrowData(int slot, ItemStack stack, boolean netherite) {
        this.slot = slot;
        setStack(stack.copy());
        this.netherite = netherite;
    }

    @Override
    protected void initDataTracker() {
        dataTracker.startTracking(STACK, new ItemStack(QuarkPickarangFabric.PICKARANG));
        dataTracker.startTracking(RETURNING, false);
        dataTracker.startTracking(NETHERITE_SYNCED, false);
    }

    protected void checkImpact() {
        if (world.isClient)
            return;

        Vec3d motion = getVelocity();
        Vec3d position = getPos();
        Vec3d rayEnd = position.add(motion);

        boolean doEntities = true;
        int tries = 100;

        while (isAlive() && !dataTracker.get(RETURNING)) {
            if (doEntities) {
                EntityHitResult result = raycastEntities(position, rayEnd);
                if (result != null)
                    onCollision(result);
                else doEntities = false;
            } else {
                HitResult result = world.raycast(new RaycastContext(position, rayEnd, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, this));
                if (result.getType() == HitResult.Type.MISS)
                    return;
                else onCollision(result);
            }

            if (tries-- <= 0) {
                (new RuntimeException("Pickarang hit way too much, this shouldn't happen")).printStackTrace();
                return;
            }
        }
    }

    @Nullable
    protected EntityHitResult raycastEntities(Vec3d from, Vec3d to) {
        return ProjectileUtil.getEntityCollision(world, this, from, to, getBoundingBox().stretch(getVelocity()).expand(1.0D), entity ->
                !entity.isSpectator()
                        && entity.isAlive()
                        && (entity.collides() || entity instanceof Pickarang)
                        && entity != getThrower()
                        && (entitiesHit == null || !entitiesHit.contains(entity.getId()))
        );
    }

    @Override
    protected void onCollision(@Nonnull HitResult result) {
        LivingEntity owner = getThrower();

        if (result.getType() == HitResult.Type.BLOCK && result instanceof BlockHitResult) {
            BlockPos hit = ((BlockHitResult) result).getBlockPos();
            BlockState state = world.getBlockState(hit);

            if (getPiercingModifier() == 0 || state.getMaterial().blocksLight())
                addHit();

            if (!(owner instanceof ServerPlayerEntity player))
                return;

            float hardness = state.getHardness(world, hit);
            if (hardness <= (netherite ? QuarkPickarangFabric.netheriteMaxHardness : QuarkPickarangFabric.maxHardness) && hardness >= 0) {
                ItemStack prev = player.getMainHandStack();
                player.setStackInHand(Hand.MAIN_HAND, getStack());

                if (player.interactionManager.tryBreakBlock(hit))
                    world.syncWorldEvent(null, 2001, hit, Block.getRawIdFromState(state));
                else
                    clank();

                setStack(player.getMainHandStack());

                player.setStackInHand(Hand.MAIN_HAND, prev);
            } else
                clank();

        } else if (result.getType() == HitResult.Type.ENTITY && result instanceof EntityHitResult) {
            Entity hit = ((EntityHitResult) result).getEntity();

            if (hit != owner) {
                addHit(hit);
                if (hit instanceof Pickarang) {
                    ((Pickarang) hit).setReturning();
                    clank();
                } else {
                    ItemStack pickarang = getStack();
                    Multimap<EntityAttribute, EntityAttributeModifier> modifiers = pickarang.getAttributeModifiers(EquipmentSlot.MAINHAND);

                    if (owner != null) {
                        ItemStack prev = owner.getMainHandStack();
                        owner.setStackInHand(Hand.MAIN_HAND, pickarang);
                        owner.getAttributes().addTemporaryModifiers(modifiers);

                        LivingEntityAccessor ownerAccessor = (LivingEntityAccessor) owner;
                        int ticksSinceLastSwing = ownerAccessor.getLastAttackedTicks();
                        ownerAccessor.setLastAttackedTicks((int) (1.0 / owner.getAttributeValue(EntityAttributes.GENERIC_ATTACK_SPEED) * 20.0) + 1);

                        float prevHealth = hit instanceof LivingEntity ? ((LivingEntity) hit).getHealth() : 0;

                        QuarkPickarangFabric.setActivePickarang(this);

//                        hitEntity: {
//                            if(hit instanceof Toretoise toretoise) {
//                                int ore = toretoise.getOreType();
//
//                                if(ore != 0) {
//                                    addHit(toretoise);
//                                    if (level instanceof ServerLevel serverLevel) {
//                                        LootContext.Builder lootBuilder = new LootContext.Builder(serverLevel)
//                                                .withParameter(LootContextParams.TOOL, pickarang);
//                                        if (owner instanceof Player player)
//                                            lootBuilder.withLuck(player.getLuck());
//                                        toretoise.dropOre(ore, lootBuilder);
//                                    }
//                                    break hitEntity;
//                                }
//                            }

                        if (owner instanceof PlayerEntity)
                            ((PlayerEntity) owner).attack(hit);
                        else
                            owner.tryAttack(hit);

                        if (hit instanceof LivingEntity && ((LivingEntity) hit).getHealth() == prevHealth)
                            clank();
//                        }


                        QuarkPickarangFabric.setActivePickarang(null);

                        ownerAccessor.setLastAttackedTicks(ticksSinceLastSwing);

                        setStack(owner.getMainHandStack());
                        owner.setStackInHand(Hand.MAIN_HAND, prev);
                        owner.getAttributes().addTemporaryModifiers(modifiers);
                    } else {
                        DefaultAttributeContainer.Builder mapBuilder = new DefaultAttributeContainer.Builder();
                        mapBuilder.add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 1);
                        DefaultAttributeContainer map = mapBuilder.build();
                        AttributeContainer manager = new AttributeContainer(map);
                        manager.addTemporaryModifiers(modifiers);

                        ItemStack stack = getStack();
                        stack.damage(1, world.random, null);
                        setStack(stack);
                        hit.damage(new ProjectileDamageSource("player", this, this).setProjectile(),
                                (float) manager.getValue(EntityAttributes.GENERIC_ATTACK_DAMAGE));
                    }
                }
            }
        }
    }

    public void spark() {
        playSound(QuarkPickarangFabric.ENTITY_PICKARANG_SPARK, 1, 1);
        setReturning();
    }

    public void clank() {
        playSound(QuarkPickarangFabric.ENTITY_PICKARANG_CLANK, 1, 1);
        setReturning();
    }

    public void addHit(Entity entity) {
        if (entitiesHit == null)
            entitiesHit = new IntOpenHashSet(5);
        entitiesHit.add(entity.getId());
        postHit();
    }

    public void postHit() {
        if ((entitiesHit == null ? 0 : entitiesHit.size()) + blockHitCount > getPiercingModifier())
            setReturning();
        else if (getPiercingModifier() > 0)
            setVelocity(getVelocity().multiply(0.8));
    }

    public void addHit() {
        blockHitCount++;
        postHit();
    }

    protected void setReturning() {
        dataTracker.set(RETURNING, true);
    }

    @Override
    public boolean isPushedByFluids() {
        return false;
    }

    @Override
    public void tick() {
        Vec3d pos = getPos();

        this.lastRenderX = pos.x;
        this.lastRenderY = pos.y;
        this.lastRenderZ = pos.z;
        super.tick();

        if (!dataTracker.get(RETURNING))
            checkImpact();

        Vec3d ourMotion = this.getVelocity();
        setPosition(pos.x + ourMotion.x, pos.y + ourMotion.y, pos.z + ourMotion.z);

        float f = (float) ourMotion.horizontalLength();
        //noinspection SuspiciousNameCombination
        setYaw((float) (MathHelper.atan2(ourMotion.x, ourMotion.z) * (180F / (float) Math.PI)));

        setPitch((float) (MathHelper.atan2(ourMotion.y, f) * (180F / (float) Math.PI)));
        while (this.getPitch() - this.prevPitch < -180.0F) this.prevPitch -= 360.0F;

        while (this.getPitch() - this.prevPitch >= 180.0F) this.prevPitch += 360.0F;

        while (this.getYaw() - this.prevYaw < -180.0F) this.prevYaw -= 360.0F;

        while (this.getYaw() - this.prevYaw >= 180.0F) this.prevYaw += 360.0F;

        setPitch(MathHelper.lerp(0.2F, this.prevPitch, this.getPitch()));
        setYaw(MathHelper.lerp(0.2F, this.prevYaw, this.getYaw()));
        float drag;
        if (this.isTouchingWater()) {
            for (int i = 0; i < 4; ++i) {
                this.world.addParticle(ParticleTypes.BUBBLE, pos.x - ourMotion.x * 0.25D, pos.y - ourMotion.y * 0.25D, pos.z - ourMotion.z * 0.25D, ourMotion.x, ourMotion.y, ourMotion.z);
            }

            drag = 0.8F;
        } else drag = 0.99F;

        this.setVelocity(ourMotion.multiply(drag));

        pos = getPos();
        this.setPos(pos.x, pos.y, pos.z);

        if (!isAlive())
            return;

        ItemStack stack = getStack();

        if (dataTracker.get(NETHERITE_SYNCED)) {
            if (Math.random() < 0.4)
                this.world.addParticle(ParticleTypes.FLAME,
                        pos.x - ourMotion.x * 0.25D + (Math.random() - 0.5) * 0.4,
                        pos.y - ourMotion.y * 0.25D + (Math.random() - 0.5) * 0.4,
                        pos.z - ourMotion.z * 0.25D + (Math.random() - 0.5) * 0.4,
                        (Math.random() - 0.5) * 0.1,
                        (Math.random() - 0.5) * 0.1,
                        (Math.random() - 0.5) * 0.1);
        } else if (!world.isClient && netherite)
            dataTracker.set(NETHERITE_SYNCED, true);

        boolean returning = dataTracker.get(RETURNING);
        liveTime++;

        LivingEntity owner = getThrower();
        if (owner == null || !owner.isAlive() || !(owner instanceof PlayerEntity)) {
            if (!world.isClient) {
                while (isInsideWall())
                    setPos(getX(), getY() + 1, getZ());

                dropStack(stack, 0);
                discard();
            }

            return;
        }

        if (!returning) {
            if (liveTime > (netherite ? QuarkPickarangFabric.netheriteTimeout : QuarkPickarangFabric.timeout))
                setReturning();
            if (!world.getWorldBorder().contains(getBoundingBox()))
                spark();
        } else {
            noClip = true;

            int eff = getEfficiencyModifier();

            List<ItemEntity> items = world.getNonSpectatingEntities(ItemEntity.class, getBoundingBox().expand(2));
            List<ExperienceOrbEntity> xp = world.getNonSpectatingEntities(ExperienceOrbEntity.class, getBoundingBox().expand(2));

            Vec3d ourPos = getPos();
            for (ItemEntity item : items) {
                if (item.hasVehicle())
                    continue;
                item.startRiding(this);

                item.setPickupDelay(5);
            }

            for (ExperienceOrbEntity xpOrb : xp) {
                if (xpOrb.hasVehicle())
                    continue;
                xpOrb.startRiding(this);
            }

            Vec3d ownerPos = owner.getPos().add(0, 1, 0);
            Vec3d motion = ownerPos.subtract(ourPos);
            double motionMag = 3.25 + eff * 0.25;

            if (motion.lengthSquared() < motionMag) {
                PlayerEntity player = (PlayerEntity) owner;
                PlayerInventory inventory = player.getInventory();
                ItemStack stackInSlot = inventory.getStack(slot);

                if (!world.isClient) {
                    playSound(QuarkPickarangFabric.ENTITY_PICKARANG_PICKUP, 1, 1);

                    if (!stack.isEmpty()) if (player.isAlive() && stackInSlot.isEmpty())
                        inventory.setStack(slot, stack);
                    else if (!player.isAlive() || !inventory.insertStack(stack))
                        player.dropItem(stack, false);

                    if (player.isAlive()) {
                        for (ItemEntity item : items)
                            if (item.isAlive())
                                giveItemToPlayer(player, item);

                        for (ExperienceOrbEntity xpOrb : xp)
                            if (xpOrb.isAlive())
                                xpOrb.onPlayerCollision(player);

                        for (Entity riding : getPassengerList()) {
                            if (!riding.isAlive())
                                continue;

                            if (riding instanceof ItemEntity)
                                giveItemToPlayer(player, (ItemEntity) riding);
                            else if (riding instanceof ExperienceOrbEntity)
                                riding.onPlayerCollision(player);
                        }
                    }

                    discard();
                }
            } else
                setVelocity(motion.normalize().multiply(0.7 + eff * 0.325F));
        }
    }

    private void giveItemToPlayer(PlayerEntity player, ItemEntity itemEntity) {
        itemEntity.setPickupDelay(0);
        itemEntity.onPlayerCollision(player);

        if (itemEntity.isAlive()) {
            // Player could not pick up everything
            ItemStack drop = itemEntity.getStack();
            player.dropItem(drop, false);
            itemEntity.discard();
        }
    }

    @Nullable
    public LivingEntity getThrower() {
        if (this.owner == null && this.ownerId != null && this.world instanceof ServerWorld) {
            Entity entity = ((ServerWorld)this.world).getEntity(this.ownerId);
            if (entity instanceof LivingEntity) {
                this.owner = (LivingEntity)entity;
            } else {
                this.ownerId = null;
            }
        }

        return this.owner;
    }

    @Override
    protected boolean canAddPassenger(@Nonnull Entity passenger) {
        return super.canAddPassenger(passenger) || passenger instanceof ItemEntity || passenger instanceof ExperienceOrbEntity;
    }

    @Override
    public double getMountedHeightOffset() {
        return 0;
    }

    @Nonnull
    @Override
    public SoundCategory getSoundCategory() {
        return SoundCategory.PLAYERS;
    }

    public int getEfficiencyModifier() {
        return EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, getStack());
    }

    public int getPiercingModifier() {
        return EnchantmentHelper.getLevel(Enchantments.PIERCING, getStack());
    }

    public ItemStack getStack() {
        return dataTracker.get(STACK);
    }

    public void setStack(ItemStack stack) {
        dataTracker.set(STACK, stack);
    }

    @Override
    public void readCustomDataFromNbt(@Nonnull NbtCompound compound) {
        dataTracker.set(RETURNING, compound.getBoolean(TAG_RETURNING));
        liveTime = compound.getInt(TAG_LIVE_TIME);
        blockHitCount = compound.getInt(TAG_BLOCKS_BROKEN);
        slot = compound.getInt(TAG_RETURN_SLOT);

        if (compound.contains(TAG_ITEM_STACK))
            setStack(ItemStack.fromNbt(compound.getCompound(TAG_ITEM_STACK)));
        else
            setStack(new ItemStack(QuarkPickarangFabric.PICKARANG));

        if (compound.contains("owner", 10)) {
            NbtElement owner = compound.get("owner");
            if (owner != null)
                this.ownerId = NbtHelper.toUuid(owner);
        }

        netherite = compound.getBoolean(TAG_NETHERITE);
    }

    @Override
    public void writeCustomDataToNbt(@Nonnull NbtCompound compound) {
        compound.putBoolean(TAG_RETURNING, dataTracker.get(RETURNING));
        compound.putInt(TAG_LIVE_TIME, liveTime);
        compound.putInt(TAG_BLOCKS_BROKEN, blockHitCount);
        compound.putInt(TAG_RETURN_SLOT, slot);

        compound.put(TAG_ITEM_STACK, getStack().writeNbt(new NbtCompound()));
        if (this.ownerId != null)
            compound.put("owner", NbtHelper.fromUuid(this.ownerId));

        compound.putBoolean(TAG_NETHERITE, netherite);
    }

    @Override
    public Packet<?> createSpawnPacket() {
        return new EntitySpawnS2CPacket(this, owner == null ? 0 : owner.getId());
    }
}