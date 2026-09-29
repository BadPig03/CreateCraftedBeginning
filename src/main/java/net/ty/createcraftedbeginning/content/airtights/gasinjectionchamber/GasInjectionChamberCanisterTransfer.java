package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer.InjectionMode;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureFillService;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberCanisterTransfer {
    private GasInjectionChamberCanisterTransfer() {
    }

    public static long getTransferableAmount(GasPressureCompartment source, GasCanisterContainer canister, GasStack expectedGas, long maxAmount) {
        return planTransfer(source, canister, expectedGas, maxAmount).transferableAmount();
    }

    public static boolean transferExactly(GasPressureCompartment source, GasCanisterContainer canister, GasStack expectedGas, long amount) {
        if (amount <= 0) {
            return false;
        }

        TransferPlan plan = planTransfer(source, canister, expectedGas, amount);
        if (plan.transferableAmount() != amount) {
            return false;
        }

        GasStack drainedGas = source.drain(plan.gas().copyWithAmount(amount), GasAction.EXECUTE);
        if (drainedGas.isEmpty() || !GasStack.isSameGasSameComponents(drainedGas, plan.gas())) {
            return false;
        }

        long drainedAmount = Mth.clamp(drainedGas.getAmount(), 0L, amount);
        if (drainedAmount != amount) {
            restoreSource(source, plan.gas(), drainedAmount);
            return false;
        }

        GasPressureTransferEndpoint target = GasPressureTransferEndpoint.canister(canister, 0);
        long filledAmount = Mth.clamp(target.executeFill(plan.gas(), amount, plan.sourcePressurePa()), 0L, amount);
        long remainderAmount = BoundedMath.saturatedSubtract(amount, filledAmount);
        restoreSource(source, plan.gas(), remainderAmount);
        return filledAmount == amount;
    }

    static boolean canTransferExactly(GasPressureCompartment source, GasCanisterContainer canister, GasStack expectedGas, long amount) {
        return amount > 0 && planTransfer(source, canister, expectedGas, amount).transferableAmount() == amount;
    }

    private static TransferPlan planTransfer(GasPressureCompartment source, GasCanisterContainer canister, GasStack expectedGas, long maxAmount) {
        GasStack sourceGas = source.getGasStack();
        if (expectedGas.isEmpty() || sourceGas.isEmpty() || !GasStack.isSameGasSameComponents(sourceGas, expectedGas) || maxAmount <= 0 || canister.getInjectionMode() == InjectionMode.DENY || canister.getTanks() <= 0 || !source.supportsExactDrainRecovery()) {
            return TransferPlan.EMPTY;
        }

        long requestedAmount = Math.min(maxAmount, sourceGas.getAmount());
        GasStack gas = sourceGas.copyWithAmount(1);
        GasStack drainPreview = source.drain(gas.copyWithAmount(requestedAmount), GasAction.SIMULATE);
        if (drainPreview.isEmpty() || !GasStack.isSameGasSameComponents(drainPreview, gas)) {
            return TransferPlan.EMPTY;
        }

        long drainableAmount = Mth.clamp(drainPreview.getAmount(), 0L, requestedAmount);
        if (drainableAmount <= 0) {
            return TransferPlan.EMPTY;
        }

        long sourcePressurePa = source.getPressurePa();
        GasPressureTransferEndpoint target = GasPressureTransferEndpoint.canister(canister, 0);
        long transferableAmount = GasPressureFillService.getTransferableAmount(target, gas, drainableAmount, sourcePressurePa);
        if (transferableAmount <= 0) {
            return TransferPlan.EMPTY;
        }

        return new TransferPlan(gas, transferableAmount, sourcePressurePa);
    }

    private static void restoreSource(GasPressureCompartment source, GasStack gas, long amount) {
        if (amount <= 0) {
            return;
        }

        long restoredAmount = source.restoreDrainedGas(gas.copyWithAmount(amount), GasAction.EXECUTE);
        if (restoredAmount == amount) {
            return;
        }

        throw new IllegalStateException("Failed to restore gas injection chamber source after draining: expected " + amount + " GU, restored " + restoredAmount + " GU.");
    }

    private record TransferPlan(GasStack gas, long transferableAmount, long sourcePressurePa) {
        private static final TransferPlan EMPTY = new TransferPlan(GasStack.EMPTY, 0, 0);
    }
}
