package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.handlers.GasTank;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTankGasStorage {
    private final AbstractAirtightTankBlockEntity owner;
    private GasTank tankInventory;
    private IGasHandler gasCapability;

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
        if (gasCapability == null) {
            refreshCapability();
        }
        return gasCapability;
    }

    public void refreshCapability() {
        gasCapability = handlerForCapability();
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

    private IGasHandler handlerForCapability() {
        if (owner.isController()) {
            return tankInventory;
        }

        AbstractAirtightTankBlockEntity controller = owner.getControllerBE();
        return controller != null ? controller.getCapability() : new GasTank(0);
    }
}
