package net.ty.createcraftedbeginning.gas.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public record GasPressureTransferResult(long requestedAmount, long transferableAmount, long drainedAmount, long filledAmount, long restoredAmount, GasStack unrecoveredGas, boolean executed) {
    public GasPressureTransferResult {
        requestedAmount = Math.max(0, requestedAmount);
        transferableAmount = Mth.clamp(transferableAmount, 0L, requestedAmount);
        drainedAmount = Mth.clamp(drainedAmount, 0L, transferableAmount);
        filledAmount = Mth.clamp(filledAmount, 0L, drainedAmount);
        restoredAmount = Mth.clamp(restoredAmount, 0L, BoundedMath.saturatedSubtract(drainedAmount, filledAmount));
        unrecoveredGas = unrecoveredGas.copy();
    }

    @Override
    public GasStack unrecoveredGas() {
        return unrecoveredGas.copy();
    }

    public long unrecoveredAmount() {
        return unrecoveredGas.getAmount();
    }

    public long sourceNetLoss() {
        return BoundedMath.saturatedSubtract(drainedAmount, restoredAmount);
    }

    public boolean fullyRecovered() {
        return unrecoveredGas.isEmpty();
    }

    public boolean custodyAccountedFor() {
        return sourceNetLoss() == BoundedMath.saturatedAdd(filledAmount, unrecoveredAmount());
    }
}
