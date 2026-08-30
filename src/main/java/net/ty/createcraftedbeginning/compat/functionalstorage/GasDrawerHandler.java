package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.IntFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerHandler implements IGasHandler {
    private final GasDrawerBlockEntity owner;
    private final GasDrawerTank[] tanks;

    public GasDrawerHandler(GasDrawerBlockEntity owner, int size, IntFunction<GasDrawerTank> tankFactory) {
        this.owner = owner;
        tanks = new GasDrawerTank[size];
        for (int tankIndex = 0; tankIndex < size; tankIndex++) {
            tanks[tankIndex] = tankFactory.apply(tankIndex);
        }
    }

    public static boolean hasResources(List<GasStack> resources) {
        for (GasStack gasStack : resources) {
            if (gasStack == null || gasStack.isEmpty()) {
                continue;
            }

            return true;
        }
        return false;
    }

    public GasDrawerTank[] getInternalTanks() {
        return tanks;
    }

    public GasDrawerTank getInternalTank(int tank) {
        return tanks[tank];
    }

    public boolean isEmpty() {
        for (GasDrawerTank tank : tanks) {
            if (tank.getStoredStack().isEmpty()) {
                continue;
            }

            return false;
        }
        return true;
    }

    @Override
    public boolean isGasValid(int tank, GasStack stack) {
        return isValidTank(tank) && tanks[tank].isGasValid(stack);
    }

    @Override
    public GasStack drain(GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return GasStack.EMPTY;
        }

        for (GasDrawerTank tank : tanks) {
            GasStack drainedGas = tank.drain(resource, action);
            if (drainedGas.isEmpty()) {
                continue;
            }

            return drainedGas;
        }
        return GasStack.EMPTY;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (maxDrain <= 0) {
            return GasStack.EMPTY;
        }

        for (GasDrawerTank tank : tanks) {
            GasStack drainedGas = tank.drain(maxDrain, action);
            if (drainedGas.isEmpty()) {
                continue;
            }

            return drainedGas;
        }
        return GasStack.EMPTY;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        if (!isValidTank(tank)) {
            return GasStack.EMPTY;
        }
        return tanks[tank].getGasInTank(0);
    }

    @Override
    public int getTanks() {
        return tanks.length;
    }

    @Override
    public long fill(GasStack resource, GasAction action) {
        if (resource.isEmpty()) {
            return 0;
        }

        long acceptedAmount = fillExisting(resource, action);
        if (acceptedAmount > 0) {
            return acceptedAmount;
        }
        return fillEmpty(resource, action);
    }

    private long fillExisting(GasStack resource, GasAction action) {
        for (GasDrawerTank tank : tanks) {
            GasStack storedGas = tank.getStoredStack();
            if (storedGas.isEmpty() || !GasStack.isSameGasSameComponents(storedGas, resource)) {
                continue;
            }

            long acceptedAmount = fillTank(tank, resource, action);
            if (acceptedAmount <= 0) {
                continue;
            }

            return acceptedAmount;
        }
        return 0;
    }

    private long fillEmpty(GasStack resource, GasAction action) {
        for (GasDrawerTank tank : tanks) {
            if (!tank.getStoredStack().isEmpty() || !tank.isGasValid(resource)) {
                continue;
            }

            long acceptedAmount = fillTank(tank, resource, action);
            if (acceptedAmount <= 0) {
                continue;
            }

            return acceptedAmount;
        }
        return 0;
    }

    private static long fillTank(GasDrawerTank tank, GasStack resource, GasAction action) {
        long acceptedAmount = tank.fill(resource, GasAction.SIMULATE);
        if (acceptedAmount <= 0 || !action.execute()) {
            return acceptedAmount;
        }
        return tank.fill(resource, GasAction.EXECUTE);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        if (!hasResources(resources)) {
            return AtomicFillResult.SUCCESS;
        }

        GasStack[] contentSnapshot = snapshotContents();
        owner.beginTransaction();
        boolean filledAllResources;
        boolean shouldCommit = false;
        try {
            filledAllResources = fillAll(resources);
            shouldCommit = filledAllResources && action.execute();
            if (!filledAllResources) {
                return AtomicFillResult.REJECTED;
            }
            return AtomicFillResult.SUCCESS;
        } finally {
            if (!shouldCommit) {
                restoreContents(contentSnapshot);
            }
            owner.endTransaction(shouldCommit);
        }
    }

    @Override
    public long getTankCapacity(int tank) {
        if (!isValidTank(tank)) {
            return 0;
        }
        return tanks[tank].getTankCapacity(0);
    }

    private boolean fillAll(List<GasStack> resources) {
        for (GasStack gasStack : resources) {
            if (gasStack == null || gasStack.isEmpty() || fill(gasStack, GasAction.EXECUTE) == gasStack.getAmount()) {
                continue;
            }

            return false;
        }
        return true;
    }

    private boolean isValidTank(int tank) {
        return tank >= 0 && tank < tanks.length;
    }

    public GasStack[] snapshotContents() {
        GasStack[] contentSnapshot = new GasStack[tanks.length];
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            contentSnapshot[tankIndex] = tanks[tankIndex].getStoredStack().copy();
        }
        return contentSnapshot;
    }

    public void restoreContents(GasStack[] contentSnapshot) {
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            tanks[tankIndex].setGasStack(contentSnapshot[tankIndex].copy());
        }
    }

    public void beginTransaction() {
        owner.beginTransaction();
    }

    public void endTransaction(boolean commit) {
        owner.endTransaction(commit);
    }
}
