package net.ty.createcraftedbeginning.content.airtights.airtightcannon.windcharge;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;
import net.minecraft.world.entity.projectile.windcharge.AbstractWindCharge;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.registry.CCBEntityTypes;
import net.ty.createcraftedbeginning.registry.CCBSoundEvents;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public class AirtightCannonWindChargeProjectileEntity extends AbstractWindCharge implements IEntityWithComplexSpawn {
    private static final String COMPOUND_KEY_GAS_HOLDER = "GasHolder";
    private static final String COMPOUND_KEY_MULTIPLIER = "Multiplier";
    private static final String COMPOUND_KEY_KNOCKBACK = "Knockback";
    private static final float DEFAULT_SIZE = 0.3125f;

    private AirtightCannonWindChargeModel windChargeModel;
    private Holder<Gas> gasHolder = Gas.EMPTY_GAS_HOLDER;
    private float multiplier = 1.0f;
    private float knockback = 0.1f;
    private Vec3 deltaMotion = Vec3.ZERO;

    public AirtightCannonWindChargeProjectileEntity(EntityType<? extends AbstractWindCharge> entityType, Level level) {
        super(entityType, level);
    }

    public AirtightCannonWindChargeProjectileEntity(Level level, @NotNull Holder<Gas> gasHolder, Vec3 deltaMotion) {
        super(CCBEntityTypes.AIRTIGHT_CANNON_WIND_CHARGE_PROJECTILE.get(), level);
        this.gasHolder = gasHolder;
        this.deltaMotion = deltaMotion;
        setModelFromGas(gasHolder.value());
    }

    public static void playLaunchSound(Level world, Vec3 location, float pitch) {
        CCBSoundEvents.WIND_CHARGE_LAUNCH.playAt(world, location, 1, pitch, true);
    }

    @SuppressWarnings("unchecked")
    public static @NotNull Builder<?> build(Builder<?> builder) {
        Builder<AirtightCannonWindChargeProjectileEntity> entityBuilder = (Builder<AirtightCannonWindChargeProjectileEntity>) builder;
        return entityBuilder.sized(DEFAULT_SIZE, DEFAULT_SIZE).eyeHeight(0);
    }

    @Override
    protected void explode(@NotNull Vec3 pos) {
        explodeDirectly(pos);
    }

    @Override
    public void tick() {
        super.tick();

        Vec3 pos = position();
        Level level = level();
        if (!level.isClientSide) {
            if (level.getGameTime() % 20 == 0 && getDeltaMovement().subtract(deltaMotion).length() > 1.0e-5) {
                explodeDirectly(pos);
                discard();
                return;
            }
            return;
        }

        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gasHolder.value());
        if (cannonHandler == null) {
            return;
        }

        cannonHandler.renderTrailParticles(level, pos);
    }

    @Override
    public boolean hurt(@NotNull DamageSource source, float amount) {
        if (level().isClientSide) {
            return super.hurt(source, amount);
        }

        explodeDirectly(position());
        discard();
        return true;
    }

    @Nullable
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

    @Override
    public void addAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.addAdditionalSaveData(compoundTag);
        compoundTag.putFloat(COMPOUND_KEY_MULTIPLIER, multiplier);
        compoundTag.putFloat(COMPOUND_KEY_KNOCKBACK, knockback);
        Gas.HOLDER_CODEC.encodeStart(NbtOps.INSTANCE, gasHolder).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to encode gas holder: {}", err)).ifPresent(tag -> compoundTag.put(COMPOUND_KEY_GAS_HOLDER, tag));
    }

    @Override
    public void readAdditionalSaveData(@NotNull CompoundTag compoundTag) {
        super.readAdditionalSaveData(compoundTag);
        if (compoundTag.contains(COMPOUND_KEY_MULTIPLIER)) {
            multiplier = compoundTag.getFloat(COMPOUND_KEY_MULTIPLIER);
        }
        if (compoundTag.contains(COMPOUND_KEY_KNOCKBACK)) {
            knockback = compoundTag.getFloat(COMPOUND_KEY_KNOCKBACK);
        }
        if (compoundTag.contains(COMPOUND_KEY_GAS_HOLDER)) {
            Gas.HOLDER_CODEC.decode(NbtOps.INSTANCE, compoundTag.get(COMPOUND_KEY_GAS_HOLDER)).resultOrPartial(err -> CreateCraftedBeginning.LOGGER.error("Failed to decode gas holder: {}", err)).map(Pair::getFirst).ifPresent(holder -> gasHolder = holder);
            setModelFromGas(gasHolder.value());
        }
    }

    @Override
    protected void onDeflection(@Nullable Entity entity, boolean deflectedByPlayer) {
        explodeDirectly(position());
        discard();
    }

    @Override
    public void writeSpawnData(@NotNull RegistryFriendlyByteBuf buffer) {
        CompoundTag compound = new CompoundTag();
        addAdditionalSaveData(compound);
        buffer.writeNbt(compound);
    }

    @Override
    public void readSpawnData(@NotNull RegistryFriendlyByteBuf additionalData) {
        CompoundTag nbt = additionalData.readNbt();
        if (nbt == null) {
            return;
        }
        readAdditionalSaveData(nbt);
    }

    private void explodeDirectly(Vec3 pos) {
        Gas gas = gasHolder.value();
        Level level = level();
        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gas);
        if (cannonHandler == null) {
            return;
        }

        cannonHandler.explode(level, pos, this, multiplier);
    }

    private void setModelFromGas(Gas gas) {
        AirtightCannonHandler cannonHandler = AirtightCannonHandler.REGISTRY.get(gas);
        windChargeModel = cannonHandler != null ? new AirtightCannonWindChargeModel(cannonHandler.getLayerDefinition().bakeRoot()) : null;
    }
}