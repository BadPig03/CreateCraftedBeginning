package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
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
public class CrateItemStackHandler implements IItemHandler, IItemHandlerModifiable, INBTSerializable<CompoundTag> {
    private static final String COMPOUND_KEY_CONTENT = "Content";
    private static final String COMPOUND_KEY_COUNT = "Count";

    private final IntSupplier maxCountSupplier;
    private final Predicate<ItemStack> itemValidator;
    private final Runnable contentsChangedListener;
    protected ItemStack content = ItemStack.EMPTY;
    protected int count;
    private int batchDepth;
    private boolean batchChanged;

    public CrateItemStackHandler(int maxCount) {
        this(() -> maxCount);
    }

    public CrateItemStackHandler(IntSupplier maxCountSupplier) {
        this(maxCountSupplier, stack -> true, () -> {});
    }

    public CrateItemStackHandler(IntSupplier maxCountSupplier, Predicate<ItemStack> itemValidator, Runnable contentsChangedListener) {
        this.maxCountSupplier = Objects.requireNonNull(maxCountSupplier);
        this.itemValidator = Objects.requireNonNull(itemValidator);
        this.contentsChangedListener = Objects.requireNonNull(contentsChangedListener);
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        ensureValidState();
        CompoundTag tag = new CompoundTag();
        if (content.isEmpty()) {
            return tag;
        }

        tag.put(COMPOUND_KEY_CONTENT, content.saveOptional(provider));
        tag.putInt(COMPOUND_KEY_COUNT, count);
        return tag;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag compoundTag) {
        ItemStack storedContent = ItemStack.EMPTY;
        int storedCount = 0;
        if (compoundTag.contains(COMPOUND_KEY_CONTENT)) {
            storedContent = ItemStack.parseOptional(provider, compoundTag.getCompound(COMPOUND_KEY_CONTENT));
        }
        if (compoundTag.contains(COMPOUND_KEY_COUNT)) {
            storedCount = compoundTag.getInt(COMPOUND_KEY_COUNT);
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
        ensureValidState();
        return content.isEmpty() ? ItemStack.EMPTY : content.copyWithCount(count);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        ensureValidState();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!isItemValid(slot, stack)) {
            return stack;
        }

        int maxCount = getMaxCount();
        if (content.isEmpty()) {
            int toInsert = Math.min(stack.getCount(), maxCount);
            if (!simulate) {
                applyStoredItems(stack, toInsert, true);
            }
            int remaining = stack.getCount() - toInsert;
            return remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY;
        }

        int space = maxCount - count;
        if (space <= 0) {
            return stack;
        }

        int toInsert = Math.min(stack.getCount(), space);
        if (!simulate) {
            applyStoredItems(content, count + toInsert, true);
        }

        int remaining = stack.getCount() - toInsert;
        return remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        ensureValidState();
        if (amount <= 0 || content.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(Math.min(amount, count), content.getMaxStackSize());
        ItemStack result = content.copyWithCount(toExtract);

        if (simulate) {
            return result;
        }

        applyStoredItems(content, count - toExtract, true);
        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        ensureValidState();
        return getMaxCount();
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        ensureValidState();
        return passesItemValidator(stack) && (content.isEmpty() || ItemStack.isSameItemSameComponents(content, stack));
    }

    public ItemStack getStoredItem(int slot) {
        validateSlotIndex(slot);
        ensureValidState();
        return content.isEmpty() ? ItemStack.EMPTY : content.copy();
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        setStoredItems(slot, stack, stack.getCount());
    }

    public int getCountInSlot(int slot) {
        validateSlotIndex(slot);
        ensureValidState();
        return count;
    }

    public void setStoredItems(int slot, ItemStack stack, int newCount) {
        validateSlotIndex(slot);
        applyStoredItems(stack, newCount, true);
    }

    final void initializeStoredItems(ItemStack stack, int newCount) {
        applyStoredItems(stack, newCount, false);
    }

    protected final boolean passesItemValidator(ItemStack stack) {
        return !stack.isEmpty() && itemValidator.test(stack);
    }

    protected final int getMaxCount() {
        return Math.max(0, maxCountSupplier.getAsInt());
    }

    protected void validateSlotIndex(int slot) {
        if (slot == 0) {
            return;
        }

        throw new RuntimeException("Slot " + slot + " not in valid range - [0,1)");
    }

    protected void onLoad() {
    }

    protected void onContentsChanged() {
        contentsChangedListener.run();
    }

    public final <T> T runInBatch(Supplier<T> action) {
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

    protected final void ensureValidState() {
        applyStoredItems(content, count, true);
    }

    private void applyStoredItems(ItemStack stack, int newCount, boolean notify) {
        CrateInventoryState normalized = CrateInventoryState.normalize(stack, newCount, getMaxCount());
        boolean changed = count != normalized.count() || !ItemStack.matches(content, normalized.content());
        content = normalized.content();
        count = normalized.count();
        if (!notify || !changed) {
            return;
        }

        if (batchDepth > 0) {
            batchChanged = true;
            return;
        }

        onContentsChanged();
    }
}
