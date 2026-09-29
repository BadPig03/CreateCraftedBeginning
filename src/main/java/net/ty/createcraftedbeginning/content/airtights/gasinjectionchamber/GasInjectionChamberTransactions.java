package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.foundation.transaction.ResourceTransaction;
import net.ty.createcraftedbeginning.foundation.transaction.TransactionParticipant;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots.GasTankSnapshot;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionChamberTransactions {
    private GasInjectionChamberTransactions() {
    }

    public static boolean pressureProfileMatches(long currentPressurePa, long sourcePressurePa) {
        return sourcePressurePa < 0 || GameplayPressureProfiles.resolve(currentPressurePa).equals(GameplayPressureProfiles.resolve(sourcePressurePa));
    }

    static TransactionParticipant<GasTankSnapshot> gasParticipant(GasInjectionChamberBlockEntity chamber, GasConsumptionPlan plan) {
        return ResourceTransaction.participant(plan::canExecute, () -> MachineResourceSnapshots.snapshotGasTanks(chamber.getGasTankBehaviour()), plan::execute, snapshot -> MachineResourceSnapshots.restoreGasTanks(snapshot, chamber.getGasTankBehaviour()));
    }

    static TransactionParticipant<GasTankSnapshot> gasParticipant(GasInjectionChamberBlockEntity chamber, GasStack request) {
        return gasParticipant(chamber, request, -1);
    }

    static TransactionParticipant<GasTankSnapshot> gasParticipant(GasInjectionChamberBlockEntity chamber, GasStack request, long sourcePressurePa) {
        GasPressureCompartment gasTank = chamber.getGasTank();
        return ResourceTransaction.participant(() -> pressureProfileMatches(gasTank.getPressurePa(), sourcePressurePa) && !request.isEmpty() && GasStack.matches(gasTank.drain(request, GasAction.SIMULATE), request), () -> MachineResourceSnapshots.snapshotGasTanks(chamber.getGasTankBehaviour()), () -> pressureProfileMatches(gasTank.getPressurePa(), sourcePressurePa) && !request.isEmpty() && GasStack.matches(gasTank.drain(request, GasAction.EXECUTE), request), snapshot -> MachineResourceSnapshots.restoreGasTanks(snapshot, chamber.getGasTankBehaviour()));
    }
}
