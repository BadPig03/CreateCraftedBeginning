package net.ty.createcraftedbeginning.content.airtights.gaspackager;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasPackagerGasTransfer {

    private GasPackagerGasTransfer() {
    }

    static GasStack drainGas(GasHandler handler, long maxAmount) {
        if (maxAmount <= 0) {
            return GasStack.EMPTY;
        }

        GasStack selectedGas = GasStack.EMPTY;
        long drainedAmount = 0;
        int tankCount = Math.max(0, handler.getTanks());
        for (int tankIndex = 0; tankIndex < tankCount && drainedAmount < maxAmount; tankIndex++) {
            GasStack tankGas = handler.getGasInTank(tankIndex);
            if (tankGas.isEmpty()) {
                continue;
            }

            if (!selectedGas.isEmpty() && !GasStack.isSameGasSameComponents(selectedGas, tankGas)) {
                continue;
            }

            Optional<GasPressureTransferEndpoint> endpoint = packageableEndpoint(handler, tankIndex, tankGas);
            if (endpoint.isEmpty()) {
                continue;
            }

            GasPressureTransferEndpoint source = endpoint.get();
            long remainingAmount = maxAmount - drainedAmount;
            long requestedAmount = Math.min(remainingAmount, tankGas.getAmount());
            long simulatedAmount = source.simulateDrainAmount(tankGas, requestedAmount);
            if (simulatedAmount <= 0) {
                continue;
            }

            GasStack drainedGas = source.executeDrain(tankGas, Math.min(requestedAmount, simulatedAmount));
            if (drainedGas.isEmpty() || !GasStack.isSameGasSameComponents(drainedGas, tankGas)) {
                continue;
            }

            if (selectedGas.isEmpty()) {
                selectedGas = drainedGas.copyWithAmount(1);
            }
            drainedAmount = BoundedMath.saturatedAdd(drainedAmount, drainedGas.getAmount());
        }

        if (selectedGas.isEmpty() || drainedAmount <= 0) {
            return GasStack.EMPTY;
        }

        return selectedGas.copyWithAmount(Math.min(maxAmount, drainedAmount));
    }

    static GasStack drainGasForPackaging(GasHandler handler, GasStack requestedGas, long maxAmount) {
        if (requestedGas.isEmpty() || maxAmount <= 0) {
            return GasStack.EMPTY;
        }

        long remainingAmount = Math.min(maxAmount, requestedGas.getAmount());
        long drainedAmount = 0;
        int tankCount = Math.max(0, handler.getTanks());
        for (int tankIndex = 0; tankIndex < tankCount && remainingAmount > 0; tankIndex++) {
            GasStack tankGas = handler.getGasInTank(tankIndex);
            if (tankGas.isEmpty() || !GasStack.isSameGasSameComponents(tankGas, requestedGas)) {
                continue;
            }

            Optional<GasPressureTransferEndpoint> endpoint = packageableEndpoint(handler, tankIndex, tankGas);
            if (endpoint.isEmpty()) {
                continue;
            }

            GasPressureTransferEndpoint source = endpoint.get();
            long requestedFromTank = Math.min(remainingAmount, tankGas.getAmount());
            long simulatedAmount = source.simulateDrainAmount(tankGas, requestedFromTank);
            if (simulatedAmount <= 0) {
                continue;
            }

            GasStack drainedGas = source.executeDrain(tankGas, Math.min(requestedFromTank, simulatedAmount));
            if (drainedGas.isEmpty() || !GasStack.isSameGasSameComponents(drainedGas, requestedGas)) {
                continue;
            }

            long acceptedAmount = Math.min(remainingAmount, drainedGas.getAmount());
            drainedAmount = BoundedMath.saturatedAdd(drainedAmount, acceptedAmount);
            remainingAmount -= acceptedAmount;
        }

        if (drainedAmount <= 0) {
            return GasStack.EMPTY;
        }

        return requestedGas.copyWithAmount(drainedAmount);
    }

    static @Unmodifiable List<PackagingTankSnapshot> snapshotPackagingTanks(GasHandler handler) {
        int tankCount = Math.max(0, handler.getTanks());
        List<PackagingTankSnapshot> tankSnapshot = new ArrayList<>(tankCount);
        for (int tankIndex = 0; tankIndex < tankCount; tankIndex++) {
            GasStack gas = handler.getGasInTank(tankIndex).copy();
            long amount = gas.isEmpty() ? 0 : packageableEndpoint(handler, tankIndex, gas).map(endpoint -> Math.min(gas.getAmount(), endpoint.simulateDrainAmount(gas, gas.getAmount()))).orElse(0L);
            tankSnapshot.add(new PackagingTankSnapshot(gas, amount));
        }
        return List.copyOf(tankSnapshot);
    }

    static boolean matchesPackagingTankSnapshots(List<PackagingTankSnapshot> first, List<PackagingTankSnapshot> second) {
        if (first.size() != second.size()) {
            return false;
        }

        for (int tankIndex = 0; tankIndex < first.size(); tankIndex++) {
            PackagingTankSnapshot firstTank = first.get(tankIndex);
            PackagingTankSnapshot secondTank = second.get(tankIndex);
            if (firstTank.packageableAmount() == secondTank.packageableAmount() && GasStack.matches(firstTank.gas(), secondTank.gas())) {
                continue;
            }

            return false;
        }

        return true;
    }

    record PackagingTankSnapshot(GasStack gas, long packageableAmount) {
        PackagingTankSnapshot {
            gas = gas.copy();
            packageableAmount = Mth.clamp(packageableAmount, 0, gas.getAmount());
        }
    }

    private static Optional<GasPressureTransferEndpoint> packageableEndpoint(GasHandler handler, int tankIndex, GasStack tankGas) {
        Optional<GasPressureTransferEndpoint> endpoint = GasPressureTransferEndpoint.tryHandler(handler, tankIndex);
        if (endpoint.isEmpty() || endpoint.get().getDrainPressurePa(tankGas) <= GasPressure.VACUUM_PA) {
            return Optional.empty();
        }

        return endpoint;
    }
}
