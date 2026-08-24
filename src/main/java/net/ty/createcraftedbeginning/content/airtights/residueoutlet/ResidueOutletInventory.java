package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.foundation.blockEntity.ItemHandlerContainer;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class ResidueOutletInventory extends ItemHandlerContainer implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    private static final int ITEM_PROGRESS_UNITS_PER_ITEM = ResidueOutletInsertionTarget.ITEM_PROGRESS_UNITS_PER_ITEM;

    private static final int MAX_SIZE = 1;
    private static final String COMPOUND_KEY_PARTIAL_ITEM_UNITS = "PartialItemUnits";
    private static final String COMPOUND_KEY_PARTIAL_ITEM = "PartialItem";

    private final ResidueOutletBlockEntity outlet;
    private final IItemHandler extractionCapability;
    private ItemStack partialItem = ItemStack.EMPTY;
    private int partialItemUnits;

    ResidueOutletInventory(ResidueOutletBlockEntity outlet) {
        super(new InternalStackHandler(outlet));
        this.outlet = outlet;
        extractionCapability = new IItemHandler() {
            @Override
            public int getSlots() {
                return ResidueOutletInventory.this.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return ResidueOutletInventory.this.getStackInSlot(slot).copy();
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ResidueOutletInventory.this.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return ResidueOutletInventory.this.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
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
        CompoundTag inventoryTag = ((InternalStackHandler) inv).serializeNBT(provider);
        inventoryTag.putInt(COMPOUND_KEY_PARTIAL_ITEM_UNITS, partialItemUnits);
        if (partialItem.isEmpty()) {
            return inventoryTag;
        }

        inventoryTag.put(COMPOUND_KEY_PARTIAL_ITEM, partialItem.saveOptional(provider));
        return inventoryTag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag compoundTag) {
        ((InternalStackHandler) inv).deserializeNBT(provider, compoundTag);
        partialItem = ItemStack.EMPTY;
        partialItemUnits = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_PARTIAL_ITEM_UNITS), 0, ITEM_PROGRESS_UNITS_PER_ITEM - 1);
        if (partialItemUnits <= 0) {
            return;
        }

        ItemStack storedPartialItem = compoundTag.contains(COMPOUND_KEY_PARTIAL_ITEM) ? ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_PARTIAL_ITEM)) : ItemStack.EMPTY;
        if (storedPartialItem.isEmpty()) {
            partialItemUnits = 0;
            return;
        }

        partialItem = storedPartialItem.copyWithCount(1);
    }

    IItemHandler getExtractionCapability() {
        return extractionCapability;
    }

    int getItemInsertionCapacityUnits(ItemStack itemStack) {
        if (itemStack.isEmpty() || hasMismatchedPartialItem(itemStack)) {
            return 0;
        }

        ItemStack storedItem = getStackInSlot(0);
        if (!storedItem.isEmpty() && !ItemStack.isSameItemSameComponents(storedItem, itemStack)) {
            return 0;
        }

        int slotLimit = Math.min(getSlotLimit(0), itemStack.getMaxStackSize());
        int storedItemCount = storedItem.isEmpty() ? 0 : storedItem.getCount();
        int availableWholeItems = Math.max(0, slotLimit - storedItemCount);
        int capacityUnits = availableWholeItems * ITEM_PROGRESS_UNITS_PER_ITEM + ITEM_PROGRESS_UNITS_PER_ITEM - 1 - partialItemUnits;
        return Math.clamp(capacityUnits, 0, Integer.MAX_VALUE);
    }

    int addPartialItemUnits(int requestedUnits, ItemStack itemStack) {
        if (requestedUnits <= 0 || itemStack.isEmpty()) {
            return 0;
        }

        int insertableUnits = Math.min(requestedUnits, getItemInsertionCapacityUnits(itemStack));
        if (insertableUnits <= 0) {
            return 0;
        }

        int previousPartialUnits = hasMatchingPartialItem(itemStack) ? partialItemUnits : 0;
        int totalUnits = previousPartialUnits + insertableUnits;
        int wholeItemsToInsert = totalUnits / ITEM_PROGRESS_UNITS_PER_ITEM;
        int insertedItems = insertWholeItems(itemStack, wholeItemsToInsert);

        int maxAcceptedUnits = insertedItems * ITEM_PROGRESS_UNITS_PER_ITEM + ITEM_PROGRESS_UNITS_PER_ITEM - 1 - previousPartialUnits;
        int acceptedUnits = Math.clamp(maxAcceptedUnits, 0, insertableUnits);
        int remainingPartialUnits = previousPartialUnits + acceptedUnits - insertedItems * ITEM_PROGRESS_UNITS_PER_ITEM;
        setPartialItemProgress(itemStack, remainingPartialUnits);
        return acceptedUnits;
    }

    private int insertWholeItems(ItemStack itemStack, int itemCount) {
        if (itemCount <= 0) {
            return 0;
        }

        ItemStack remainingStack = inv.insertItem(0, itemStack.copyWithCount(itemCount), false);
        return itemCount - remainingStack.getCount();
    }

    private boolean hasMismatchedPartialItem(ItemStack itemStack) {
        return partialItemUnits > 0 && !matchesPartialItem(itemStack);
    }

    private boolean hasMatchingPartialItem(ItemStack itemStack) {
        return partialItemUnits > 0 && matchesPartialItem(itemStack);
    }

    private boolean matchesPartialItem(ItemStack itemStack) {
        return !partialItem.isEmpty() && ItemStack.isSameItemSameComponents(partialItem, itemStack);
    }

    private void setPartialItemProgress(ItemStack itemStack, int partialUnits) {
        int clampedUnits = Mth.clamp(partialUnits, 0, ITEM_PROGRESS_UNITS_PER_ITEM - 1);
        ItemStack newPartialItem = clampedUnits > 0 ? itemStack.copyWithCount(1) : ItemStack.EMPTY;
        boolean partialProgressChanged = partialItemUnits != clampedUnits || !ItemStack.isSameItemSameComponents(partialItem, newPartialItem);
        partialItemUnits = clampedUnits;
        partialItem = newPartialItem;
        if (!partialProgressChanged) {
            return;
        }

        outlet.setChanged();
    }

    private static class InternalStackHandler extends ItemStackHandler {
        private final ResidueOutletBlockEntity outlet;

        private InternalStackHandler(ResidueOutletBlockEntity outlet) {
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
