package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
record AirtightReactorKettlePortHandler(IItemHandlerModifiable input, IItemHandlerModifiable output) implements IItemHandler {
    @Override
    public int getSlots() {
        return input.getSlots() + output.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return getHandler(slot).getStackInSlot(getLocalSlot(slot));
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        getHandler(slot);
        if (slot >= input.getSlots()) {
            return stack;
        }
        return input.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        getHandler(slot);
        if (slot < input.getSlots()) {
            return ItemStack.EMPTY;
        }
        return output.extractItem(slot - input.getSlots(), amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return getHandler(slot).getSlotLimit(getLocalSlot(slot));
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        getHandler(slot);
        return slot < input.getSlots() && input.isItemValid(slot, stack);
    }

    private IItemHandlerModifiable getHandler(int slot) {
        if (slot < 0 || slot >= getSlots()) {
            throw new IndexOutOfBoundsException("Slot " + slot + " not in valid range [0," + getSlots() + ')');
        }
        return slot < input.getSlots() ? input : output;
    }

    private int getLocalSlot(int slot) {
        return slot < input.getSlots() ? slot : slot - input.getSlots();
    }
}
