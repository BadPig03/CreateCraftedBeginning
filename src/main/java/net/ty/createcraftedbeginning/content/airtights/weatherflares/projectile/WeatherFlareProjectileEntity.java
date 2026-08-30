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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.ty.createcraftedbeginning.api.gascanisters.GasConsumptions;
import net.ty.createcraftedbeginning.api.weatherflares.IWeatherFlare;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.ty.createcraftedbeginning.registry.CCBAdvancements;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBItems;
import net.ty.createcraftedbeginning.foundation.CCBMathUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WeatherFlareProjectileEntity extends AbstractHurtingProjectile implements ItemSupplier, IEntityWithComplexSpawn {
    private static final double MIN_DELTA_MOVEMENT_LENGTH = 0.01;
    private static final double MIN_DELTA_MOVEMENT_LENGTH_SQR = MIN_DELTA_MOVEMENT_LENGTH * MIN_DELTA_MOVEMENT_LENGTH;
    private static final double MIN_WEATHER_DURATION_RATIO = MIN_DELTA_MOVEMENT_LENGTH;
    private static final float DEFAULT_SIZE = 0.25f;
    private static final float INERTIA = 0.95f;
    private static final int DEFAULT_Y = 32;
    private static final int MAX_LIFE_TIME = 1800;
    private static final String COMPOUND_KEY_ITEM = "Item";
    private static final String COMPOUND_KEY_LIFE_TIME = "LifeTime";
    private static final String COMPOUND_KEY_START_Y = "StartY";
    private static final String COMPOUND_KEY_COPIED = "Copied";

    private ItemStack itemStack;
    private int lifeTime;
    private double startY;
    private boolean copied;

    public WeatherFlareProjectileEntity(EntityType<WeatherFlareProjectileEntity> entityType, Level level) {
        super(entityType, level);
        accelerationPower = 0;
        itemStack = getDefaultItem();
    }

    public WeatherFlareProjectileEntity(Level level, Item flareItem, double startY) {
        super(CCBEntityTypes.WEATHER_FLARE_PROJECTILE.get(), level);
        accelerationPower = 0;
        itemStack = new ItemStack(flareItem);
        this.startY = startY;
    }

    @Contract(" -> new")
    private static ItemStack getDefaultItem() {
        return new ItemStack(CCBItems.SUNNY_FLARE.asItem());
    }

    public static void build(EntityType.Builder<WeatherFlareProjectileEntity> builder) {
        builder.sized(DEFAULT_SIZE, DEFAULT_SIZE).eyeHeight(0);
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
        super.tick();
        Level level = level();
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
        return INERTIA * INERTIA;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_ITEM, itemStack.save(registryAccess()));
        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_LIFE_TIME, lifeTime);
        CCBNbtUtils.putDouble(compoundTag, COMPOUND_KEY_START_Y, startY);
        CCBNbtUtils.putBoolean(compoundTag, COMPOUND_KEY_COPIED, copied);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_ITEM, Tag.TAG_COMPOUND)) {
            itemStack = ItemStack.parse(registryAccess(), CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_ITEM)).orElseGet(WeatherFlareProjectileEntity::getDefaultItem);
        }
        else {
            itemStack = getDefaultItem();
        }
        lifeTime = CCBMathUtils.clampNonNegative(CCBNbtUtils.getIntOrDefault(compoundTag, COMPOUND_KEY_LIFE_TIME, lifeTime), MAX_LIFE_TIME);
        double storedStartY = CCBNbtUtils.getDoubleOrDefault(compoundTag, COMPOUND_KEY_START_Y, startY);
        if (GasConsumptions.isFinite(storedStartY)) {
            startY = storedStartY;
        }
        copied = CCBNbtUtils.getBooleanOrDefault(compoundTag, COMPOUND_KEY_COPIED, copied);
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
        super.onHitEntity(hitResult);
        destroy();
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        destroy();
    }

    @Override
    public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(LivingEntity target, DamageSource damageSource) {
        return DoubleDoubleImmutablePair.of(target.position().x - position().x, target.position().z - position().z);
    }

    public void setCopied(boolean copied) {
        this.copied = copied;
    }

    private void explode() {
        if (!(level() instanceof ServerLevel level) || !(itemStack.getItem() instanceof IWeatherFlare flare)) {
            return;
        }

        Vec3 flarePos = position();
        boolean wasStormy = level.isRaining() || level.isThundering();
        level.explode(null, flarePos.x, flarePos.y, flarePos.z, 0, ExplosionInteraction.NONE);
        double weatherDurationRatio = Mth.clamp((flarePos.y - startY) / DEFAULT_Y, MIN_WEATHER_DURATION_RATIO, 16);
        flare.setWeather(level, weatherDurationRatio);
        grantAdvancements(level, wasStormy);
        discard();
    }

    private void grantAdvancements(ServerLevel level, boolean wasStormy) {
        if (!(getOwner() instanceof Player player)) {
            return;
        }

        if (wasStormy && itemStack.is(CCBItems.SUNNY_FLARE)) {
            CCBAdvancements.LOOKS_LIKE_THE_WEATHERS_CLEARING_UP.awardTo(player);
        }

        boolean isWeatherCycleDisabled = !level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).get();
        boolean isStormAnchored = level.isThundering() && isWeatherCycleDisabled && (itemStack.is(CCBItems.ANCHOR_FLARE) || itemStack.is(CCBItems.THUNDERSTORM_FLARE));
        if (!isStormAnchored) {
            return;
        }

        CCBAdvancements.I_AM_THE_STORM_THAT_IS_APPROACHING.awardTo(player);
    }

    private void destroy() {
        Level level = level();
        if (level.isClientSide) {
            return;
        }

        Vec3 projectilePos = position();
        if (copied) {
            level.explode(null, projectilePos.x, projectilePos.y, projectilePos.z, 0, ExplosionInteraction.NONE);
        }
        else {
            level.addFreshEntity(new ItemEntity(level, projectilePos.x, projectilePos.y, projectilePos.z, itemStack.copy()));
        }
        discard();
    }
}
