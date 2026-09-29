package net.ty.createcraftedbeginning.gas.storage.handler;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.util.Mth;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.foundation.BoundedMath;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CombinedGasStorageHandler implements GasStorageHandler {
    protected final GasStorageHandler[] gasHandlers;
    protected final int[] baseIndex;
    protected final int tankCount;
    private boolean enforceVariety;

    public CombinedGasStorageHandler(GasStorageHandler @NotNull ... gasHandlers) {
        this.gasHandlers = gasHandlers;
        baseIndex = new int[gasHandlers.length];
        int totalTanks = 0;
        for (int i = 0; i < gasHandlers.length; i++) {
            totalTanks += gasHandlers[i].getTanks();
            baseIndex[i] = totalTanks;
        }
        tankCount = totalTanks;
    }

    @Override
    public GasPressureCompartment getPressureCompartment(int tank) {
        int index = getIndexForSlot(tank);
        if (index < 0 || index >= gasHandlers.length) {
            throw new IndexOutOfBoundsException("Tank index must be in [0, " + tankCount + "); got " + tank + '.');
        }

        int localTank = getSlotFromIndex(tank, index);
        GasPressureCompartment compartment = gasHandlers[index].getPressureCompartment(localTank);
        return new CombinedPressureCompartment(tank, compartment);
    }

    @Override
    public long getTankVolume(int tank) {
        int index = getIndexForSlot(tank);
        if (index < 0 || index >= gasHandlers.length) {
            return 0;
        }

        int localSlot = getSlotFromIndex(tank, index);
        return gasHandlers[index].getTankVolume(localSlot);
    }

    @Override
    public long getTankPressurePa(int tank) {
        int index = getIndexForSlot(tank);
        if (index < 0 || index >= gasHandlers.length) {
            return 0;
        }

        int localSlot = getSlotFromIndex(tank, index);
        return gasHandlers[index].getTankPressurePa(localSlot);
    }

    @Override
    public long getTankMaxPressurePa(int tank) {
        int index = getIndexForSlot(tank);
        if (index < 0 || index >= gasHandlers.length) {
            return 0;
        }

        int localSlot = getSlotFromIndex(tank, index);
        return gasHandlers[index].getTankMaxPressurePa(localSlot);
    }

    @Override
    public long getTankMaxAmount(int tank) {
        int index = getIndexForSlot(tank);
        if (index < 0 || index >= gasHandlers.length) {
            return 0;
        }

        int localSlot = getSlotFromIndex(tank, index);
        return gasHandlers[index].getTankMaxAmount(localSlot);
    }

    @Override
    public PressureModel getTankPressureModel(int tank) {
        int index = getIndexForSlot(tank);
        if (index < 0 || index >= gasHandlers.length) {
            return PressureModel.VARIABLE;
        }

        int localSlot = getSlotFromIndex(tank, index);
        return gasHandlers[index].getTankPressureModel(localSlot);
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
        for (GasStorageHandler handler : gasHandlers) {
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
        for (GasStorageHandler handler : gasHandlers) {
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
        int localTank = getSlotFromIndex(tank, index);
        return getHandlerFromIndex(index).getGasInTank(localTank);
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
        GasStack remaining = resource.copy();
        for (boolean searchPass : Iterate.trueAndFalse) {
            for (GasStorageHandler handler : gasHandlers) {
                boolean found = false;
                for (int tank = 0; tank < handler.getTanks(); tank++) {
                    if (!GasStack.isSameGasSameComponents(handler.getGasInTank(tank), remaining)) {
                        continue;
                    }

                    found = true;
                    break;
                }

                if (searchPass && !found) {
                    continue;
                }

                long filledIntoCurrent = handler.fill(remaining, action);
                filledIntoCurrent = Mth.clamp(filledIntoCurrent, 0L, remaining.getAmount());
                remaining.shrink(filledIntoCurrent);
                filled = BoundedMath.saturatedAdd(filled, filledIntoCurrent);
                if (remaining.isEmpty()) {
                    return filled;
                }

                if (!found || !enforceVariety && filledIntoCurrent == 0) {
                    continue;
                }

                return filled;
            }
        }
        return filled;
    }

    public void enforceVariety() {
        enforceVariety = true;
    }

    protected boolean canDrainPressureCompartment(int tank) {
        return true;
    }

    protected boolean canFillPressureCompartment(int tank) {
        return true;
    }

    protected int getIndexForSlot(int slot) {
        if (slot < 0) {
            return -1;
        }

        for (int i = 0; i < baseIndex.length; i++) {
            if (slot >= baseIndex[i]) {
                continue;
            }

            return i;
        }
        return -1;
    }

    protected GasHandler getHandlerFromIndex(int index) {
        if (index < 0 || index >= gasHandlers.length) {
            return EmptyGasHandler.INSTANCE;
        }

        return gasHandlers[index];
    }

    protected int getSlotFromIndex(int slot, int index) {
        if (index <= 0 || index >= baseIndex.length) {
            return slot;
        }

        return slot - baseIndex[index - 1];
    }

    private final class CombinedPressureCompartment implements GasPressureCompartment {
        private final int tank;
        private final GasPressureCompartment delegate;

        private CombinedPressureCompartment(int tank, GasPressureCompartment delegate) {
            this.tank = tank;
            this.delegate = delegate;
        }

        @Override
        public @Nullable PredictedTransferLimits predictTransferLimits(GasStack gas, long storedAmount) {
            long amount = Math.max(0, Math.min(getMaxAmount(), storedAmount));
            if (enforceVariety && delegate.getGasStack().isEmpty() != (amount == 0)) {
                return null;
            }

            PredictedTransferLimits limits = delegate.predictTransferLimits(gas, amount);
            if (limits == null) {
                return null;
            }

            long drain = canDrainPressureCompartment(tank) ? limits.drainLimit() : 0;
            long fill = canFillPressureCompartment(tank) && isGasValid(gas) ? limits.fillLimit() : 0;
            if (enforceVariety && amount == 0 && fill > 0 && fill(gas.copyWithAmount(1), GasAction.SIMULATE) == 0) {
                fill = 0;
            }
            return new PredictedTransferLimits(drain, fill);
        }

        @Override
        public Object getCompartmentIdentity() {
            return delegate.getCompartmentIdentity();
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            if (!canDrainPressureCompartment(tank)) {
                return GasStack.EMPTY;
            }

            return delegate.drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            if (!canDrainPressureCompartment(tank)) {
                return GasStack.EMPTY;
            }

            return delegate.drain(maxDrain, action);
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            if (!canFillPressureCompartment(tank) || resource.isEmpty() || !isGasValid(resource)) {
                return 0;
            }

            if (enforceVariety && delegate.getGasStack().isEmpty()) {
                for (int otherTank = 0; otherTank < tankCount; otherTank++) {
                    if (otherTank == tank) {
                        continue;
                    }

                    GasStack otherGas = CombinedGasStorageHandler.this.getGasInTank(otherTank);
                    if (otherGas.isEmpty() || !GasStack.isSameGasSameComponents(otherGas, resource)) {
                        continue;
                    }

                    return 0;
                }

                for (int earlierTank = 0; earlierTank < tank; earlierTank++) {
                    GasPressureCompartment earlierCompartment = getPressureCompartment(earlierTank);
                    if (earlierCompartment.fill(resource, GasAction.SIMULATE) <= 0) {
                        continue;
                    }

                    return 0;
                }
            }
            return delegate.fill(resource, action);
        }

        @Override
        public boolean isGasValid(GasStack stack) {
            return CombinedGasStorageHandler.this.isGasValid(tank, stack);
        }

        @Override
        public GasStack getGasStack() {
            return delegate.getGasStack();
        }

        @Override
        public boolean supportsExactDrainRecovery() {
            return delegate.supportsExactDrainRecovery();
        }

        @Override
        public long restoreDrainedGas(GasStack resource, GasAction action) {
            return delegate.restoreDrainedGas(resource, action);
        }

        @Override
        public long getVolume() {
            return delegate.getVolume();
        }

        @Override
        public long getPressurePa() {
            return delegate.getPressurePa();
        }

        @Override
        public long getMaxPressurePa() {
            return delegate.getMaxPressurePa();
        }

        @Override
        public long getMaxAmount() {
            return delegate.getMaxAmount();
        }

        @Override
        public long getStoredAmount() {
            return delegate.getStoredAmount();
        }

        @Override
        public PressureModel getPressureModel() {
            return delegate.getPressureModel();
        }
    }
}
