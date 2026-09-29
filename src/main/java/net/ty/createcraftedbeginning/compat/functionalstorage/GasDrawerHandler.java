package net.ty.createcraftedbeginning.compat.functionalstorage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.gas.storage.GasPressureCompartmentView;
import net.ty.createcraftedbeginning.gas.storage.GasTankLimits;
import net.ty.createcraftedbeginning.gas.storage.GasTankState;
import net.ty.createcraftedbeginning.gas.transfer.GasPressureFillService;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.IntFunction;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasDrawerHandler implements GasStorageHandler {
    private final GasDrawerBlockEntity owner;
    private final GasDrawerTank[] tanks;
    private final GasPressureCompartment[] pressureCompartments;

    public GasDrawerHandler(GasDrawerBlockEntity owner, int size, IntFunction<GasDrawerTank> tankFactory) {
        this.owner = owner;
        tanks = new GasDrawerTank[size];
        pressureCompartments = new GasPressureCompartment[size];
        for (int tankIndex = 0; tankIndex < size; tankIndex++) {
            tanks[tankIndex] = tankFactory.apply(tankIndex);
            pressureCompartments[tankIndex] = new RoutedPressureCompartment(tankIndex);
        }
    }

    @Override
    public GasPressureCompartment getPressureCompartment(int tank) {
        if (!isValidTank(tank)) {
            throw new IndexOutOfBoundsException("Tank index must be in [0, " + tanks.length + "); got " + tank + '.');
        }

        return pressureCompartments[tank];
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

    @Override
    public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
        if (!hasResources(resources)) {
            return AtomicFillResult.SUCCESS;
        }

        GasTankState[] stateSnapshot = snapshotStates();
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
        }
        finally {
            if (!shouldCommit) {
                restoreStates(stateSnapshot);
            }
            owner.endTransaction(shouldCommit);
        }
    }

    @Override
    public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
        if (!hasResources(resources)) {
            return AtomicFillResult.SUCCESS;
        }

        GasTankState[] stateSnapshot = snapshotStates();
        owner.beginTransaction();
        boolean filledAllResources;
        boolean shouldCommit = false;
        try {
            filledAllResources = GasPressureFillService.fillAllFromFixedPressure(this, resources, sourcePressurePa);
            shouldCommit = filledAllResources && action.execute();
            if (filledAllResources) {
                return AtomicFillResult.SUCCESS;
            }

            return AtomicFillResult.REJECTED;
        }
        finally {
            if (!shouldCommit) {
                restoreStates(stateSnapshot);
            }
            owner.endTransaction(shouldCommit);
        }
    }

    @Override
    public long getTankVolume(int tank) {
        if (!isValidTank(tank)) {
            return 0;
        }

        return tanks[tank].getTankVolume(0);
    }

    @Override
    public long getTankPressurePa(int tank) {
        if (!isValidTank(tank)) {
            return 0;
        }

        return tanks[tank].getTankPressurePa(0);
    }

    @Override
    public long getTankMaxPressurePa(int tank) {
        if (!isValidTank(tank)) {
            return 0;
        }

        return tanks[tank].getTankMaxPressurePa(0);
    }

    @Override
    public long getTankMaxAmount(int tank) {
        if (!isValidTank(tank)) {
            return 0;
        }

        return tanks[tank].getTankMaxAmount(0);
    }

    @Override
    public PressureModel getTankPressureModel(int tank) {
        if (!isValidTank(tank)) {
            return PressureModel.VARIABLE;
        }

        return tanks[tank].getPressureModel();
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

    public GasTankState[] snapshotStates() {
        GasTankState[] stateSnapshot = new GasTankState[tanks.length];
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            stateSnapshot[tankIndex] = tanks[tankIndex].snapshot();
        }
        return stateSnapshot;
    }

    public void validateStates(GasTankState[] stateSnapshot) {
        if (stateSnapshot.length != tanks.length) {
            throw new IllegalArgumentException("Gas drawer state count mismatch: expected " + tanks.length + ", got " + stateSnapshot.length + '.');
        }

        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            GasTankState state = stateSnapshot[tankIndex];
            if (state == null || !tanks[tankIndex].canContain(state.limits(), state.contents())) {
                throw new IllegalStateException("Gas drawer state at tank index " + tankIndex + " is missing or cannot be contained by the tank.");
            }
        }
    }

    public void restoreStates(GasTankState[] stateSnapshot) {
        validateStates(stateSnapshot);
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            tanks[tankIndex].tryApplyState(stateSnapshot[tankIndex]).requireAccepted();
        }
    }

    public void reconfigure(GasTankLimits limits) {
        GasTankState[] states = new GasTankState[tanks.length];
        for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
            states[tankIndex] = new GasTankState(limits, tanks[tankIndex].getStoredStack());
        }
        restoreStates(states);
    }

    public void beginTransaction() {
        owner.beginTransaction();
    }

    public void endTransaction(boolean commit) {
        owner.endTransaction(commit);
    }

    private static long fillTank(GasDrawerTank tank, GasStack resource, GasAction action) {
        long acceptedAmount = tank.fill(resource, GasAction.SIMULATE);
        if (acceptedAmount <= 0 || !action.execute()) {
            return acceptedAmount;
        }

        return tank.fill(resource, GasAction.EXECUTE);
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

    private boolean fillAll(List<GasStack> resources) {
        for (GasStack gasStack : resources) {
            if (gasStack == null || gasStack.isEmpty() || fill(gasStack, GasAction.EXECUTE) == gasStack.getAmount()) {
                continue;
            }

            return false;
        }
        return true;
    }

    private final class RoutedPressureCompartment extends GasPressureCompartmentView {
        private final int tankIndex;

        private RoutedPressureCompartment(int tankIndex) {
            super(tanks[tankIndex]);
            this.tankIndex = tankIndex;
        }

        @Override
        public @Nullable PredictedTransferLimits predictTransferLimits(GasStack gas, long storedAmount) {
            PredictedTransferLimits limits = tanks[tankIndex].predictTransferLimits(gas, storedAmount);
            if (limits.drainLimit() <= 0 && limits.fillLimit() <= 0) {
                return limits;
            }

            long amount = Math.max(0, Math.min(getMaxAmount(), storedAmount));
            GasStack projectedGas = amount == 0 ? GasStack.EMPTY : gas.copyWithAmount(amount);
            // Apply the same first-matching-slot rule to the projected contents. Delegating
            // directly to the tank would expose sibling slots that the live pressure view blocks.
            for (int index = 0; index < tanks.length; index++) {
                GasStack contents = index == tankIndex ? projectedGas : tanks[index].getStoredStack();
                if (contents.isEmpty() || !GasStack.isSameGasSameComponents(contents, gas)) {
                    continue;
                }
                return index == tankIndex ? limits : blockedProjection(gas, amount);
            }

            // With no matching stored gas, only the first eligible empty slot can fill.
            for (int index = 0; index < tanks.length; index++) {
                if (index == tankIndex) {
                    if (limits.fillLimit() > 0) {
                        return new PredictedTransferLimits(0, limits.fillLimit());
                    }
                }
                else if (tanks[index].getStoredStack().isEmpty() && tanks[index].fill(gas.copyWithAmount(1), GasAction.SIMULATE) > 0) {
                    return blockedProjection(gas, amount);
                }
            }
            return new PredictedTransferLimits(0, 0);
        }

        private @Nullable PredictedTransferLimits blockedProjection(GasStack gas, long amount) {
            // Emptying the selected slot can activate another compartment. The per-compartment
            // prediction API cannot rebuild that sibling's endpoints; do not claim equilibrium.
            if (amount == 0 && canPressureDrainTank(tankIndex, gas)) {
                return null;
            }
            return new PredictedTransferLimits(0, 0);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (!canPressureDrainTank(tankIndex, resource)) {
                return GasStack.EMPTY;
            }

            return super.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            GasStack storedGas = getGasStack();
            if (!canPressureDrainTank(tankIndex, storedGas)) {
                return GasStack.EMPTY;
            }

            return super.drain(maxDrain, action);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (!canPressureFillTank(tankIndex, resource)) {
                return 0;
            }

            return super.fill(resource, action);
        }

        @Override
        public boolean isGasValid(GasStack stack) {
            return canPressureFillTank(tankIndex, stack);
        }

        private boolean canPressureDrainTank(int sourceTank, GasStack resource) {
            if (!isValidTank(sourceTank) || resource.isEmpty()) {
                return false;
            }

            for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
                GasDrawerTank candidate = tanks[tankIndex];
                GasStack storedGas = candidate.getStoredStack();
                if (storedGas.isEmpty() || !GasStack.isSameGasSameComponents(storedGas, resource)) {
                    continue;
                }

                return tankIndex == sourceTank;
            }
            return false;
        }

        private boolean canPressureFillTank(int targetTank, GasStack resource) {
            if (!isValidTank(targetTank) || resource.isEmpty()) {
                return false;
            }

            for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
                GasDrawerTank candidate = tanks[tankIndex];
                GasStack storedGas = candidate.getStoredStack();
                if (storedGas.isEmpty() || !GasStack.isSameGasSameComponents(storedGas, resource)) {
                    continue;
                }

                return tankIndex == targetTank && candidate.fill(resource.copyWithAmount(1), GasAction.SIMULATE) > 0;
            }

            for (int tankIndex = 0; tankIndex < tanks.length; tankIndex++) {
                GasDrawerTank candidate = tanks[tankIndex];
                if (!candidate.getStoredStack().isEmpty() || candidate.fill(resource.copyWithAmount(1), GasAction.SIMULATE) <= 0) {
                    continue;
                }

                return tankIndex == targetTank;
            }
            return false;
        }
    }

    private boolean isValidTank(int tank) {
        return tank >= 0 && tank < tanks.length;
    }
}
