package net.ty.createcraftedbeginning.content.airtights.airtighttank;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.gases.GasAction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AirtightTankStorageController {
    private final AirtightTankBlockEntity owner;

    AirtightTankStorageController(AirtightTankBlockEntity owner) {
        this.owner = owner;
    }

    void resizeToBlocks(int blocks) {
        setCapacityForBlocks(blocks);
        drainOverflow();
    }

    void setCapacityForBlocks(int blocks) {
        owner.getTankInventory().setCapacity((long) blocks * AirtightTankBlockEntity.getCapacityPerTank());
    }

    void setCapacityForStructure() {
        setCapacityForBlocks(owner.getTotalTankSize());
    }

    void drainOverflow() {
        long overflow = owner.getTankInventory().getGasAmount() - owner.getTankInventory().getCapacity();
        if (overflow > 0) {
            owner.getTankInventory().drain(overflow, GasAction.EXECUTE);
        }
    }
}
