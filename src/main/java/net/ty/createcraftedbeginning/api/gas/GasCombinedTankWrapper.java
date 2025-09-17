package net.ty.createcraftedbeginning.api.gas;

import net.createmod.catnip.data.Iterate;
import net.ty.createcraftedbeginning.api.gas.interfaces.IGasHandler;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class GasCombinedTankWrapper implements IGasHandler {
    protected final IGasHandler[] itemHandler;
    protected final int[] baseIndex;
    protected final int tankCount;
    protected boolean enforceVariety;

    public GasCombinedTankWrapper(IGasHandler @NotNull ... gasHandlers) {
        this.itemHandler = gasHandlers;
        this.baseIndex = new int[gasHandlers.length];
        int index = 0;
        for (int i = 0; i < gasHandlers.length; i++) {
            index += gasHandlers[i].getTanks();
            baseIndex[i] = index;
        }
        this.tankCount = index;
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
        IGasHandler handler = getHandlerFromIndex(index);
        tank = getSlotFromIndex(tank, index);
        return handler.getGasInTank(tank);
    }

    @Override
    public long getTankCapacity(int tank) {
        int index = getIndexForSlot(tank);
        IGasHandler handler = getHandlerFromIndex(index);
        int localSlot = getSlotFromIndex(tank, index);
        return handler.getTankCapacity(localSlot);
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        int index = getIndexForSlot(tank);
        IGasHandler handler = getHandlerFromIndex(index);
        int localSlot = getSlotFromIndex(tank, index);
        return handler.isGasValid(localSlot, stack);
    }

    @Override
    public long fill(@NotNull GasStack resource, GasAction action) {
		if (resource.isEmpty()) {
			return 0;
		}

        long filled = 0;
        resource = resource.copy();

        boolean fittingHandlerFound = false;
        Outer:
        for (boolean searchPass : Iterate.trueAndFalse) {
            for (IGasHandler gasHandler : itemHandler) {

				for (int i = 0; i < gasHandler.getTanks(); i++) {
					if (searchPass && GasStack.isSameGas(gasHandler.getGasInTank(i), resource)) {
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
					break Outer;
				}
				if (fittingHandlerFound && (enforceVariety || filledIntoCurrent != 0)) {
					break Outer;
				}
            }
        }

        return filled;
    }

    @Override
    public GasStack drain(@NotNull GasStack resource, GasAction action) {
		if (resource.isEmpty()) {
			return resource;
		}

        GasStack drained = GasStack.EMPTY;
        resource = resource.copy();

        for (IGasHandler gasHandler : itemHandler) {
            GasStack drainedFromCurrent = gasHandler.drain(resource, action);
            long amount = drainedFromCurrent.getAmount();
            resource.shrink(amount);

			if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || GasStack.isSameGas(drainedFromCurrent, drained))) {
				drained = new GasStack(drainedFromCurrent.getGasHolder(), amount + drained.getAmount());
			}
			if (resource.isEmpty()) {
				break;
			}
        }

        return drained;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        GasStack drained = GasStack.EMPTY;

        for (IGasHandler gasHandler : itemHandler) {
            GasStack drainedFromCurrent = gasHandler.drain(maxDrain, action);
            long amount = drainedFromCurrent.getAmount();
            maxDrain -= amount;

			if (!drainedFromCurrent.isEmpty() && (drained.isEmpty() || GasStack.isSameGas(drainedFromCurrent, drained))) {
				drained = new GasStack(drainedFromCurrent.getGasHolder(), amount + drained.getAmount());
			}
			if (maxDrain == 0) {
				break;
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
		if (index < 0 || index >= itemHandler.length) {
			return EmptyGasHandler.INSTANCE;
		}
        return itemHandler[index];
    }

    protected int getSlotFromIndex(int slot, int index) {
		if (index <= 0 || index >= baseIndex.length) {
			return slot;
		}
        return slot - baseIndex[index - 1];
    }
}
