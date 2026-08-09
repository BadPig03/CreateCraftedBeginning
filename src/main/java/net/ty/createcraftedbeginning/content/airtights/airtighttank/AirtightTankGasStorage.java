package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankGasStorage {
    private final AbstractAirtightTankBlockEntity owner;
    private GasTank tankInventory;
    private IGasHandler gasCapability;

    AirtightTankGasStorage(AbstractAirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    void initialize(GasTank tankInventory) {
        this.tankInventory = tankInventory;
        refreshCapability();
    }

    GasTank getTankInventory() {
        return tankInventory;
    }

    IGasHandler getCapability() {
        if (gasCapability == null) {
            refreshCapability();
        }
        return gasCapability;
    }

    void refreshCapability() {
        gasCapability = handlerForCapability();
        owner.invalidateGasCapabilities();
    }

    void invalidate() {
        owner.invalidateGasCapabilities();
    }

    void onGasStackChanged(GasStack ignored) {
        if (!owner.isController() || owner.getLevel() == null || owner.getLevel().isClientSide) {
            return;
        }
        owner.notifyUpdate();
    }

    private IGasHandler handlerForCapability() {
        if (owner.isController()) {
            return tankInventory;
        }

        AbstractAirtightTankBlockEntity controller = owner.getControllerBE();
        return controller != null ? controller.getCapability() : new GasTank(0);
    }
}
