package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasTank;
import net.ty.createcraftedbeginning.core.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.MachineResourceSnapshots.GasTankSnapshot;
import net.ty.createcraftedbeginning.core.ResourceTransaction;
import net.ty.createcraftedbeginning.core.Participant;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberTransactions {
    private GasInjectionChamberTransactions() {
    }

    static Participant<GasTankSnapshot> gasParticipant(GasInjectionChamberBlockEntity chamber, GasStack request) {
        IGasTank gasTank = chamber.getGasTank();
        return ResourceTransaction.participant(() -> !request.isEmpty() && GasStack.matches(gasTank.drain(request, GasAction.SIMULATE), request), () -> MachineResourceSnapshots.snapshotGasTanks(chamber.getGasTankBehaviour()), () -> !request.isEmpty() && GasStack.matches(gasTank.drain(request, GasAction.EXECUTE), request), snapshot -> MachineResourceSnapshots.restoreGasTanks(snapshot, chamber.getGasTankBehaviour()));
    }
}
