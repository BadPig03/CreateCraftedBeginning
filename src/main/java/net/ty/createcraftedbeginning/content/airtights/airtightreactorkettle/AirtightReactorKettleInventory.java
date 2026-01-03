package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class AirtightReactorKettleInventory extends SmartInventory {
    private final AirtightReactorKettleBlockEntity blockEntity;

    public AirtightReactorKettleInventory(int slots, AirtightReactorKettleBlockEntity blockEntity) {
        super(slots, blockEntity, 64, true);
        this.blockEntity = blockEntity;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        int firstFreeSlot = -1;
        for (int i = 0; i < getSlots(); i++) {
            if (i != slot && ItemStack.isSameItemSameComponents(stack, inv.getStackInSlot(i))) {
                return stack;
            }
            if (!inv.getStackInSlot(i).isEmpty() || firstFreeSlot != -1) {
                continue;
            }

            firstFreeSlot = i;
        }
        if (inv.getStackInSlot(slot).isEmpty() && firstFreeSlot != slot) {
            return stack;
        }

        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack extractItem = super.extractItem(slot, amount, simulate);
        if (!simulate && !extractItem.isEmpty()) {
            blockEntity.notifyContentsChanged();
        }
        return extractItem;
    }
}
