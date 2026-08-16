package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightTankStorageController {
    private final AirtightTankBlockEntity owner;

    public AirtightTankStorageController(AirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    public void resizeToBlocks(int blocks) {
        setCapacityForBlocks(blocks);
        drainOverflow();
    }

    private void setCapacityForBlocks(int blocks) {
        owner.getTankInventory().setCapacity((long) blocks * AirtightTankBlockEntity.getCapacityPerTank());
    }

    public void setCapacityForStructure() {
        setCapacityForBlocks(owner.getTotalTankSize());
    }

    public void drainOverflow() {
        long overflow = owner.getTankInventory().getGasAmount() - owner.getTankInventory().getCapacity();
        if (overflow <= 0) {
            return;
        }

        owner.getTankInventory().drain(overflow, GasAction.EXECUTE);
    }
}
