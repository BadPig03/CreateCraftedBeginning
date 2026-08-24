package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.simibubi.create.foundation.item.SmartInventory;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightReactorKettleInventory extends SmartInventory {
    private final AirtightReactorKettleBlockEntity blockEntity;

    public AirtightReactorKettleInventory(int slots, AirtightReactorKettleBlockEntity blockEntity) {
        super(slots, blockEntity, 64, true);
        this.blockEntity = blockEntity;
    }

    public static IItemHandlerModifiable createSimulation(int slots) {
        return new ItemStackHandler(slots) {
            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (!isInsertionAllowed(this, slot, stack)) {
                    return stack;
                }
                return super.insertItem(slot, stack, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return 64;
            }
        };
    }

    private static boolean isInsertionAllowed(IItemHandler inventory, int slot, ItemStack stack) {
        int firstFreeSlot = -1;
        for (int candidateSlot = 0; candidateSlot < inventory.getSlots(); candidateSlot++) {
            ItemStack storedStack = inventory.getStackInSlot(candidateSlot);
            if (candidateSlot != slot && ItemStack.isSameItemSameComponents(stack, storedStack)) {
                return false;
            }

            if (!storedStack.isEmpty() || firstFreeSlot != -1) {
                continue;
            }

            firstFreeSlot = candidateSlot;
        }
        return !inventory.getStackInSlot(slot).isEmpty() || firstFreeSlot == slot;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!isInsertionAllowed(this, slot, stack)) {
            return stack;
        }
        return super.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack extractedStack = super.extractItem(slot, amount, simulate);
        if (simulate || extractedStack.isEmpty()) {
            return extractedStack;
        }

        blockEntity.notifyContentsChanged();
        return extractedStack;
    }
}
