package net.ty.createcraftedbeginning.content.airtights.aircompressor.overheatstates;

import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity;
import org.jetbrains.annotations.NotNull;

public interface IOverheatState {
    IOverheatState getNextState();

    IOverheatState getPreviousState();

    void tick(@NotNull AirCompressorBlockEntity blockEntity);

    String getSerializedName();

    ChatFormatting getDisplayColor();

    float getEfficiency();

    String getTranslationKey();

    int getAnalogOutputSignal();

    void spawnParticlesInPonderLevel(PonderLevel level, BlockPos pos, int tick);

    default void spawnParticles(@NotNull Level level, BlockPos pos, float smokeThreshold, float flameThreshold) {
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= smokeThreshold) {
            Vec3 added = VecHelper.getCenterOf(pos).add(VecHelper.offsetRandomly(Vec3.ZERO, random, 0.5f).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextFloat() * 0.125)).add(0, 0.5, 0);
            level.addParticle(ParticleTypes.SMOKE, added.x, added.y, added.z, 0, random.nextFloat() * 0.0125, 0);
        }
        if (random.nextFloat() >= flameThreshold) {
            Vec3 added = VecHelper.getCenterOf(pos).add(VecHelper.offsetRandomly(Vec3.ZERO, random, 1.0f).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextFloat() * 0.125)).add(0, 0.5, 0);
            level.addParticle(ParticleTypes.FLAME, added.x, added.y, added.z, 0, random.nextFloat() * 0.025, 0);
        }
    }

    default int tryAddHeat(@NotNull AirCompressorBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (level == null || level.isClientSide) {
            return 0;
        }

        int heatAdded = blockEntity.getHeatAdded();
        int heatReduced = blockEntity.getCoolantEfficiency().getHeatReduced(level);
        return heatAdded - heatReduced;
    }
}
