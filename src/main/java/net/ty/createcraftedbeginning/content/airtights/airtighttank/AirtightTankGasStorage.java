package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.EmptyGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTankGasStorage {
    private final AbstractAirtightTankBlockEntity owner;
    private final IGasHandler gasCapability = new ControllerAwareGasHandler();
    private GasTank tankInventory;

    public AirtightTankGasStorage(AbstractAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    public void initialize(GasTank tankInventory) {
        this.tankInventory = tankInventory;
        refreshCapability();
    }

    public GasTank getTankInventory() {
        return tankInventory;
    }

    public IGasHandler getCapability() {
        return gasCapability;
    }

    public void refreshCapability() {
        owner.invalidateGasCapabilities();
    }

    public void invalidate() {
        owner.invalidateGasCapabilities();
    }

    public void onGasStackChanged(GasStack ignored) {
        if (!owner.isController() || owner.getLevel() == null || owner.getLevel().isClientSide) {
            return;
        }

        owner.notifyUpdate();
    }

    private final class ControllerAwareGasHandler implements IGasHandler {
        @Override
        public boolean isGasValid(int tank, GasStack stack) {
            return resolveHandler().isGasValid(tank, stack);
        }

        @Override
        public GasStack drain(GasStack resource, GasAction action) {
            return resolveHandler().drain(resource, action);
        }

        @Override
        public GasStack drain(long maxDrain, GasAction action) {
            return resolveHandler().drain(maxDrain, action);
        }

        @Override
        public GasStack getGasInTank(int tank) {
            return resolveHandler().getGasInTank(tank);
        }

        @Override
        public int getTanks() {
            return resolveHandler().getTanks();
        }

        @Override
        public long fill(GasStack resource, GasAction action) {
            return resolveHandler().fill(resource, action);
        }

        @Override
        public AtomicFillResult tryFillAtomically(List<GasStack> resources, GasAction action) {
            return resolveHandler().tryFillAtomically(resources, action);
        }

        @Override
        public long getTankCapacity(int tank) {
            return resolveHandler().getTankCapacity(tank);
        }

        private IGasHandler resolveHandler() {
            if (owner.isRemoved()) {
                return EmptyGasHandler.INSTANCE;
            }
            if (owner.isController()) {
                return tankInventory != null ? tankInventory : EmptyGasHandler.INSTANCE;
            }

            AbstractAirtightTankBlockEntity controllerTank = owner.getControllerBE();
            if (controllerTank == null || controllerTank.isRemoved() || !controllerTank.isController()) {
                return EmptyGasHandler.INSTANCE;
            }
            return controllerTank.getTankInventory();
        }
    }
}
