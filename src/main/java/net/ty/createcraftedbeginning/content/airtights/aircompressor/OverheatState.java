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
public enum OverheatState {
    NORMAL("normal", 100, 0, ChatFormatting.AQUA, 0, 0, 1.0f, 1.0f),
    SLIGHT("slight", 90, 5, ChatFormatting.YELLOW, 8, 4, 0.7f, 1.0f),
    MODERATE("moderate", 60, 10, ChatFormatting.GOLD, 4, 2, 0.4f, 0.8f),
    SEVERE("severe", 20, 15, ChatFormatting.RED, 2, 1, 0.1f, 0.3f),
    MELTDOWN("meltdown", 0, 15, ChatFormatting.DARK_RED, 0, 0, 1.0f, 1.0f);

    private final String serializedName;
    private final int efficiencyPercent;
    private final int analogOutputSignal;
    private final ChatFormatting displayColor;
    private final int particleTickRate;
    private final int ponderParticleTickRate;
    private final float smokeThreshold;
    private final float flameThreshold;

    OverheatState(String serializedName, int efficiencyPercent, int analogOutputSignal, ChatFormatting displayColor, int particleTickRate, int ponderParticleTickRate, float smokeThreshold, float flameThreshold) {
        this.serializedName = serializedName;
        this.efficiencyPercent = efficiencyPercent;
        this.analogOutputSignal = analogOutputSignal;
        this.displayColor = displayColor;
        this.particleTickRate = particleTickRate;
        this.ponderParticleTickRate = ponderParticleTickRate;
        this.smokeThreshold = smokeThreshold;
        this.flameThreshold = flameThreshold;
    }

    public static OverheatState fromName(String name) {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        for (OverheatState state : values()) {
            if (!state.serializedName.equals(normalizedName)) {
                continue;
            }

            return state;
        }
        return NORMAL;
    }

    public static OverheatState fromItem(ItemStack item) {
        return fromName(item.getOrDefault(CCBDataComponents.COMPRESSOR_OVERHEAT_STATE, NORMAL.serializedName));
    }

    public static OverheatState fromStoredHeat(int storedHeat, int threshold) {
        int safeThreshold = Math.max(1, threshold);
        int stateIndex = Math.min(MELTDOWN.ordinal(), Math.max(0, storedHeat) / safeThreshold);
        return values()[stateIndex];
    }

    private static Vec3 getParticlePosition(BlockPos pos, RandomSource random, float radius) {
        return VecHelper.getCenterOf(pos).add(VecHelper.offsetRandomly(Vec3.ZERO, random, radius).multiply(1, 0.25, 1).normalize().scale(0.5 + random.nextFloat() * 0.125)).add(0, 0.5, 0);
    }

    public ChatFormatting getDisplayColor() {
        return displayColor;
    }

    public int getEfficiencyPercent() {
        return efficiencyPercent;
    }

    public int getAnalogOutputSignal() {
        return analogOutputSignal;
    }

    public String getSerializedName() {
        return serializedName;
    }

    public String getTranslationKey() {
        return "gui.air_compressor." + serializedName;
    }

    public void spawnParticlesInPonderLevel(PonderLevel level, BlockPos pos, int tick) {
        if (ponderParticleTickRate <= 0 || tick % ponderParticleTickRate != 0) {
            return;
        }

        spawnParticles(level, pos);
    }

    public void tick(AirCompressorBlockEntity blockEntity) {
        Level level = blockEntity.getLevel();
        if (level == null) {
            return;
        }

        if (this == MELTDOWN) {
            if (level.isClientSide) {
                return;
            }

            BlockPos pos = blockEntity.getBlockPos();
            level.destroyBlock(pos, false);
            if (CCBConfig.server().airtights.explodesOnMeltdown.get()) {
                level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, true, ExplosionInteraction.NONE);
            }
            return;
        }

        if (!level.isClientSide || particleTickRate <= 0 || level.getGameTime() % particleTickRate != 0) {
            return;
        }

        spawnParticles(level, blockEntity.getBlockPos());
    }

    private void spawnParticles(Level level, BlockPos pos) {
        RandomSource random = level.getRandom();
        if (random.nextFloat() >= smokeThreshold) {
            Vec3 particlePos = getParticlePosition(pos, random, 0.5f);
            level.addParticle(ParticleTypes.SMOKE, particlePos.x, particlePos.y, particlePos.z, 0, random.nextFloat() * 0.0125, 0);
        }
        if (random.nextFloat() >= flameThreshold) {
            Vec3 particlePos = getParticlePosition(pos, random, 1.0f);
            level.addParticle(ParticleTypes.FLAME, particlePos.x, particlePos.y, particlePos.z, 0, random.nextFloat() * 0.025, 0);
        }
    }
}
