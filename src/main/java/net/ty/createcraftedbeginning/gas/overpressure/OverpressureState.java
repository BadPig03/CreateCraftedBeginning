package net.ty.createcraftedbeginning.gas.overpressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.nbt.CompoundTag;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasPressureLimits;
import net.ty.createcraftedbeginning.foundation.NbtValues;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class OverpressureState {
    public static final String COMPOUND_KEY_STRESS = "OverpressureStress";

    private double stress;
    private long lastObservedPressurePa = GasPressure.VACUUM_PA;
    private int stabilizationTicksRemaining = OverpressureStressCalculator.STABILIZATION_GRACE_TICKS;

    public boolean tick(long currentPressurePa) {
        lastObservedPressurePa = GasPressureLimits.clampToHardLimit(currentPressurePa);
        if (stabilizationTicksRemaining > 0) {
            stabilizationTicksRemaining--;
            return false;
        }

        double previousStress = stress;
        stress = OverpressureStressCalculator.advance(stress, lastObservedPressurePa);
        return Double.compare(previousStress, stress) != 0;
    }

    public double getStress() {
        return stress;
    }

    public float getStressFraction() {
        return (float) stress;
    }

    public boolean setStress(double stress) {
        double sanitized = OverpressureStressCalculator.sanitizeStress(stress);
        if (Double.compare(this.stress, sanitized) == 0) {
            return false;
        }

        this.stress = sanitized;
        return true;
    }

    public int getStabilizationTicksRemaining() {
        return stabilizationTicksRemaining;
    }

    public boolean isStabilizing() {
        return stabilizationTicksRemaining > 0;
    }

    public boolean isAtFailureThreshold() {
        return stress >= OverpressureStressCalculator.MAX_STRESS;
    }

    public boolean isFailureReady() {
        return !isStabilizing() && isAtFailureThreshold() && GasPressureLimits.isOverpressure(lastObservedPressurePa);
    }

    public void restartStabilizationGrace() {
        stabilizationTicksRemaining = OverpressureStressCalculator.STABILIZATION_GRACE_TICKS;
    }

    public void readStress(CompoundTag compoundTag) {
        stress = OverpressureStressCalculator.sanitizeStress(NbtValues.getDoubleOrDefault(compoundTag, COMPOUND_KEY_STRESS, 0.0));
    }

    public void writeStress(CompoundTag compoundTag) {
        compoundTag.putDouble(COMPOUND_KEY_STRESS, stress);
    }
}
