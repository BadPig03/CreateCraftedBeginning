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

    public static Participant<GasTankSnapshot> gasParticipant(GasInjectionChamberBlockEntity chamber, GasStack request, Provider provider) {
        IGasTank gasTank = chamber.getGasTank();
        return ResourceTransaction.participant(() -> !request.isEmpty() && GasStack.matches(gasTank.drain(request, GasAction.SIMULATE), request), () -> MachineResourceSnapshots.snapshotGasTanks(provider, chamber.getGasTankBehaviour()), () -> !request.isEmpty() && GasStack.matches(gasTank.drain(request, GasAction.EXECUTE), request), snapshot -> MachineResourceSnapshots.restoreGasTanks(provider, snapshot, chamber.getGasTankBehaviour()));
    }
}
