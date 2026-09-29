package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment.PressureModel;
import net.ty.createcraftedbeginning.gas.storage.GasTank;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankGasStorage {
    private static final GasStorageHandler EMPTY_HANDLER = new GasTank(0, 0);

    private final AbstractAirtightTankBlockEntity owner;
    private final GasStorageHandler gasCapability = new ControllerAwareGasHandler();

    private GasStorageHandler tankInventory;

    AirtightTankGasStorage(AbstractAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    void initialize(GasStorageHandler tankInventory) {
        this.tankInventory = tankInventory;
        refreshCapability();
    }

    GasStorageHandler getTankInventory() {
        return tankInventory;
    }

    GasStorageHandler getCapability() {
        return gasCapability;
    }

    void refreshCapability() {
        owner.invalidateGasCapabilities();
    }

    void invalidate() {
        owner.invalidateGasCapabilities();
    }

    void onTankStateChanged() {
        if (!owner.isController()) {
            return;
        }

        Level level = owner.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        owner.notifyUpdate();
    }

    private final class ControllerAwareGasHandler implements GasStorageHandler {
        @Override
        public GasPressureCompartment getPressureCompartment(int tank) {
            return resolveHandler().getPressureCompartment(tank);
        }

        @Override
        public long getTankVolume(int tank) {
            return resolveHandler().getTankVolume(tank);
        }

        @Override
        public long getTankPressurePa(int tank) {
            return resolveHandler().getTankPressurePa(tank);
        }

        @Override
        public long getTankMaxPressurePa(int tank) {
            return resolveHandler().getTankMaxPressurePa(tank);
        }

        @Override
        public long getTankMaxAmount(int tank) {
            return resolveHandler().getTankMaxAmount(tank);
        }

        @Override
        public PressureModel getTankPressureModel(int tank) {
            return resolveHandler().getTankPressureModel(tank);
        }

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
        public AtomicFillResult tryFillAtomicallyFromPressure(List<GasStack> resources, long sourcePressurePa, GasAction action) {
            return resolveHandler().tryFillAtomicallyFromPressure(resources, sourcePressurePa, action);
        }

        private GasStorageHandler resolveHandler() {
            if (owner.isRemoved()) {
                return EMPTY_HANDLER;
            }

            if (owner.isController()) {
                if (tankInventory == null) {
                    return EMPTY_HANDLER;
                }

                return tankInventory;
            }

            AbstractAirtightTankBlockEntity controllerTank = owner.getControllerBE();
            if (controllerTank == null || controllerTank.isRemoved() || !controllerTank.isController()) {
                return EMPTY_HANDLER;
            }

            return controllerTank.getTankInventory();
        }
    }
}
