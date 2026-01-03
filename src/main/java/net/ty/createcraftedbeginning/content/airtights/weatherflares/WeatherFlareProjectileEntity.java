package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.syncher.SynchedEntityData.Builder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class WeatherFlareProjectileEntity extends AbstractHurtingProjectile implements ItemSupplier, IEntityWithComplexSpawn {
    private static final float DEFAULT_SIZE = 0.25f;
    private static final float INERTIA = 0.95f;
    private static final double MIN_DELTA_MOVEMENT_LENGTH = 0.01;
    private static final int DEFAULT_Y = 32;
    private static final int MAX_LIFE_TIME = 1800;

    private static final String COMPOUND_KEY_ITEM = "Item";
    private static final String COMPOUND_KEY_LIFE_TIME = "LifeTime";
    private static final String COMPOUND_KEY_START_Y = "StartY";

    private ItemStack itemStack;
    private int lifeTime;
    private double startY;

    public WeatherFlareProjectileEntity(EntityType<? extends AbstractHurtingProjectile> entityType, Level level) {
        super(entityType, level);
        accelerationPower = 0;
        itemStack = getDefaultItem();
    }

    public WeatherFlareProjectileEntity(Level level, @NotNull Item item, double startY) {
        super(CCBEntityTypes.WEATHER_FLARE_PROJECTILE.get(), level);
        accelerationPower = 0;
        itemStack = new ItemStack(item);
        this.startY = startY;
    }

    @Contract(" -> new")
    private static @NotNull ItemStack getDefaultItem() {
        return new ItemStack(CCBItems.SUNNY_FLARE.asItem());
    }

    @SuppressWarnings("unchecked")
    public static @NotNull EntityType.Builder<?> build(EntityType.Builder<?> builder) {
        EntityType.Builder<WeatherFlareProjectileEntity> entityBuilder = (EntityType.Builder<WeatherFlareProjectileEntity>) builder;
        return entityBuilder.sized(DEFAULT_SIZE, DEFAULT_SIZE).eyeHeight(0);
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean canCollideWith(@NotNull Entity entity) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void defineSynchedData(@NotNull Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Level level = level();
        if (level.isClientSide) {
            return;
        }

        if (getBlockY() > level.getMaxBuildHeight() + 30 || ++lifeTime > MAX_LIFE_TIME) {
            destroy();
            return;
        }
        if (getDeltaMovement().length() >= MIN_DELTA_MOVEMENT_LENGTH) {
            return;
        }

        explode();
    }

    @Override
    protected boolean canHitEntity(@NotNull Entity target) {
        return true;
    }

    @Override
    protected boolean shouldBurn() {
        return false;
    }

    @Nullable
    @Override
    protected ParticleOptions getTrailParticle() {
        return null;
    }

    @Override
    protected float getInertia() {
        return INERTIA;
    }

    @Override
    protected float getLiquidInertia() {
        return INERTIA * INERTIA;
    }

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.put(COMPOUND_KEY_ITEM, itemStack.save(registryAccess()));
        compoundTag.putInt(COMPOUND_KEY_LIFE_TIME, lifeTime);
        compoundTag.putDouble(COMPOUND_KEY_START_Y, startY);
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(COMPOUND_KEY_ITEM)) {
            itemStack = ItemStack.parse(registryAccess(), compoundTag.getCompound(COMPOUND_KEY_ITEM)).orElseGet(WeatherFlareProjectileEntity::getDefaultItem);
        }
        else {
            itemStack = getDefaultItem();
        }
        if (compoundTag.contains(COMPOUND_KEY_LIFE_TIME)) {
            lifeTime = compoundTag.getInt(COMPOUND_KEY_LIFE_TIME);
        }
        if (compoundTag.contains(COMPOUND_KEY_START_Y)) {
            startY = compoundTag.getDouble(COMPOUND_KEY_START_Y);
        }
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        CompoundTag compoundTag = new CompoundTag();
        addAdditionalSaveData(compoundTag);
        buffer.writeNbt(compoundTag);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf additionalData) {
        CompoundTag compoundTag = additionalData.readNbt();
        if (compoundTag == null) {
            return;
        }
        readAdditionalSaveData(compoundTag);
    }

    @Override
    public @NotNull ItemStack getItem() {
        return itemStack;
    }

    @Override
    protected void onHitEntity(@NotNull EntityHitResult result) {
        super.onHitEntity(result);
        destroy();
    }

    @Override
    protected void onHitBlock(@NotNull BlockHitResult result) {
        super.onHitBlock(result);
        destroy();
    }

    @Override
    public @NotNull DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(@NotNull LivingEntity entity, @NotNull DamageSource damageSource) {
        return DoubleDoubleImmutablePair.of(entity.position().x - position().x, entity.position().z - position().z);
    }

    private void explode() {
        Level level = level();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel) || !(itemStack.getItem() instanceof IWeatherFlare flareItem)) {
            return;
        }

        Vec3 pos = position();
        serverLevel.explode(null, pos.x, pos.y, pos.z, 1, ExplosionInteraction.NONE);
        flareItem.setWeather(serverLevel, Mth.clamp((getY() - startY) / DEFAULT_Y, MIN_DELTA_MOVEMENT_LENGTH, 16));
        grantAdvancements(serverLevel);
        discard();
    }

    private void grantAdvancements(ServerLevel serverLevel) {
        if (!(getOwner() instanceof Player player)) {
            return;
        }

        if ((serverLevel.isRaining() || serverLevel.isThundering()) && itemStack.is(CCBItems.SUNNY_FLARE)) {
            CCBAdvancements.THE_SKIES_ARE_CLEARING_UP_NOW.awardTo(player);
        }
        if (!serverLevel.isThundering() && itemStack.is(CCBItems.THUNDERSTORM_FLARE)) {
            CCBAdvancements.I_AM_THE_STORM_THAT_IS_APPROACHING.awardTo(player);
        }
    }

    private void destroy() {
        Level level = level();
        if (level.isClientSide) {
            return;
        }

        Vec3 pos = position();
        level.addFreshEntity(new ItemEntity(level, pos.x, pos.y, pos.z, itemStack.copy()));
        discard();
    }
}
