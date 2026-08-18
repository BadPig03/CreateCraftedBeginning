package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class AirtightForgingPressInventory extends SmartInventory {
    private final AirtightForgingPressBlockEntity blockEntity;

    AirtightForgingPressInventory(int slots, AirtightForgingPressBlockEntity blockEntity) {
        super(slots, blockEntity);
        this.blockEntity = blockEntity;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack extracted = super.extractItem(slot, amount, simulate);
        if (simulate || extracted.isEmpty()) {
            return extracted;
        }

        blockEntity.notifyContentsChanged();
        return extracted;
    }
}
