package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import com.simibubi.create.content.kinetics.base.IRotate.SpeedLevel;
import com.simibubi.create.content.kinetics.fan.processing.AllFanProcessingTypes;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessing;
import com.simibubi.create.content.kinetics.fan.processing.FanProcessingType;
import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.end.endincinerationblower.EndIncinerationBlowerStructuralBlockEntity.BlowerWorkingMode;
import net.ty.createcraftedbeginning.platform.SubLevelBridge;
import net.ty.createcraftedbeginning.platform.SubLevelBridge.EntityArea;
import net.ty.createcraftedbeginning.registry.CCBParticleTypes;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class EndIncinerationBlowerVisualState {
    private static final int PROCESSING_PARTICLE_INTERVAL_TICKS = 10;
    private static final int MAX_PROCESSING_PARTICLE_TARGETS = 8;
    private static final double PARTICLE_DIRECTION_LENGTH_SQR_EPSILON = 1.0E-6;

    private int particleCounter;

    private static void spawnFanProcessingParticles(Level level, FanProcessingType processingType, AABB processingArea, EntityArea entityArea) {
        int spawnedCount = 0;
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, processingArea)) {
            if (!entityArea.intersects(itemEntity) || !FanProcessing.canProcess(itemEntity, processingType)) {
                continue;
            }

            processingType.spawnProcessingParticles(level, itemEntity.position());
            if (++spawnedCount < MAX_PROCESSING_PARTICLE_TARGETS) {
                continue;
            }

            return;
        }
    }

    private static void spawnPrimaryEffectParticles(Level level, BlockPos pos, float speed, @Nullable BlowerWorkingMode workingMode) {
        float absSpeed = Mth.abs(speed);
        if (EndIncinerationBlowerRange.calculateRange(absSpeed) <= 0 || workingMode == null) {
            return;
        }

        AABB processingArea = EndIncinerationBlowerRange.calculateArea(pos, absSpeed);
        EntityArea entityArea = SubLevelBridge.createEntityArea(level, pos, processingArea);
        switch (workingMode) {
            case SMOKING -> spawnFanProcessingParticles(level, AllFanProcessingTypes.SMOKING, processingArea, entityArea);
            case BLASTING -> spawnFanProcessingParticles(level, AllFanProcessingTypes.BLASTING, processingArea, entityArea);
            case IGNITION -> {
            }
        }
    }

    private static boolean shouldSpawnProcessingParticles(Level level, BlockPos pos) {
        return Math.floorMod(level.getGameTime(), PROCESSING_PARTICLE_INTERVAL_TICKS) == Math.floorMod(pos.hashCode(), PROCESSING_PARTICLE_INTERVAL_TICKS);
    }

    void tick(Level level, BlockPos pos, float speed, Supplier<BlowerWorkingMode> workingModeSupplier) {
        spawnParticles(level, pos, speed);
        if (level instanceof PonderLevel || !shouldSpawnProcessingParticles(level, pos)) {
            return;
        }

        spawnPrimaryEffectParticles(level, pos, speed, workingModeSupplier.get());
    }

    private void spawnParticles(Level level, BlockPos pos, float speed) {
        float absSpeed = Mth.abs(speed);
        float range = EndIncinerationBlowerRange.calculateRange(absSpeed);
        if (range <= 0 || !level.isClientSide) {
            return;
        }

        float mediumSpeed = SpeedLevel.MEDIUM.getSpeedValue();
        float maxEffectiveRatio = Math.max(1, EndIncinerationBlowerRange.getMaxRange() + 0.5f);
        float effectiveRatio = mediumSpeed <= 0 ? maxEffectiveRatio : Mth.clamp(absSpeed / mediumSpeed, 1, maxEffectiveRatio);
        int spawnInterval = Math.max(1, Mth.floor(40 / effectiveRatio));
        particleCounter++;
        if (particleCounter < spawnInterval) {
            return;
        }

        particleCounter = 0;
        int particleCount = Math.max(1, Mth.floor(effectiveRatio));
        Vec3 center = VecHelper.getCenterOf(pos);
        for (int i = 0; i < particleCount; i++) {
            Vec3 offset = VecHelper.offsetRandomly(center, level.random, range * 0.9f);
            Vec3 direction = center.subtract(offset);
            if (direction.lengthSqr() < PARTICLE_DIRECTION_LENGTH_SQR_EPSILON) {
                continue;
            }

            Vec3 velocity = direction.normalize().scale(0.025 + effectiveRatio * 0.015);
            level.addParticle(CCBParticleTypes.END_INCINERATION.getParticleOptions(), offset.x, offset.y, offset.z, velocity.x, velocity.y, velocity.z);
        }
    }
}
