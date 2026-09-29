package net.ty.createcraftedbeginning.api.gas.pressure;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasPressureCompartment extends GasHandler {
    @Override
    default boolean isGasValid(int tank, GasStack stack) {
        return tank == 0 && isGasValid(stack);
    }

    @Override
    GasStack drain(GasStack resource, GasAction action);

    @Override
    GasStack drain(long maxDrain, GasAction action);

    @Override
    default GasStack getGasInTank(int tank) {
        if (tank != 0) {
            return GasStack.EMPTY;
        }

        return getGasStack();
    }

    @Override
    default int getTanks() {
        return 1;
    }

    @Override
    long fill(GasStack resource, GasAction action);

    Object getCompartmentIdentity();

    boolean isGasValid(GasStack stack);

    GasStack getGasStack();

    default boolean supportsExactDrainRecovery() {
        return false;
    }

    default long restoreDrainedGas(GasStack resource, GasAction action) {
        return fill(resource, action);
    }

    long getVolume();

    long getPressurePa();

    long getMaxPressurePa();

    default long getMaxAmount() {
        return GasPressure.amount(getVolume(), getMaxPressurePa());
    }

    long getStoredAmount();

    /**
     * Predicts transfer permissions and limits after this compartment contains the given
     * amount of gas, without changing live storage. Return null when that state cannot
     * be predicted reliably; zero limits mean a known, blocked direction.
     */
    default @Nullable PredictedTransferLimits predictTransferLimits(GasStack gas, long storedAmount) {
        return null;
    }

    record PredictedTransferLimits(long drainLimit, long fillLimit) {}

    default PressureModel getPressureModel() {
        return PressureModel.VARIABLE;
    }

    enum PressureModel {
        VARIABLE,
        FIXED
    }
}
