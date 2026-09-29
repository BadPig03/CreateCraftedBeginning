package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockPart;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankStorageController {
    private final AirtightTankBlockEntity owner;

    AirtightTankStorageController(AirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    void resizeToBlocks(int blockCount) {
        GasTank tank = owner.getTankInventory();
        tank.tryReconfigure(limitsForBlocks(tank, blockCount)).requireAccepted();
    }

    void setVolumeForStructure() {
        GasTank tank = owner.getTankInventory();
        tank.tryReconfigure(limitsForStructure()).requireAccepted();
    }

    GasTankLimits limitsForStructure() {
        return limitsForBlocks(owner.getTankInventory(), owner.getTotalTankSize());
    }

    void mergeTankStateFrom(GasTankMultiblockPart source) {
        if (!source.hasTank() || !(source.getTank(0) instanceof GasTank sourceTank)) {
            return;
        }

        GasStack sourceGas = sourceTank.getGasStack();
        if (sourceGas.isEmpty()) {
            return;
        }

        GasTank targetTank = owner.getTankInventory();
        GasStack targetGas = targetTank.getGasStack();
        if (!targetGas.isEmpty() && !GasStack.isSameGasSameComponents(targetGas, sourceGas)) {
            return;
        }

        long mergedAmount = BoundedMath.saturatedAdd(targetGas.getAmount(), sourceGas.getAmount());
        GasStack mergedGas = targetGas.isEmpty() ? sourceGas.copyWithAmount(mergedAmount) : targetGas.copyWithAmount(mergedAmount);
        GasTankState targetState = new GasTankState(targetTank.getLimits(), mergedGas);
        GasTankState sourceState = new GasTankState(sourceTank.getLimits(), GasStack.EMPTY);
        if (!targetTank.canContain(targetState.limits(), targetState.contents()) || !sourceTank.canContain(sourceState.limits(), sourceState.contents())) {
            return;
        }

        targetTank.tryApplyState(targetState).requireAccepted();
        sourceTank.tryApplyState(sourceState).requireAccepted();
    }

    GasStack prepareTankStateForSplit() {
        GasTank tank = owner.getTankInventory();
        GasStack state = tank.getGasStack();
        GasTankState standaloneState = new GasTankState(limitsForBlocks(tank, 1), GasStack.EMPTY);
        tank.tryApplyState(standaloneState).requireAccepted();
        return state;
    }

    void applySplitTankState(GasStack state, int remainingTankCount) {
        if (remainingTankCount <= 0) {
            return;
        }

        GasTank tank = owner.getTankInventory();
        GasStack assignedGas = GasStack.EMPTY;
        long amount = 0;
        if (!state.isEmpty()) {
            amount = remainingTankCount == 1 ? state.getAmount() : state.getAmount() / remainingTankCount;
            assignedGas = state.copyWithAmount(amount);
        }

        GasTankState splitState = new GasTankState(tank.getLimits(), assignedGas);
        tank.tryApplyState(splitState).requireAccepted();
        if (amount <= 0) {
            return;
        }

        state.shrink(amount);
    }

    private static GasTankLimits limitsForBlocks(GasTank tank, int blockCount) {
        if (blockCount < 0) {
            throw new IllegalArgumentException("Airtight tank block count must be non-negative; got " + blockCount + '.');
        }

        long volume = Math.multiplyExact(blockCount, AirtightTankBlockEntity.getVolumePerBlock());
        return new GasTankLimits(volume, tank.getMaxPressurePa());
    }
}
