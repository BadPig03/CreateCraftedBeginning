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
public class ResidueOutletInventory extends ItemHandlerContainer implements IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    public static final int ITEM_PROGRESS_UNITS_PER_ITEM = ResidueOutletInsertionTarget.ITEM_PROGRESS_UNITS_PER_ITEM;

    private static final int MAX_SIZE = 1;
    private static final String COMPOUND_KEY_PARTIAL_ITEM_UNITS = "PartialItemUnits";
    private static final String COMPOUND_KEY_PARTIAL_ITEM = "PartialItem";

    private final ResidueOutletBlockEntity outlet;
    private final IItemHandler extractionCapability;
    private ItemStack partialItem = ItemStack.EMPTY;
    private int partialItemUnits;

    public ResidueOutletInventory(ResidueOutletBlockEntity outlet) {
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
        CompoundTag tag = ((InternalStackHandler) inv).serializeNBT(provider);
        tag.putInt(COMPOUND_KEY_PARTIAL_ITEM_UNITS, partialItemUnits);
        if (partialItem.isEmpty()) {
            return tag;
        }

        tag.put(COMPOUND_KEY_PARTIAL_ITEM, partialItem.saveOptional(provider));
        return tag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag compoundTag) {
        ((InternalStackHandler) inv).deserializeNBT(provider, compoundTag);
        partialItem = ItemStack.EMPTY;
        partialItemUnits = Mth.clamp(compoundTag.getInt(COMPOUND_KEY_PARTIAL_ITEM_UNITS), 0, ITEM_PROGRESS_UNITS_PER_ITEM - 1);
        if (partialItemUnits <= 0) {
            return;
        }

        ItemStack storedItem = ItemStack.EMPTY;
        if (compoundTag.contains(COMPOUND_KEY_PARTIAL_ITEM)) {
            storedItem = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_PARTIAL_ITEM));
        }
        if (storedItem.isEmpty()) {
            partialItemUnits = 0;
            return;
        }

        partialItem = storedItem.copyWithCount(1);
    }

    public IItemHandler getExtractionCapability() {
        return extractionCapability;
    }

    public int getItemInsertionCapacityUnits(ItemStack itemStack) {
        if (itemStack.isEmpty() || hasMismatchedPartialItem(itemStack)) {
            return 0;
        }

        ItemStack storedItem = getStackInSlot(0);
        if (!storedItem.isEmpty() && !ItemStack.isSameItemSameComponents(storedItem, itemStack)) {
            return 0;
        }

        int limit = Math.min(getSlotLimit(0), itemStack.getMaxStackSize());
        int storedCount = storedItem.isEmpty() ? 0 : storedItem.getCount();
        int wholeItems = Math.max(0, limit - storedCount);
        int capacity = wholeItems * ITEM_PROGRESS_UNITS_PER_ITEM + ITEM_PROGRESS_UNITS_PER_ITEM - 1 - partialItemUnits;
        return Math.clamp(capacity, 0, Integer.MAX_VALUE);
    }

    public int addPartialItemUnits(int units, ItemStack itemStack) {
        if (units <= 0 || itemStack.isEmpty()) {
            return 0;
        }

        int acceptedUnits = Math.min(units, getItemInsertionCapacityUnits(itemStack));
        if (acceptedUnits <= 0) {
            return 0;
        }

        int previousUnits = hasMatchingPartialItem(itemStack) ? partialItemUnits : 0;
        int totalUnits = previousUnits + acceptedUnits;
        int requestedItems = totalUnits / ITEM_PROGRESS_UNITS_PER_ITEM;
        int insertedItems = insertWholeItems(itemStack, requestedItems);

        int maximumAccepted = insertedItems * ITEM_PROGRESS_UNITS_PER_ITEM + ITEM_PROGRESS_UNITS_PER_ITEM - 1 - previousUnits;
        int actualAccepted = Math.clamp(maximumAccepted, 0, acceptedUnits);
        int remainingUnits = previousUnits + actualAccepted - insertedItems * ITEM_PROGRESS_UNITS_PER_ITEM;
        setPartialItemProgress(itemStack, remainingUnits);
        return actualAccepted;
    }

    private int insertWholeItems(ItemStack item, int amount) {
        if (amount <= 0) {
            return 0;
        }

        ItemStack remainder = inv.insertItem(0, item.copyWithCount(amount), false);
        return amount - remainder.getCount();
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

    private void setPartialItemProgress(ItemStack itemStack, int units) {
        int newUnits = Mth.clamp(units, 0, ITEM_PROGRESS_UNITS_PER_ITEM - 1);
        ItemStack newItem = newUnits > 0 ? itemStack.copyWithCount(1) : ItemStack.EMPTY;
        boolean changed = partialItemUnits != newUnits || !ItemStack.isSameItemSameComponents(partialItem, newItem);
        partialItemUnits = newUnits;
        partialItem = newItem;
        if (!changed) {
            return;
        }

        outlet.setChanged();
    }

    private static class InternalStackHandler extends ItemStackHandler {
        protected final ResidueOutletBlockEntity outlet;

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
