package net.ty.createcraftedbeginning.content.airtights.weatherflares.projectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.ty.createcraftedbeginning.api.canister.GasConsumptionMath;
import net.ty.createcraftedbeginning.api.weatherflares.VanillaWeatherFlareHandler;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlare;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareContext;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareEffect;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareHandler;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareHandlers;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareQueries;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareResult;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import net.ty.createcraftedbeginning.gas.interaction.GasInteractionFeedback;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WeatherFlareProjectileEntity extends AbstractHurtingProjectile implements ItemSupplier, IEntityWithComplexSpawn {
    private static final double MIN_DELTA_MOVEMENT_LENGTH = 0.01;
    private static final double MIN_DELTA_MOVEMENT_LENGTH_SQR = MIN_DELTA_MOVEMENT_LENGTH * MIN_DELTA_MOVEMENT_LENGTH;
    private static final double MIN_WEATHER_DURATION_RATIO = MIN_DELTA_MOVEMENT_LENGTH;
    private static final float DEFAULT_SIZE = 0.25F;
    private static final float INERTIA = 0.95F;
    private static final int DEFAULT_Y = 32;
    private static final int MAX_LIFE_TIME = 1800;
    private static final int RECOVERY_RETRY_INTERVAL = 20;
    private static final String COMPOUND_KEY_ITEM = "Item";
    private static final String COMPOUND_KEY_LIFE_TIME = "LifeTime";
    private static final String COMPOUND_KEY_START_Y = "StartY";
    private static final String COMPOUND_KEY_COPIED = "Copied";
    private static final String COMPOUND_KEY_RECOVERING = "Recovering";

    private ItemStack itemStack;
    private int lifeTime;
    private double startY;
    private boolean copied;
    private boolean recovering;
    private boolean resolving;
    private int recoveryTicks;

    public WeatherFlareProjectileEntity(EntityType<WeatherFlareProjectileEntity> entityType, Level level) {
        super(entityType, level);
        accelerationPower = 0;
        itemStack = getDefaultItem();
    }

    public WeatherFlareProjectileEntity(Level level, ItemStack flareStack, double startY) {
        super(CCBEntityTypes.WEATHER_FLARE_PROJECTILE.get(), level);
        accelerationPower = 0;
        itemStack = flareStack.copyWithCount(1);
        this.startY = startY;
    }

    public static void build(EntityType.Builder<WeatherFlareProjectileEntity> builder) {
        builder.sized(DEFAULT_SIZE, DEFAULT_SIZE).eyeHeight(0);
    }

    @Contract(" -> new")
    private static ItemStack getDefaultItem() {
        return new ItemStack(CCBItems.SUNNY_FLARE.asItem());
    }

    @Override
    public void push(double x, double y, double z) {
    }

    @Override
    public boolean shouldRender(double x, double y, double z) {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return true;
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    @Override
    protected void defineSynchedData(Builder builder) {
    }

    @Override
    public void tick() {
        if (isRemoved() || resolving) {
            return;
        }

        if (!WeatherFlareQueries.isValidFlare(itemStack)) {
            discard();
            return;
        }

        Level level = level();
        if (recovering) {
            if (level.isClientSide || --recoveryTicks > 0) {
                return;
            }

            recoverItem();
            return;
        }

        super.tick();
        if (isRemoved() || resolving) {
            return;
        }

        if (recovering) {
            setDeltaMovement(Vec3.ZERO);
            return;
        }

        if (level.isClientSide) {
            level.addParticle(ParticleTypes.END_ROD, getX(), getY() + 0.15, getZ(), 0, 0, 0);
            return;
        }

        if (getBlockY() >= level.getMaxBuildHeight()) {
            explode();
            return;
        }

        if (++lifeTime > MAX_LIFE_TIME) {
            destroy();
            return;
        }

        if (getDeltaMovement().lengthSqr() >= MIN_DELTA_MOVEMENT_LENGTH_SQR) {
            return;
        }

        explode();
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        return super.canHitEntity(target);
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
        return Mth.square(INERTIA);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.put(COMPOUND_KEY_ITEM, itemStack.saveOptional(registryAccess()));
        compoundTag.putInt(COMPOUND_KEY_LIFE_TIME, lifeTime);
        compoundTag.putDouble(COMPOUND_KEY_START_Y, startY);
        compoundTag.putBoolean(COMPOUND_KEY_COPIED, copied);
        compoundTag.putBoolean(COMPOUND_KEY_RECOVERING, recovering);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(COMPOUND_KEY_ITEM, Tag.TAG_COMPOUND)) {
            itemStack = ItemStack.parseOptional(registryAccess(), compoundTag.getCompound(COMPOUND_KEY_ITEM));
        }
        else {
            itemStack = getDefaultItem();
        }
        if (WeatherFlareQueries.isValidFlare(itemStack)) {
            itemStack = itemStack.copyWithCount(1);
        }
        else {
            itemStack = ItemStack.EMPTY;
        }
        lifeTime = Mth.clamp(NbtValues.getIntOrDefault(compoundTag, COMPOUND_KEY_LIFE_TIME, lifeTime), 0, MAX_LIFE_TIME);
        double storedStartY = NbtValues.getDoubleOrDefault(compoundTag, COMPOUND_KEY_START_Y, startY);
        if (GasConsumptionMath.isFinite(storedStartY)) {
            startY = storedStartY;
        }
        copied = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_COPIED, copied);
        recovering = NbtValues.getBooleanOrDefault(compoundTag, COMPOUND_KEY_RECOVERING, recovering);
        recoveryTicks = 0;
        if (!recovering) {
            return;
        }

        setDeltaMovement(Vec3.ZERO);
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        CompoundTag spawnTag = new CompoundTag();
        addAdditionalSaveData(spawnTag);
        buffer.writeNbt(spawnTag);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        CompoundTag spawnTag = buffer.readNbt();
        if (spawnTag == null) {
            return;
        }

        readAdditionalSaveData(spawnTag);
    }

    @Override
    public ItemStack getItem() {
        return itemStack;
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        if (isRemoved() || recovering || resolving) {
            return;
        }

        super.onHitEntity(hitResult);
        destroy();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        if (isRemoved() || recovering || resolving) {
            return;
        }

        super.onHitBlock(hitResult);
        destroy();
    }

    @Override
    public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity target, DamageSource damageSource) {
        Vec3 targetPosition = target.position();
        Vec3 sourcePosition = position();
        return DoubleDoubleImmutablePair.of(targetPosition.x - sourcePosition.x, targetPosition.z - sourcePosition.z);
    }

    public void setCopied(boolean copied) {
        this.copied = copied;
    }

    private void explode() {
        if (isRemoved() || recovering || resolving || !(level() instanceof ServerLevel level)) {
            return;
        }

        if (!(itemStack.getItem() instanceof WeatherFlare flare)) {
            discard();
            return;
        }

        Vec3 flarePos = position();
        double weatherDurationRatio = Mth.clamp((flarePos.y - startY) / DEFAULT_Y, MIN_WEATHER_DURATION_RATIO, 16);
        WeatherFlareEffect effect = flare.getWeatherEffect();
        WeatherFlareResult result;
        resolving = true;
        try {
            if (effect == null) {
                result = VanillaWeatherFlareHandler.applyDirect(level, itemStack, weatherDurationRatio);
            }
            else {
                WeatherFlareHandler handler = WeatherFlareHandlers.resolve(level.dimension());
                if (handler == null || !handler.supports(level, flarePos, effect)) {
                    result = WeatherFlareResult.REJECTED;
                }
                else {
                    result = Objects.requireNonNull(handler.apply(new WeatherFlareContext(level, flarePos, effect, itemStack, weatherDurationRatio)), "Weather flare handler result must not be null.");
                }
            }
        }
        catch (RuntimeException | Error failure) {
            discard();
            throw failure;
        }
        finally {
            resolving = false;
        }
        if (!result.applied()) {
            destroy();
            if (getOwner() instanceof Player player) {
                GasInteractionFeedback.sendWarningFeedback(player, "gui.airtight_cannon.weather_not_applied");
            }
            return;
        }

        discard();
        grantAdvancements(result);
        level.explode(null, flarePos.x, flarePos.y, flarePos.z, 0, ExplosionInteraction.NONE);
    }

    private void grantAdvancements(WeatherFlareResult result) {
        if (!(getOwner() instanceof Player player)) {
            return;
        }

        if (result.clearedStorm() && itemStack.is(CCBItems.SUNNY_FLARE)) {
            CCBAdvancements.LOOKS_LIKE_THE_WEATHERS_CLEARING_UP.awardTo(player);
        }

        if (!result.anchoredStorm() || !itemStack.is(CCBItems.ANCHOR_FLARE) && !itemStack.is(CCBItems.THUNDERSTORM_FLARE)) {
            return;
        }

        CCBAdvancements.I_AM_THE_STORM_THAT_IS_APPROACHING.awardTo(player);
    }

    private void destroy() {
        Level level = level();
        if (level.isClientSide || isRemoved() || recovering || resolving) {
            return;
        }

        if (copied) {
            Vec3 projectilePos = position();
            discard();
            level.explode(null, projectilePos.x, projectilePos.y, projectilePos.z, 0, ExplosionInteraction.NONE);
            return;
        }

        recovering = true;
        setDeltaMovement(Vec3.ZERO);
        recoverItem();
    }

    private void recoverItem() {
        recoveryTicks = RECOVERY_RETRY_INTERVAL;
        Level level = level();
        Vec3 projectilePos = position();
        if (!level.addFreshEntity(new ItemEntity(level, projectilePos.x, projectilePos.y, projectilePos.z, itemStack.copy()))) {
            return;
        }

        discard();
    }
}
