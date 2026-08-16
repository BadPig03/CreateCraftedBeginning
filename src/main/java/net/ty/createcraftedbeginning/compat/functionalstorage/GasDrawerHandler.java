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
        for (int tank = 0; tank < size; tank++) {
            tanks[tank] = tankFactory.apply(tank);
        }
    }

    public static boolean hasResources(List<GasStack> resources) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty()) {
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
            GasStack drained = tank.drain(resource, action);
            if (drained.isEmpty()) {
                continue;
            }

            return drained;
        }
        return GasStack.EMPTY;
    }

    @Override
    public GasStack drain(long maxDrain, GasAction action) {
        if (maxDrain <= 0) {
            return GasStack.EMPTY;
        }

        for (GasDrawerTank tank : tanks) {
            GasStack drained = tank.drain(maxDrain, action);
            if (drained.isEmpty()) {
                continue;
            }

            return drained;
        }
        return GasStack.EMPTY;
    }

    @Override
    public GasStack getGasInTank(int tank) {
        return isValidTank(tank) ? tanks[tank].getGasInTank(0) : GasStack.EMPTY;
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

        long accepted = fillExisting(resource, action);
        if (accepted > 0) {
            return accepted;
        }
        return fillEmpty(resource, action);
    }

    private long fillExisting(GasStack resource, GasAction action) {
        for (GasDrawerTank tank : tanks) {
            GasStack stored = tank.getStoredStack();
            if (stored.isEmpty() || !GasStack.isSameGasSameComponents(stored, resource)) {
                continue;
            }

            long accepted = fillTank(tank, resource, action);
            if (accepted <= 0) {
                continue;
            }

            return accepted;
        }
        return 0;
    }

    private long fillEmpty(GasStack resource, GasAction action) {
        for (GasDrawerTank tank : tanks) {
            if (!tank.getStoredStack().isEmpty() || !tank.isGasValid(resource)) {
                continue;
            }

            long accepted = fillTank(tank, resource, action);
            if (accepted <= 0) {
                continue;
            }

            return accepted;
        }
        return 0;
    }

    private static long fillTank(GasDrawerTank tank, GasStack resource, GasAction action) {
        long accepted = tank.fill(resource, GasAction.SIMULATE);
        if (accepted <= 0 || !action.execute()) {
            return accepted;
        }
        return tank.fill(resource, GasAction.EXECUTE);
    }

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        if (!hasResources(resources)) {
            return AtomicFillResult.SUCCESS;
        }

        GasStack[] snapshot = snapshotContents();
        owner.beginTransaction();
        boolean success;
        boolean commit = false;
        try {
            success = fillAll(resources);
            commit = success && action.execute();
            return success ? AtomicFillResult.SUCCESS : AtomicFillResult.REJECTED;
        } finally {
            if (!commit) {
                restoreContents(snapshot);
            }
            owner.endTransaction(commit);
        }
    }

    @Override
    public long getTankCapacity(int tank) {
        return isValidTank(tank) ? tanks[tank].getTankCapacity(0) : 0;
    }

    private boolean fillAll(List<GasStack> resources) {
        for (GasStack resource : resources) {
            if (resource == null || resource.isEmpty() || fill(resource, GasAction.EXECUTE) == resource.getAmount()) {
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
        GasStack[] snapshot = new GasStack[tanks.length];
        for (int tank = 0; tank < tanks.length; tank++) {
            snapshot[tank] = tanks[tank].getStoredStack().copy();
        }
        return snapshot;
    }

    public void restoreContents(GasStack[] snapshot) {
        for (int tank = 0; tank < tanks.length; tank++) {
            tanks[tank].setGasStack(snapshot[tank].copy());
        }
    }

    public void beginTransaction() {
        owner.beginTransaction();
    }

    public void endTransaction(boolean commit) {
        owner.endTransaction(commit);
    }
}
