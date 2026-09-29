package net.ty.createcraftedbeginning.gas.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfile;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfileCompoundTags;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.foundation.NbtValues;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasReleaseState {
    private static final String COMPOUND_KEY_EFFECT_PROGRESS = "EffectProgress";
    private static final String COMPOUND_KEY_EFFECT_GAS = "EffectGas";
    private static final String COMPOUND_KEY_EFFECT_PRESSURE_PROFILE = "EffectPressureProfile";

    private GasStack effectGas = GasStack.EMPTY;
    private GameplayPressureProfile effectPressureProfile = GameplayPressureProfiles.NORMAL;
    private long effectProgress;
    private long lastFeedbackTick = Long.MIN_VALUE;
    private long lastEffectTick = Long.MIN_VALUE;

    public static GasReleaseState read(CompoundTag tag, Provider provider) {
        GasReleaseState state = new GasReleaseState();
        long progress = Mth.clamp(NbtValues.getLongOrDefault(tag, COMPOUND_KEY_EFFECT_PROGRESS, 0), 0L, GasReleaseService.EFFECT_INTERVAL - 1);
        if (progress <= 0 || !tag.contains(COMPOUND_KEY_EFFECT_GAS, Tag.TAG_COMPOUND)) {
            return state;
        }

        GasStack gas = GasStack.parseOptional(provider, tag.getCompound(COMPOUND_KEY_EFFECT_GAS));
        if (gas.isEmpty()) {
            return state;
        }

        state.effectGas = gas.copyWithAmount(1);
        state.effectPressureProfile = GameplayPressureProfileCompoundTags.read(tag, COMPOUND_KEY_EFFECT_PRESSURE_PROFILE);
        state.effectProgress = progress;
        return state;
    }

    public CompoundTag write(Provider provider) {
        CompoundTag tag = new CompoundTag();
        if (effectProgress <= 0 || effectGas.isEmpty()) {
            return tag;
        }

        tag.putLong(COMPOUND_KEY_EFFECT_PROGRESS, effectProgress);
        tag.put(COMPOUND_KEY_EFFECT_GAS, effectGas.saveOptional(provider));
        GameplayPressureProfileCompoundTags.write(tag, COMPOUND_KEY_EFFECT_PRESSURE_PROFILE, effectPressureProfile);
        return tag;
    }

    @Internal
    public GameplayPressureProfile getEffectPressureProfile() {
        return effectPressureProfile;
    }

    @Internal
    public long getEffectProgress() {
        return effectProgress;
    }

    @Internal
    public long getLastFeedbackTick() {
        return lastFeedbackTick;
    }

    @Internal
    public long getLastEffectTick() {
        return lastEffectTick;
    }

    boolean recordFeedback(long gameTime) {
        if (lastFeedbackTick == gameTime) {
            return false;
        }

        lastFeedbackTick = gameTime;
        return true;
    }

    boolean recordEffect(GasStack gas, long sourcePressurePa, long amount, long gameTime) {
        GameplayPressureProfile pressureProfile = GameplayPressureProfiles.resolve(sourcePressurePa);
        if (effectGas.isEmpty() || !GasStack.isSameGasSameComponents(effectGas, gas) || !effectPressureProfile.equals(pressureProfile)) {
            effectGas = gas.copyWithAmount(1);
            effectPressureProfile = pressureProfile;
            effectProgress = 0;
        }

        long combinedProgress = effectProgress + amount % GasReleaseService.EFFECT_INTERVAL;
        boolean reachedEffectInterval = amount >= GasReleaseService.EFFECT_INTERVAL || combinedProgress >= GasReleaseService.EFFECT_INTERVAL;
        effectProgress = combinedProgress >= GasReleaseService.EFFECT_INTERVAL ? combinedProgress - GasReleaseService.EFFECT_INTERVAL : combinedProgress;
        if (!reachedEffectInterval || lastEffectTick == gameTime) {
            return false;
        }

        lastEffectTick = gameTime;
        return true;
    }
}
