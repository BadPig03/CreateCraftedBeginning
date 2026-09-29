package net.ty.createcraftedbeginning.gas.storage;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasTankMutationResult(Status status, long excessAmount) {
    public enum Status {
        APPLIED,
        UNCHANGED,
        INVALID_GAS,
        EXCEEDS_PRESSURE_LIMIT
    }

    public GasTankMutationResult {
        if (excessAmount < 0) {
            throw new IllegalArgumentException("Gas tank excess amount must be non-negative; got " + excessAmount + " GU.");
        }

        if (status != Status.EXCEEDS_PRESSURE_LIMIT && excessAmount != 0) {
            throw new IllegalArgumentException("Gas tank excess amount must be zero unless the pressure limit was exceeded; got " + excessAmount + " GU with status " + status + '.');
        }
    }

    public static GasTankMutationResult applied() {
        return new GasTankMutationResult(Status.APPLIED, 0);
    }

    public static GasTankMutationResult unchanged() {
        return new GasTankMutationResult(Status.UNCHANGED, 0);
    }

    public static GasTankMutationResult invalidGas() {
        return new GasTankMutationResult(Status.INVALID_GAS, 0);
    }

    public static GasTankMutationResult exceedsPressureLimit(long excessAmount) {
        return new GasTankMutationResult(Status.EXCEEDS_PRESSURE_LIMIT, excessAmount);
    }

    public boolean accepted() {
        return status == Status.APPLIED || status == Status.UNCHANGED;
    }

    public boolean changed() {
        return status == Status.APPLIED;
    }

    public GasTankMutationResult requireAccepted() {
        if (!accepted()) {
            throw new IllegalStateException("Gas tank mutation rejected: " + status + '.');
        }

        return this;
    }
}
