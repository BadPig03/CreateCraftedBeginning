package net.ty.createcraftedbeginning.gas.transfer;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.network.math.GasPressureTransferMath;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPressureTransferService {
    private GasPressureTransferService() {
    }

    public static GasPressureTransferResult transferPassive(GasPressureTransferEndpoint source, GasPressureTransferEndpoint target, long maxAmount, GasAction action) {
        long requestedAmount = Math.max(0, maxAmount);
        TransferPlan plan = planPassive(source, target, requestedAmount);
        if (plan.transferableAmount <= 0 || !action.execute()) {
            return new GasPressureTransferResult(requestedAmount, plan.transferableAmount, 0, 0, 0, GasStack.EMPTY, false);
        }

        GasStack drainedGas = source.executeDrain(plan.gas, plan.transferableAmount);
        if (drainedGas.isEmpty() || !GasStack.isSameGasSameComponents(drainedGas, plan.gas)) {
            return new GasPressureTransferResult(requestedAmount, plan.transferableAmount, 0, 0, 0, GasStack.EMPTY, true);
        }

        long drainedAmount = Mth.clamp(drainedGas.getAmount(), 0L, plan.transferableAmount);
        if (drainedAmount <= 0) {
            return new GasPressureTransferResult(requestedAmount, plan.transferableAmount, 0, 0, 0, GasStack.EMPTY, true);
        }

        long fillLimit = Math.min(drainedAmount, maxFillAmountAfterDrain(target, plan, drainedAmount));
        long filledAmount = target.executeFill(plan.gas, fillLimit, plan.sourcePressurePa);
        filledAmount = Mth.clamp(filledAmount, 0L, fillLimit);

        long remainderAmount = BoundedMath.saturatedSubtract(drainedAmount, filledAmount);
        long restoredAmount = 0;
        if (remainderAmount > 0) {
            GasStack remainder = plan.gas.copyWithAmount(remainderAmount);
            restoredAmount = source.restoreDrainedGas(remainder);
            if (restoredAmount != remainderAmount) {
                throw new IllegalStateException("Failed to restore passive gas transfer source after draining: expected " + remainderAmount + " GU, restored " + restoredAmount + " GU.");
            }
        }
        return new GasPressureTransferResult(requestedAmount, plan.transferableAmount, drainedAmount, filledAmount, restoredAmount, GasStack.EMPTY, true);
    }

    private static TransferPlan planPassive(GasPressureTransferEndpoint source, GasPressureTransferEndpoint target, long requestedAmount) {
        if (requestedAmount <= 0 || source.identifiesSameCompartment(target) || !source.supportsExactDrainRecovery()) {
            return TransferPlan.EMPTY;
        }

        GasStack sourceGas = source.getGasStack();
        if (sourceGas.isEmpty()) {
            return TransferPlan.EMPTY;
        }

        GasStack gas = sourceGas.copyWithAmount(1);
        GasStack targetGas = target.getGasStack();
        if (!targetGas.isEmpty() && !GasStack.isSameGasSameComponents(targetGas, gas)) {
            return TransferPlan.EMPTY;
        }

        double sourcePressurePa = source.getDrainPressurePa(gas);
        double targetPressurePa = target.getFillPressurePa(gas);
        if (sourcePressurePa <= targetPressurePa) {
            return TransferPlan.EMPTY;
        }

        PressureModel sourceModel = source.getPressureModel();
        PressureModel targetModel = target.getPressureModel();
        double equilibriumPressurePa = calculateEquilibriumPressure(source, target, sourcePressurePa, targetPressurePa);
        double sourcePressureFloorPa = targetModel == PressureModel.VARIABLE ? equilibriumPressurePa : targetPressurePa;
        double targetPressureCeilingPa = sourceModel == PressureModel.VARIABLE ? equilibriumPressurePa : sourcePressurePa;
        long pressureLimitedAmount = requestedAmount;
        if (sourceModel == PressureModel.VARIABLE) {
            long safeDrain = GasPressureTransferMath.maxDrainAmount(source.getStoredAmount(), source.getVolume(), sourcePressurePa, sourcePressureFloorPa);
            pressureLimitedAmount = Math.min(pressureLimitedAmount, safeDrain);
        }
        if (targetModel == PressureModel.VARIABLE) {
            long safeFill = GasPressureTransferMath.maxFillAmount(target.getStoredAmount(), target.getMaxAmount(), target.getVolume(), targetPressurePa, targetPressureCeilingPa, target.getMaxPressurePa());
            pressureLimitedAmount = Math.min(pressureLimitedAmount, safeFill);
        }
        if (pressureLimitedAmount <= 0) {
            return TransferPlan.EMPTY;
        }

        long simulatedDrain = source.simulateDrainAmount(gas, pressureLimitedAmount);
        long candidateAmount = Math.min(pressureLimitedAmount, simulatedDrain);
        if (candidateAmount <= 0) {
            return TransferPlan.EMPTY;
        }

        long sourcePressureForFill = GasPressure.floor(sourcePressurePa);
        long simulatedFill = target.simulateFillAmount(gas, candidateAmount, sourcePressureForFill);
        long transferableAmount = Math.min(candidateAmount, simulatedFill);
        if (transferableAmount <= 0) {
            return TransferPlan.EMPTY;
        }

        return new TransferPlan(gas, transferableAmount, sourcePressureForFill, targetPressureCeilingPa);
    }

    private static long maxFillAmountAfterDrain(GasPressureTransferEndpoint target, TransferPlan plan, long drainedAmount) {
        if (drainedAmount <= 0) {
            return 0;
        }

        long capped = drainedAmount;
        if (target.getPressureModel() == PressureModel.VARIABLE) {
            double currentTargetPressurePa = target.getFillPressurePa(plan.gas);
            long pressureSafeFill = GasPressureTransferMath.maxFillAmount(target.getStoredAmount(), target.getMaxAmount(), target.getVolume(), currentTargetPressurePa, plan.targetPressureCeilingPa, target.getMaxPressurePa());
            capped = Math.min(capped, pressureSafeFill);
        }
        if (capped <= 0) {
            return 0;
        }

        return Math.min(capped, target.simulateFillAmount(plan.gas, capped, plan.sourcePressurePa));
    }

    private static double calculateEquilibriumPressure(GasPressureTransferEndpoint source, GasPressureTransferEndpoint target, double sourcePressurePa, double targetPressurePa) {
        PressureModel sourceModel = source.getPressureModel();
        PressureModel targetModel = target.getPressureModel();
        if (sourceModel == PressureModel.FIXED) {
            return sourcePressurePa;
        }

        if (targetModel == PressureModel.FIXED) {
            return targetPressurePa;
        }

        long sourceVolume = source.getVolume();
        long targetVolume = target.getVolume();
        if (sourceVolume <= 0 || targetVolume <= 0) {
            return targetPressurePa;
        }

        double totalAmount = (double) source.getStoredAmount() + target.getStoredAmount();
        double totalVolume = (double) sourceVolume + targetVolume;
        if (totalAmount <= 0 || totalVolume <= 0 || !Double.isFinite(totalAmount) || !Double.isFinite(totalVolume)) {
            return targetPressurePa;
        }

        return Math.max(GasPressure.VACUUM_PA, totalAmount * GasPressure.REFERENCE_PRESSURE_PA / totalVolume);
    }

    private record TransferPlan(GasStack gas, long transferableAmount, long sourcePressurePa, double targetPressureCeilingPa) {
        private static final TransferPlan EMPTY = new TransferPlan(GasStack.EMPTY, 0, GasPressure.VACUUM_PA, GasPressure.VACUUM_PA);
    }
}
