package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer;
import net.ty.createcraftedbeginning.api.canister.GasCanisterContainer.InjectionMode;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferEndpoint;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferService;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureTransferService.EquilibriumPolicy;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberCanisterTransfer {
    private GasInjectionChamberCanisterTransfer() {
    }

    public static long getTransferableAmount(GasPressureCompartment source, GasCanisterContainer canister, GasStack expectedGas, long maxAmount) {
        GasStack sourceGas = source.getGasStack();
        if (expectedGas.isEmpty() || sourceGas.isEmpty() || !GasStack.isSameGasSameComponents(sourceGas, expectedGas) || maxAmount <= 0 || canister.getInjectionMode() == InjectionMode.DENY || canister.getTanks() <= 0) {
            return 0;
        }

        GasPressureTransferEndpoint sourceEndpoint = GasPressureTransferEndpoint.compartment(source);
        GasPressureTransferEndpoint targetEndpoint = GasPressureTransferEndpoint.canister(canister, 0);
        return GasPressureTransferService.transferPassive(sourceEndpoint, targetEndpoint, maxAmount, EquilibriumPolicy.BOUNDED_FINAL_UNIT, GasAction.SIMULATE).transferableAmount();
    }

    public static boolean transferExactly(GasPressureCompartment source, GasCanisterContainer canister, GasStack expectedGas, long amount) {
        if (!canTransferExactly(source, canister, expectedGas, amount)) {
            return false;
        }

        GasPressureTransferEndpoint sourceEndpoint = GasPressureTransferEndpoint.compartment(source);
        GasPressureTransferEndpoint targetEndpoint = GasPressureTransferEndpoint.canister(canister, 0);
        return GasPressureTransferService.transferPassive(sourceEndpoint, targetEndpoint, amount, EquilibriumPolicy.BOUNDED_FINAL_UNIT, GasAction.EXECUTE).filledAmount() == amount;
    }

    static boolean canTransferExactly(GasPressureCompartment source, GasCanisterContainer canister, GasStack expectedGas, long amount) {
        return amount > 0 && getTransferableAmount(source, canister, expectedGas, amount) == amount;
    }
}
