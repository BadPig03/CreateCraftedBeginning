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

    public void resizeToBlocks(int blockCount) {
        setCapacityForBlocks(blockCount);
        drainOverflow();
    }

    private void setCapacityForBlocks(int blockCount) {
        owner.getTankInventory().setCapacity((long) blockCount * AirtightTankBlockEntity.getCapacityPerTank());
    }

    public void setCapacityForStructure() {
        setCapacityForBlocks(owner.getTotalTankSize());
    }

    public void drainOverflow() {
        long overflowAmount = owner.getTankInventory().getGasAmount() - owner.getTankInventory().getCapacity();
        if (overflowAmount <= 0) {
            return;
        }

        owner.getTankInventory().drain(overflowAmount, GasAction.EXECUTE);
    }
}
