package net.ty.createcraftedbeginning.api.gas.gases;

import net.createmod.catnip.data.Iterate;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CombinedGasTankWrapper implements IGasHandler {
    protected final IGasHandler[] gasHandlers;
    protected final int[] baseIndex;
    protected final int tankCount;
    protected boolean enforceVariety;

    public CombinedGasTankWrapper(IGasHandler @NotNull ... gasHandlers) {
        this.gasHandlers = gasHandlers;
        baseIndex = new int[gasHandlers.length];
        int index = 0;
        for (int i = 0; i < gasHandlers.length; i++) {
            index += gasHandlers[i].getTanks();
            baseIndex[i] = index;
        }
        tankCount = index;
    }

    public void enforceVariety() {
        enforceVariety = true;
    }

    @Override
    public int getTanks() {
        return tankCount;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        int index = getIndexForSlot(tank);
        tank = getSlotFromIndex(tank, index);
        return getHandlerFromIndex(index).getGasInTank(tank);
    }

    @Override
    public long getTankCapacity(int tank) {
        int index = getIndexForSlot(tank);
        int localSlot = getSlotFromIndex(tank, index);
        return getHandlerFromIndex(index).getTankCapacity(localSlot);
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        int index = getIndexForSlot(tank);
        int localSlot = getSlotFromIndex(tank, index);
        return getHandlerFromIndex(index).isGasValid(localSlot, stack);
    }

    @Override
    public long fill(@NotNull GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        long filled = 0;
        resource = resource.copy();
        boolean fittingHandlerFound = false;
        for (boolean searchPass : Iterate.trueAndFalse) {
            for (IGasHandler gasHandler : gasHandlers) {
                for (int i = 0; i < gasHandler.getTanks(); i++) {
                    if (searchPass && GasStack.isSameGasSameComponents(gasHandler.getGasInTank(i), resource)) {
                        fittingHandlerFound = true;
                    }
                }

                if (searchPass && !fittingHandlerFound) {
                    continue;
                }

                long filledIntoCurrent = gasHandler.fill(resource, action);
                resource.shrink(filledIntoCurrent);
                filled += filledIntoCurrent;

                if (resource.isEmpty()) {
                    return filled;
                }
                if (fittingHandlerFound && (enforceVariety || filledIntoCurrent != 0)) {
                    return filled;
                }
            }
        }

        return filled;
    }

    @Override
    public GasStack drain(@NotNull GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return GasStack.EMPTY;
        }

        GasStack drained = GasStack.EMPTY;
        resource = resource.copy();
        for (IGasHandler gasHandler : gasHandlers) {
            GasStack drainedFromCurrent = gasHandler.drain(resource, action);
            long amount = drainedFromCurrent.getAmount();
            resource.shrink(amount);

            if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || GasStack.isSameGasSameComponents(drainedFromCurrent, drained))) {
                drained = new GasStack(drainedFromCurrent.getGasHolder(), amount + drained.getAmount());
            }
            if (resource.isEmpty()) {
                return drained;
            }
        }

        return drained;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        GasStack drained = GasStack.EMPTY;
        for (IGasHandler gasHandler : gasHandlers) {
            GasStack drainedFromCurrent = gasHandler.drain(maxDrain, action);
            long amount = drainedFromCurrent.getAmount();
            maxDrain -= amount;

            if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || GasStack.isSameGasSameComponents(drainedFromCurrent, drained))) {
                drained = new GasStack(drainedFromCurrent.getGasHolder(), amount + drained.getAmount());
            }
            if (maxDrain == 0) {
                return drained;
            }
        }

        return drained;
    }

    protected int getIndexForSlot(int slot) {
        if (slot < 0) {
            return -1;
        }

        for (int i = 0; i < baseIndex.length; i++) {
            if (slot - baseIndex[i] < 0) {
                return i;
            }
        }
        return -1;
    }

    protected IGasHandler getHandlerFromIndex(int index) {
        return index < 0 || index >= gasHandlers.length ? EmptyGasHandler.INSTANCE : gasHandlers[index];
    }

    protected int getSlotFromIndex(int slot, int index) {
        return index <= 0 || index >= baseIndex.length ? slot : slot - baseIndex[index - 1];
    }
}
