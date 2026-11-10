package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseCause;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import net.ty.createcraftedbeginning.gas.multiblock.GasTankMultiblockPart;
import net.ty.createcraftedbeginning.gas.release.GasReleaseRequest;
import net.ty.createcraftedbeginning.gas.release.GasReleaseService;
import net.ty.createcraftedbeginning.gas.storage.GasTank;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankStorageController {
    private final AirtightTankBlockEntity owner;
    private final List<GasReleaseRequest> pendingOverflowReleases = new ArrayList<>();
    private @Nullable GasReleaseRequest pendingContentsOverflow;

    AirtightTankStorageController(AirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    private static GasTankLimits limitsForBlocks(GasTank tank, int blockCount) {
        if (blockCount < 0) {
            throw new IllegalArgumentException("Airtight tank block count must be non-negative; got " + blockCount + '.');
        }

        long volume = Math.multiplyExact(blockCount, AirtightTankBlockEntity.getVolumePerBlock());
        return new GasTankLimits(volume, tank.getMaxPressurePa());
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

    List<GasReleaseRequest> getPendingOverflowReleases() {
        return List.copyOf(pendingOverflowReleases);
    }

    void setPendingOverflowReleases(List<GasReleaseRequest> releases, @Nullable GasReleaseRequest contentsOverflow) {
        pendingOverflowReleases.clear();
        pendingOverflowReleases.addAll(releases);
        pendingContentsOverflow = contentsOverflow;
    }

    void applyMountedState(GasTankState state) {
        GasTankLimits limits = new GasTankLimits(limitsForStructure().volumeLiters(), state.limits().maxPressurePa());
        GasStack contents = state.contents();
        long retainedAmount = Math.min(contents.getAmount(), limits.maxAmount());
        owner.getTankInventory().tryApplyState(new GasTankState(limits, contents.copyWithAmount(retainedAmount))).requireAccepted();
        if (pendingContentsOverflow != null) {
            pendingOverflowReleases.removeIf(release -> release == pendingContentsOverflow);
            pendingContentsOverflow = null;
        }
        long excessAmount = contents.getAmount() - retainedAmount;
        if (excessAmount > 0) {
            long sourcePressurePa = GasPressure.pressure(contents.getAmount(), limits.volumeLiters());
            pendingContentsOverflow = GasReleaseRequest.radial(contents.copyWithAmount(excessAmount), owner.getBlockPos(), GasReleaseCause.OVERFLOW, sourcePressurePa);
            pendingOverflowReleases.add(pendingContentsOverflow);
        }
        owner.setChanged();
    }

    void releasePendingOverflow() {
        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        while (!pendingOverflowReleases.isEmpty()) {
            GasReleaseRequest release = pendingOverflowReleases.removeFirst();
            if (release == pendingContentsOverflow) {
                pendingContentsOverflow = null;
            }
            owner.setChanged();
            GasReleaseService.release(level, release);
        }
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
}
