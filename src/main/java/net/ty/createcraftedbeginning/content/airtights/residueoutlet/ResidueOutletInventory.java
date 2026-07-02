package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.foundation.blockEntity.ItemHandlerContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueOutletInventory extends ItemHandlerContainer implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    private static final int MAX_SIZE = 1;
    private static final String COMPOUND_KEY_PARTIAL_ITEM_COUNT = "PartialItemCount";

    private float partialItemCount;

    public ResidueOutletInventory(ResidueOutletBlockEntity outlet) {
        super(new InternalStackHandler(outlet));
    }

    @Override
    public int getSlots() {
        return MAX_SIZE;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inv.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return inv.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return inv.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return inv.isItemValid(slot, stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        inv.setStackInSlot(slot, stack);
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag compoundTag = ((InternalStackHandler) inv).serializeNBT(provider);
        compoundTag.putFloat(COMPOUND_KEY_PARTIAL_ITEM_COUNT, partialItemCount);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag compoundTag) {
        ((InternalStackHandler) inv).deserializeNBT(provider, compoundTag);
        if (!compoundTag.contains(COMPOUND_KEY_PARTIAL_ITEM_COUNT)) {
            return;
        }

        partialItemCount = compoundTag.getFloat(COMPOUND_KEY_PARTIAL_ITEM_COUNT);
    }

    public IItemHandler getCapability() {
        return inv;
    }

    public boolean canAcceptItem(ItemStack itemStack) {
        return itemStack.isEmpty() || inv.insertItem(0, itemStack.copyWithCount(1), true).isEmpty();
    }

    public boolean canAddPartialItemCount(float count, ItemStack itemStack) {
        if (count <= 0 || itemStack.isEmpty()) {
            return true;
        }

        float newPartialItemCount = partialItemCount + count;
        int intPart = (int) newPartialItemCount;
        return intPart <= 0 || inv.insertItem(0, itemStack.copyWithCount(intPart), true).isEmpty();
    }

    public boolean addPartialItemCount(float count, ItemStack itemStack) {
        if (count <= 0 || itemStack.isEmpty()) {
            return true;
        }

        float newPartialItemCount = partialItemCount + count;
        int intPart = (int) newPartialItemCount;
        if (intPart > 0) {
            if (!canAddPartialItemCount(count, itemStack)) {
                return false;
            }

            ItemStack leftover = inv.insertItem(0, itemStack.copyWithCount(intPart), false);
            if (!leftover.isEmpty()) {
                return false;
            }

            newPartialItemCount -= intPart;
        }

        partialItemCount = newPartialItemCount;
        return true;
    }

    public static class InternalStackHandler extends ItemStackHandler {
        private final ResidueOutletBlockEntity outlet;

        public InternalStackHandler(ResidueOutletBlockEntity outlet) {
            super(MAX_SIZE);
            this.outlet = outlet;
        }

        @Override
        protected void onContentsChanged(int slot) {
            super.onContentsChanged(slot);
            outlet.notifyUpdate();
        }
    }
}
