package net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge;

import com.mojang.datafixers.util.Pair;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonShotContext;
import net.ty.createcraftedbeginning.api.cannonhandlers.visual.AirtightCannonVisualHandlerUtils;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightCannonWindChargeProjectileEntity extends AbstractWindCharge implements IEntityWithComplexSpawn {
    private static final String COMPOUND_KEY_GAS_HOLDER = "GasHolder";
    private static final String COMPOUND_KEY_MULTIPLIER = "Multiplier";
    private static final String COMPOUND_KEY_KNOCKBACK = "Knockback";
    private static final String COMPOUND_KEY_FLAME = "Flame";
    private static final String COMPOUND_KEY_INIT_MOTION = "InitMotion";
    private static final float DEFAULT_SIZE = 0.3125f;
    private static final int MOTION_CHECK_INTERVAL = 5;
    private static final double MIN_INITIAL_MOTION_SQR = 1.0e-8;
    private static final double EXTERNAL_IMPULSE_THRESHOLD = 0.05;
    private static final double EXTERNAL_IMPULSE_THRESHOLD_SQR = EXTERNAL_IMPULSE_THRESHOLD * EXTERNAL_IMPULSE_THRESHOLD;

    private Holder<Gas> gasHolder = Gas.EMPTY_GAS_HOLDER;
    private float multiplier = 1.0f;
    private float knockback = 0.1f;
    private boolean flame;
    private Vec3 initMotion = Vec3.ZERO;

    public AirtightCannonWindChargeProjectileEntity(EntityType<? extends AbstractWindCharge> entityType, Level level) {
        super(entityType, level);
    }

    public AirtightCannonWindChargeProjectileEntity(Level level, Holder<Gas> gasHolder, Vec3 initMotion) {
        super(CCBEntityTypes.AIRTIGHT_CANNON_WIND_CHARGE_PROJECTILE.get(), level);
        this.gasHolder = gasHolder;
        this.initMotion = initMotion;
    }

    public static void playLaunchSound(Level level, Vec3 location, float pitch) {
        CCBSoundEvents.WIND_CHARGE_LAUNCH.playAt(level, location, 1, pitch, true);
    }

    @SuppressWarnings("unchecked")
    public static Builder<?> build(Builder<?> builder) {
        Builder<AirtightCannonWindChargeProjectileEntity> typedBuilder = (Builder<AirtightCannonWindChargeProjectileEntity>) builder;
        return typedBuilder.sized(DEFAULT_SIZE, DEFAULT_SIZE).eyeHeight(0);
    }

    public Holder<Gas> getGasHolder() {
        return gasHolder;
    }

    public void setMultiplier(float multiplier) {
        this.multiplier = multiplier;
    }

    public void setKnockback(float knockback) {
        this.knockback = knockback;
    }

    public void setFlame(boolean flame) {
        this.flame = flame;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.put(COMPOUND_KEY_INIT_MOTION, createMotionTag());
        tag.putFloat(COMPOUND_KEY_MULTIPLIER, multiplier);
        tag.putFloat(COMPOUND_KEY_KNOCKBACK, knockback);
        tag.putBoolean(COMPOUND_KEY_FLAME, flame);
        Gas.HOLDER_CODEC.encodeStart(NbtOps.INSTANCE, gasHolder).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to encode gas holder: {}", err)).ifPresent(gasTag -> tag.put(COMPOUND_KEY_GAS_HOLDER, gasTag));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        readInitialMotion(tag);

        if (tag.contains(COMPOUND_KEY_MULTIPLIER)) {
            float storedMultiplier = tag.getFloat(COMPOUND_KEY_MULTIPLIER);
            if (Float.isFinite(storedMultiplier) && storedMultiplier > 0) {
                multiplier = storedMultiplier;
            }
        }
        if (tag.contains(COMPOUND_KEY_KNOCKBACK)) {
            float storedKnockback = tag.getFloat(COMPOUND_KEY_KNOCKBACK);
            if (Float.isFinite(storedKnockback) && storedKnockback >= 0) {
                knockback = storedKnockback;
            }
        }
        if (tag.contains(COMPOUND_KEY_FLAME)) {
            flame = tag.getBoolean(COMPOUND_KEY_FLAME);
        }

        readGasHolder(tag);
    }

    @Override
    protected void onDeflection(@Nullable Entity entity, boolean deflectedByPlayer) {
        explodeDirectly(position());
        discard();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level().isClientSide || !flame || !(result.getEntity() instanceof LivingEntity entity) || entity.fireImmune()) {
            return;
        }

        entity.igniteForTicks(100);
    }

    @Override
    protected void explode(Vec3 pos) {
        explodeDirectly(pos);
    }

    @Override
    protected boolean shouldBurn() {
        return flame;
    }

    @Override
    public void tick() {
        super.tick();
        if (isRemoved()) {
            return;
        }

        Vec3 pos = position();
        Level level = level();
        if (level.isClientSide) {
            AirtightCannonVisualHandlerUtils.of(gasHolder.value()).renderTrailParticles(level, pos);
            return;
        }
        if (tickCount <= 1 || tickCount % MOTION_CHECK_INTERVAL != 0 || !hasSignificantExternalImpulse()) {
            return;
        }

        explodeDirectly(pos);
        discard();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (source.is(DamageTypes.ON_FIRE)) {
            return false;
        }

        if (level().isClientSide) {
            return super.hurt(source, amount);
        }

        explodeDirectly(position());
        discard();
        return true;
    }

    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = new CompoundTag();
        addAdditionalSaveData(tag);
        buffer.writeNbt(tag);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf buffer) {
        CompoundTag tag = buffer.readNbt();
        if (tag == null) {
            return;
        }

        readAdditionalSaveData(tag);
    }

    private boolean hasSignificantExternalImpulse() {
        double initialSpeedSqr = initMotion.lengthSqr();
        if (initialSpeedSqr <= MIN_INITIAL_MOTION_SQR) {
            return false;
        }

        Vec3 currentMotion = getDeltaMovement();
        if (!Double.isFinite(currentMotion.x()) || !Double.isFinite(currentMotion.y()) || !Double.isFinite(currentMotion.z())) {
            return true;
        }

        double initialSpeed = Math.sqrt(initialSpeedSqr);
        Vec3 initialDirection = initMotion.scale(1.0 / initialSpeed);
        double parallelSpeed = currentMotion.dot(initialDirection);
        Vec3 lateralMotion = currentMotion.subtract(initialDirection.scale(parallelSpeed));
        boolean hasLateralImpulse = lateralMotion.lengthSqr() > EXTERNAL_IMPULSE_THRESHOLD_SQR;
        boolean hasParallelImpulse = parallelSpeed > initialSpeed + EXTERNAL_IMPULSE_THRESHOLD || parallelSpeed < -EXTERNAL_IMPULSE_THRESHOLD;
        return hasLateralImpulse || hasParallelImpulse;
    }

    private ListTag createMotionTag() {
        ListTag motionTag = new ListTag();
        motionTag.add(DoubleTag.valueOf(initMotion.x()));
        motionTag.add(DoubleTag.valueOf(initMotion.y()));
        motionTag.add(DoubleTag.valueOf(initMotion.z()));
        return motionTag;
    }

    private void readInitialMotion(CompoundTag tag) {
        if (!tag.contains(COMPOUND_KEY_INIT_MOTION)) {
            return;
        }

        ListTag motionTag = tag.getList(COMPOUND_KEY_INIT_MOTION, Tag.TAG_DOUBLE);
        if (motionTag.size() < 3) {
            return;
        }

        double x = motionTag.getDouble(0);
        double y = motionTag.getDouble(1);
        double z = motionTag.getDouble(2);
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            return;
        }

        initMotion = new Vec3(x, y, z);
    }

    private void readGasHolder(CompoundTag tag) {
        Tag gasTag = tag.get(COMPOUND_KEY_GAS_HOLDER);
        if (gasTag == null) {
            return;
        }

        Gas.HOLDER_CODEC.decode(NbtOps.INSTANCE, gasTag).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to decode gas holder: {}", err)).map(Pair::getFirst).ifPresent(holder -> gasHolder = holder);
    }

    private void explodeDirectly(Vec3 pos) {
        AirtightCannonHandler handler = AirtightCannonHandlerUtils.of(gasHolder.value());
        AirtightCannonShotContext context = new AirtightCannonShotContext(this, getOwner(), gasHolder, multiplier, knockback, flame);
        handler.explode(level(), pos, context);
    }
}