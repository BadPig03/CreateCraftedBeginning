package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.content.airtights.gas.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.content.airtights.gas.transaction.MachineResourceSnapshots.GasTankSnapshot;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.core.transaction.ResourceTransaction.Participant;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberTransactions {
    private GasInjectionChamberTransactions() {
    }

    public static Participant<GasTankSnapshot> operationGasParticipant(GasInjectionChamberBlockEntity chamber, GasInjectionChamberOperationState operation, Provider provider) {
        IGasTank tank = chamber.getGasTank();
        return ResourceTransaction.participant(() -> !operation.gas.isEmpty() && GasStack.matches(tank.drain(operation.gas, GasAction.SIMULATE), operation.gas), () -> MachineResourceSnapshots.snapshotGasTanks(provider, chamber.getGasTankBehaviour()), () -> !operation.gas.isEmpty() && GasStack.matches(tank.drain(operation.gas, GasAction.EXECUTE), operation.gas), snapshot -> MachineResourceSnapshots.restoreGasTanks(provider, snapshot, chamber.getGasTankBehaviour()));
    }
}
