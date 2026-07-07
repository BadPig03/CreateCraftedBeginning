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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
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

    @Nullable
    @OnlyIn(Dist.CLIENT)
    private AirtightCannonWindChargeModel windChargeModel;
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
        if (!level.isClientSide) {
            return;
        }

        setModelFromGas(gasHolder.value());
    }

    public static void playLaunchSound(Level world, Vec3 location, float pitch) {
        CCBSoundEvents.WIND_CHARGE_LAUNCH.playAt(world, location, 1, pitch, true);
    }

    @SuppressWarnings("unchecked")
    public static Builder<?> build(Builder<?> builder) {
        Builder<AirtightCannonWindChargeProjectileEntity> entityBuilder = (Builder<AirtightCannonWindChargeProjectileEntity>) builder;
        return entityBuilder.sized(DEFAULT_SIZE, DEFAULT_SIZE).eyeHeight(0);
    }

    @Nullable
    @OnlyIn(Dist.CLIENT)
    public AirtightCannonWindChargeModel getWindChargeModel() {
        return windChargeModel;
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
    public void addAdditionalSaveData(CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        ListTag motionTag = new ListTag();
        motionTag.add(DoubleTag.valueOf(initMotion.x()));
        motionTag.add(DoubleTag.valueOf(initMotion.y()));
        motionTag.add(DoubleTag.valueOf(initMotion.z()));
        compoundTag.put(COMPOUND_KEY_INIT_MOTION, motionTag);
        compoundTag.putFloat(COMPOUND_KEY_MULTIPLIER, multiplier);
        compoundTag.putFloat(COMPOUND_KEY_KNOCKBACK, knockback);
        compoundTag.putBoolean(COMPOUND_KEY_FLAME, flame);
        Gas.HOLDER_CODEC.encodeStart(NbtOps.INSTANCE, gasHolder).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to encode gas holder: {}", err)).ifPresent(tag -> compoundTag.put(COMPOUND_KEY_GAS_HOLDER, tag));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(COMPOUND_KEY_INIT_MOTION)) {
            ListTag motionTag = compoundTag.getList(COMPOUND_KEY_INIT_MOTION, Tag.TAG_DOUBLE);
            initMotion = new Vec3(motionTag.getDouble(0), motionTag.getDouble(1), motionTag.getDouble(2));
        }
        if (compoundTag.contains(COMPOUND_KEY_MULTIPLIER)) {
            multiplier = compoundTag.getFloat(COMPOUND_KEY_MULTIPLIER);
        }
        if (compoundTag.contains(COMPOUND_KEY_KNOCKBACK)) {
            knockback = compoundTag.getFloat(COMPOUND_KEY_KNOCKBACK);
        }
        if (compoundTag.contains(COMPOUND_KEY_FLAME)) {
            flame = compoundTag.getBoolean(COMPOUND_KEY_FLAME);
        }
        if (compoundTag.contains(COMPOUND_KEY_GAS_HOLDER)) {
            Gas.HOLDER_CODEC.decode(NbtOps.INSTANCE, compoundTag.get(COMPOUND_KEY_GAS_HOLDER)).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to decode gas holder: {}", err)).map(Pair::getFirst).ifPresent(holder -> gasHolder = holder);
            if (level().isClientSide) {
                setModelFromGas(gasHolder.value());
            }
        }
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
        if (!level.isClientSide) {
            if (tickCount > 1 && tickCount % 5 == 0 && getDeltaMovement().subtract(initMotion).lengthSqr() > 1.0e-5) {
                explodeDirectly(pos);
                discard();
                return;
            }
            return;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandlerUtils.of(gasHolder.value());
        cannonHandler.renderTrailParticles(level, pos);
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
        CompoundTag compoundTag = new CompoundTag();
        addAdditionalSaveData(compoundTag);
        buffer.writeNbt(compoundTag);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        CompoundTag compoundTag = additionalData.readNbt();
        if (compoundTag == null) {
            return;
        }

        readAdditionalSaveData(compoundTag);
    }

    private void explodeDirectly(Vec3 pos) {
        AirtightCannonHandler cannonHandler = AirtightCannonHandlerUtils.of(gasHolder.value());
        cannonHandler.explode(level(), pos, this, multiplier);
    }

    @OnlyIn(Dist.CLIENT)
    private void setModelFromGas(Gas gasType) {
        AirtightCannonHandler cannonHandler = AirtightCannonHandlerUtils.of(gasType);
        windChargeModel = new AirtightCannonWindChargeModel(cannonHandler.getLayerDefinition().bakeRoot());
    }
}