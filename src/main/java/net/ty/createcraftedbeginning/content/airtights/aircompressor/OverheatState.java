package net.ty.createcraftedbeginning.content.airtights.aircompressor;

import net.createmod.catnip.math.VecHelper;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.CCBDataComponents;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Locale;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
enum OverheatState {
    NORMAL("normal", 100, 0, ChatFormatting.AQUA, 0, 0, 1, 1),
    SLIGHT("slight", 90, 5, ChatFormatting.YELLOW, 8, 4, 0.7f, 1),
    MODERATE("moderate", 60, 10, ChatFormatting.GOLD, 4, 2, 0.4f, 0.8f),
    SEVERE("severe", 20, 15, ChatFormatting.RED, 2, 1, 0.1f, 0.3f),
    MELTDOWN("meltdown", 0, 15, ChatFormatting.DARK_RED, 0, 0, 1, 1);

    private final String serializedName;
    private final int efficiencyPercent;
    private final int analogOutputSignal;
    private final ChatFormatting displayColor;
    private final int particleTickRate;
    private final int ponderParticleTickRate;
    private final float smokeChance;
    private final float flameChance;

    OverheatState(String serializedName, int efficiencyPercent, int analogOutputSignal, ChatFormatting displayColor, int particleTickRate, int ponderParticleTickRate, float smokeChance, float flameChance) {
        this.serializedName = serializedName;
        this.efficiencyPercent = efficiencyPercent;
        this.analogOutputSignal = analogOutputSignal;
        this.displayColor = displayColor;
        this.particleTickRate = particleTickRate;
        this.ponderParticleTickRate = ponderParticleTickRate;
        this.smokeChance = smokeChance;
        this.flameChance = flameChance;
    }

    static OverheatState fromName(String serializedName) {
        String normalizedName = serializedName.toLowerCase(Locale.ROOT);
        for (OverheatState overheatState : values()) {
            if (!overheatState.serializedName.equals(normalizedName)) {
                continue;
            }

            return overheatState;
        }
        return NORMAL;
    }

    static OverheatState fromItem(ItemStack stack) {
        return AirCompressorThermal.getOverheatState(stack.getOrDefault(CCBDataComponents.COMPRESSOR_STORED_HEAT, 0));
    }

    static OverheatState fromStoredHeat(int storedHeat, int overheatThreshold) {
        int safeOverheatThreshold = Math.max(1, overheatThreshold);
        int overheatStateIndex = Math.min(MELTDOWN.ordinal(), Math.max(0, storedHeat) / safeOverheatThreshold);
        return values()[overheatStateIndex];
    }

    private static Vec3 getParticlePosition(BlockPos compressorPos, RandomSource random, float radius) {
        return VecHelper.getCenterOf(compressorPos).add(VecHelper.offsetRandomly(Vec3.ZERO, random, radius).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextFloat() * 0.125)).add(0, 0.5, 0);
    }

    ChatFormatting getDisplayColor() {
        return displayColor;
    }

    int getEfficiencyPercent() {
        return efficiencyPercent;
    }

    int getAnalogOutputSignal() {
        return analogOutputSignal;
    }

    String getSerializedName() {
        return serializedName;
    }

    String getTranslationKey() {
        return "gui.air_compressor." + serializedName;
    }

    void spawnParticlesInPonderLevel(PonderLevel level, BlockPos compressorPos, int ponderTick) {
        if (ponderParticleTickRate <= 0 || ponderTick % ponderParticleTickRate != 0) {
            return;
        }

        spawnParticles(level, compressorPos);
    }

    void tick(AirCompressorBlockEntity compressor) {
        Level level = compressor.getLevel();
        if (level == null) {
            return;
        }

        if (this == MELTDOWN) {
            if (level.isClientSide) {
                return;
            }

            BlockPos compressorPos = compressor.getBlockPos();
            level.destroyBlock(compressorPos, false);
            if (CCBConfig.server().airtights.explodesOnMeltdown.get()) {
                level.explode(null, compressorPos.getX() + 0.5, compressorPos.getY() + 0.5, compressorPos.getZ() + 0.5, 8, true, ExplosionInteraction.NONE);
            }
            return;
        }

        if (!level.isClientSide || particleTickRate <= 0 || level.getGameTime() % particleTickRate != 0) {
            return;
        }

        spawnParticles(level, compressor.getBlockPos());
    }

    private void spawnParticles(Level level, BlockPos compressorPos) {
        RandomSource random = level.getRandom();
        if (random.nextFloat() <= smokeChance) {
            Vec3 particlePos = getParticlePosition(compressorPos, random, 0.5f);
            level.addParticle(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 0, random.nextFloat() * 0.0125, 0);
        }
        if (random.nextFloat() > flameChance) {
            return;
        }

        Vec3 particlePos = getParticlePosition(compressorPos, random, 1);
        level.addParticle(ParticleTypes.FLAME, particlePos.x, particlePos.y, particlePos.z, 0, random.nextFloat() * 0.025, 0);
    }
}
