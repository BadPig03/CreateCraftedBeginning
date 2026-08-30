package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.foundation.CCBNbtUtils;
import net.neoforged.neoforge.common.util.INBTSerializable;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
class CrateItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    private static final String COMPOUND_KEY_CONTENT = "Content";
    private static final String COMPOUND_KEY_COUNT = "Count";

    private final IntSupplier maxCountSupplier;
    private final Predicate<ItemStack> itemValidator;
    private final Runnable contentsChangedListener;

    ItemStack content = ItemStack.EMPTY;
    int count;
    private int batchDepth;
    private boolean batchChanged;

    CrateItemStackHandler(IntSupplier maxCountSupplier, Predicate<ItemStack> itemValidator, Runnable contentsChangedListener) {
        this.maxCountSupplier = Objects.requireNonNull(maxCountSupplier);
        this.itemValidator = Objects.requireNonNull(itemValidator);
        this.contentsChangedListener = Objects.requireNonNull(contentsChangedListener);
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        CompoundTag compoundTag = new CompoundTag();
        if (content.isEmpty()) {
            return compoundTag;
        }

        CCBNbtUtils.putTag(compoundTag, COMPOUND_KEY_CONTENT, content.saveOptional(provider));
        CCBNbtUtils.putInt(compoundTag, COMPOUND_KEY_COUNT, count);
        return compoundTag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag compoundTag) {
        ItemStack storedContent = ItemStack.EMPTY;
        int storedCount = CCBNbtUtils.getIntOrDefault(compoundTag, COMPOUND_KEY_COUNT, 0);
        if (CCBNbtUtils.contains(compoundTag, COMPOUND_KEY_CONTENT)) {
            storedContent = ItemStack.parseOptional(provider, CCBNbtUtils.getCompound(compoundTag, COMPOUND_KEY_CONTENT));
        }
        applyStoredItems(storedContent, storedCount, false);
        onLoad();
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        if (content.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return content.copyWithCount(count);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!isCompatibleItem(stack)) {
            return stack;
        }

        int capacity = getConfiguredCapacity();
        if (content.isEmpty()) {
            int insertCount = Math.min(stack.getCount(), capacity);
            if (!simulate) {
                applyStoredItems(stack, insertCount, true);
            }
            int remainingCount = stack.getCount() - insertCount;
            if (remainingCount <= 0) {
                return ItemStack.EMPTY;
            }
            return stack.copyWithCount(remainingCount);
        }

        int remainingCapacity = getRemainingCapacity();
        if (remainingCapacity <= 0) {
            return stack;
        }

        int insertCount = Math.min(stack.getCount(), remainingCapacity);
        if (!simulate) {
            applyStoredItems(content, count + insertCount, true);
        }

        int remainingCount = stack.getCount() - insertCount;
        if (remainingCount <= 0) {
            return ItemStack.EMPTY;
        }
        return stack.copyWithCount(remainingCount);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (amount <= 0 || content.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int extractCount = Math.min(Math.min(amount, count), content.getMaxStackSize());
        ItemStack extractedStack = content.copyWithCount(extractCount);
        if (simulate) {
            return extractedStack;
        }

        applyStoredItems(content, count - extractCount, true);
        return extractedStack;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        return Math.max(getConfiguredCapacity(), count);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        return isCompatibleItem(stack);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        setStoredItems(slot, stack, Math.min(stack.getCount(), getConfiguredCapacity()));
    }

    ItemStack getStoredItem(int slot) {
        validateSlotIndex(slot);
        if (content.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return content.copy();
    }

    int getCountInSlot(int slot) {
        validateSlotIndex(slot);
        return count;
    }

    void setStoredItems(int slot, ItemStack stack, int newCount) {
        validateSlotIndex(slot);
        applyStoredItems(stack, newCount, true);
    }

    void validateSlotIndex(int slot) {
        if (slot == 0) {
            return;
        }

        throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
    }

    final void initializeStoredItems(ItemStack stack, int newCount) {
        applyStoredItems(stack, newCount, false);
    }

    final boolean passesItemValidator(ItemStack stack) {
        return !stack.isEmpty() && itemValidator.test(stack);
    }

    final int getConfiguredCapacity() {
        return Math.max(0, maxCountSupplier.getAsInt());
    }

    final int getRemainingCapacity() {
        return Math.max(0, getConfiguredCapacity() - count);
    }

    final <T> T runInBatch(Supplier<T> action) {
        Objects.requireNonNull(action);
        batchDepth++;
        try {
            return action.get();
        } finally {
            batchDepth--;
            if (batchDepth == 0 && batchChanged) {
                batchChanged = false;
                onContentsChanged();
            }
        }
    }

    private void onLoad() {
    }

    private boolean isCompatibleItem(ItemStack stack) {
        return passesItemValidator(stack) && (content.isEmpty() || ItemStack.isSameItemSameComponents(content, stack));
    }

    private void onContentsChanged() {
        contentsChangedListener.run();
    }

    private void applyStoredItems(ItemStack stack, int newCount, boolean notify) {
        ItemStack normalizedContent;
        int normalizedCount;
        if (stack.isEmpty() || newCount <= 0) {
            normalizedContent = ItemStack.EMPTY;
            normalizedCount = 0;
        }
        else {
            normalizedContent = stack == content ? content : stack.copyWithCount(1);
            normalizedCount = newCount;
        }

        boolean contentsChanged = count != normalizedCount || !ItemStack.matches(content, normalizedContent);
        content = normalizedContent;
        count = normalizedCount;
        if (!notify || !contentsChanged) {
            return;
        }

        if (batchDepth > 0) {
            batchChanged = true;
            return;
        }

        onContentsChanged();
    }
}
