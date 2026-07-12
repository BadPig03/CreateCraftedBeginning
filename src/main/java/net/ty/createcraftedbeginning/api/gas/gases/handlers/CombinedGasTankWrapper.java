package net.ty.createcraftedbeginning.api.gas.gases.handlers;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
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
    public boolean isGasValid(int tank, GasStack stack) {
        int index = getIndexForSlot(tank);
        int localSlot = getSlotFromIndex(tank, index);
        return getHandlerFromIndex(index).isGasValid(localSlot, stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return GasStack.EMPTY;
        }

        long remaining = resource.getAmount();
        GasStack total = GasStack.EMPTY;
        for (IGasHandler handler : gasHandlers) {
            if (remaining <= 0) {
                break;
            }

            GasStack request = resource.copyWithAmount(remaining);
            GasStack preview = handler.drain(request, GasAction.SIMULATE);
            if (preview.isEmpty() || !GasStack.isSameGasSameComponents(preview, resource)) {
                continue;
            }

            GasStack part = action.simulate() ? preview : handler.drain(preview, GasAction.EXECUTE);
            if (part.isEmpty() || !GasStack.isSameGasSameComponents(part, resource)) {
                continue;
            }

            if (total.isEmpty()) {
                total = part.copy();
            }
            else {
                total.grow(part.getAmount());
            }
            remaining -= part.getAmount();
        }

        return total;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (maxDrain <= 0) {
            return GasStack.EMPTY;
        }

        long remaining = maxDrain;
        GasStack total = GasStack.EMPTY;
        for (IGasHandler handler : gasHandlers) {
            if (remaining <= 0) {
                break;
            }

            GasStack preview;
            if (total.isEmpty()) {
                preview = handler.drain(remaining, GasAction.SIMULATE);
            }
            else {
                preview = handler.drain(total.copyWithAmount(remaining), GasAction.SIMULATE);
            }

            if (preview.isEmpty() || !total.isEmpty() && !GasStack.isSameGasSameComponents(preview, total)) {
                continue;
            }

            GasStack request = preview.copyWithAmount(Math.min(remaining, preview.getAmount()));
            GasStack part = action.simulate() ? request : handler.drain(request, GasAction.EXECUTE);
            if (part.isEmpty() || !total.isEmpty() && !GasStack.isSameGasSameComponents(part, total)) {
                continue;
            }

            if (total.isEmpty()) {
                total = part.copy();
            }
            else {
                total.grow(part.getAmount());
            }
            remaining -= part.getAmount();
        }
        return total;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        int index = getIndexForSlot(tank);
        tank = getSlotFromIndex(tank, index);
        return getHandlerFromIndex(index).getGasInTank(tank);
    }

    @Override
    public int getTanks() {
        return tankCount;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        long filled = 0;
        resource = resource.copy();
        boolean found = false;
        for (boolean searchPass : Iterate.trueAndFalse) {
            for (IGasHandler gasHandler : gasHandlers) {
                for (int i = 0; i < gasHandler.getTanks(); i++) {
                    if (searchPass && GasStack.isSameGasSameComponents(gasHandler.getGasInTank(i), resource)) {
                        found = true;
                    }
                }

                if (searchPass && !found) {
                    continue;
                }

                long filledIntoCurrent = gasHandler.fill(resource, action);
                resource.shrink(filledIntoCurrent);
                filled += filledIntoCurrent;
                if (resource.isEmpty()) {
                    return filled;
                }
                if (found && (enforceVariety || filledIntoCurrent != 0)) {
                    return filled;
                }
            }
        }

        return filled;
    }

    @Override
    public long getTankCapacity(int tank) {
        int index = getIndexForSlot(tank);
        int localSlot = getSlotFromIndex(tank, index);
        return getHandlerFromIndex(index).getTankCapacity(localSlot);
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
