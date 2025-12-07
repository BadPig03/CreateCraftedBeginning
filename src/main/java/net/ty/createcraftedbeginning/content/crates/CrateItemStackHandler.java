package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.content.logistics.filter.FilterItem;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.NotNull;

public class CrateItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    private static final String COMPOUND_KEY_CONTENT = "Content";
    private static final String COMPOUND_KEY_COUNT = "Count";

    protected final int maxCount;

    protected FilteringBehaviour filtering;
    protected ItemStack content = ItemStack.EMPTY;
    protected int count;

    public CrateItemStackHandler(int maxCount, FilteringBehaviour filtering) {
        this.maxCount = maxCount;
        this.filtering = filtering;
    }

    @Override
    public CompoundTag serializeNBT(@NotNull Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        compoundTag.put(COMPOUND_KEY_CONTENT, content.saveOptional(provider));
        compoundTag.putInt(COMPOUND_KEY_COUNT, count);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(@NotNull Provider provider, @NotNull CompoundTag compoundTag) {
        if (compoundTag.contains(COMPOUND_KEY_CONTENT)) {
            content = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_CONTENT));
        }
        if (compoundTag.contains(COMPOUND_KEY_COUNT)) {
            count = compoundTag.getInt(COMPOUND_KEY_COUNT);
        }

        count = Math.min(count, maxCount);
        if (content.isEmpty() && count > 0) {
            count = 0;
        }
        onLoad();
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        return content.isEmpty() || count == 0 ? ItemStack.EMPTY : content.copyWithCount(count);
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        validateSlotIndex(slot);

        content = stack;
        count = stack.isEmpty() ? 0 : 1;
        onContentsChanged(slot);
    }

    public int getCountInSlot(int slot) {
        validateSlotIndex(slot);
        return content.isEmpty() ? 0 : count;
    }

    public void setCountInSlot(int slot, int newCount) {
        validateSlotIndex(slot);
        if (content.isEmpty()) {
            return;
        }

        count = newCount;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        validateSlotIndex(slot);
        if (!isItemValid(slot, stack)) {
            return stack;
        }

        if (content.isEmpty()) {
            int newCount = Math.min(stack.getCount(), maxCount);
            if (!simulate) {
                content = stack.copyWithCount(1);
                count = newCount;
                onContentsChanged(slot);
            }
            int remaining = stack.getCount() - newCount;
            return remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY;
        }

        if (!ItemStack.isSameItemSameComponents(content, stack)) {
            return stack;
        }

        int space = maxCount - count;
        if (space <= 0) {
            return stack;
        }

        int toInsert = Math.min(stack.getCount(), space);
        if (!simulate) {
            count += toInsert;
            onContentsChanged(slot);
        }

        int remaining = stack.getCount() - toInsert;
        return remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        validateSlotIndex(slot);
        if (content.isEmpty() || count == 0) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(Math.min(amount, count), content.getMaxStackSize());
        ItemStack result = content.copyWithCount(toExtract);

        if (!simulate) {
            count -= toExtract;
            if (count <= 0) {
                content = ItemStack.EMPTY;
                count = 0;
            }
            onContentsChanged(slot);
        }

        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        return maxCount;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        validateSlotIndex(slot);
        return (filtering == null || filtering.getFilter().isEmpty() || FilterItem.testDirect(filtering.getFilter(), stack, false)) && (content.isEmpty() || ItemStack.isSameItemSameComponents(content, stack));
    }

    protected void validateSlotIndex(int slot) {
        if (slot == 0) {
            return;
        }

        throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
    }

    protected void onLoad() {
    }

    protected void onContentsChanged(int slot) {
    }
}
