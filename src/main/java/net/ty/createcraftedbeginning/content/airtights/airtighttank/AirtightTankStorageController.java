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

    void resizeToBlocks(int blockCount) {
        setCapacityForBlocks(blockCount);
        drainOverflow();
    }

    void setCapacityForStructure() {
        setCapacityForBlocks(owner.getTotalTankSize());
    }

    void drainOverflow() {
        long overflowAmount = owner.getTankInventory().getGasAmount() - owner.getTankInventory().getCapacity();
        if (overflowAmount <= 0) {
            return;
        }

        owner.getTankInventory().drain(overflowAmount, GasAction.EXECUTE);
    }

    private void setCapacityForBlocks(int blockCount) {
        owner.getTankInventory().setCapacity((long) blockCount * AirtightTankBlockEntity.getCapacityPerTank());
    }
}
