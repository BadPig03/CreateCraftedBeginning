package net.ty.createcraftedbeginning.content.crates;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DiscardingCrateItemStackHandler extends CrateItemStackHandler {
    private static final int STORAGE_SLOT = 0;
    private static final int DISCARD_SLOT = 1;
    private static final int SLOT_COUNT = 2;
    private static final int VIRTUAL_DISCARD_SLOT_LIMIT = Integer.MAX_VALUE;

    private final Predicate<ItemStack> trackedItemPredicate;
    private final Runnable trackedDiscardListener;

    public DiscardingCrateItemStackHandler(IntSupplier maxCountSupplier, Predicate<ItemStack> itemValidator, Runnable contentsChangedListener, Predicate<ItemStack> trackedDiscardPredicate, Runnable trackedDiscardListener) {
        super(maxCountSupplier, itemValidator, contentsChangedListener);
        trackedItemPredicate = Objects.requireNonNull(trackedDiscardPredicate);
        this.trackedDiscardListener = Objects.requireNonNull(trackedDiscardListener);
    }

    @Override
    public int getSlots() {
        return SLOT_COUNT;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        validateSlotIndex(slot);
        if (slot == DISCARD_SLOT) {
            return ItemStack.EMPTY;
        }
        return super.getStackInSlot(STORAGE_SLOT);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        validateSlotIndex(slot);
        ensureValidState();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!passesItemValidator(stack)) {
            return stack;
        }

        int maxCount = getMaxCount();
        int incoming = stack.getCount();
        ItemStack nextContent = content;
        int nextCount;
        boolean hasTrackedDiscard;
        if (content.isEmpty()) {
            nextContent = stack;
            nextCount = Math.min(incoming, maxCount);
            hasTrackedDiscard = trackedItemPredicate.test(stack) && nextCount < incoming;
        }
        else if (ItemStack.isSameItemSameComponents(content, stack)) {
            int accepted = Math.clamp(maxCount - count, 0, incoming);
            nextCount = count + accepted;
            hasTrackedDiscard = trackedItemPredicate.test(stack) && accepted < incoming;
        }
        else {
            nextContent = stack;
            nextCount = Math.min(incoming, maxCount);
            hasTrackedDiscard = count > 0 && trackedItemPredicate.test(content) || trackedItemPredicate.test(stack) && nextCount < incoming;
        }

        if (simulate) {
            return ItemStack.EMPTY;
        }

        setStoredItems(STORAGE_SLOT, nextContent, nextCount);
        if (!hasTrackedDiscard) {
            return ItemStack.EMPTY;
        }

        trackedDiscardListener.run();
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        validateSlotIndex(slot);
        if (slot == DISCARD_SLOT) {
            return ItemStack.EMPTY;
        }
        return super.extractItem(STORAGE_SLOT, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        validateSlotIndex(slot);
        if (slot == DISCARD_SLOT) {
            return VIRTUAL_DISCARD_SLOT_LIMIT;
        }
        return super.getSlotLimit(STORAGE_SLOT);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        ensureValidState();
        return passesItemValidator(stack);
    }

    @Override
    public ItemStack getStoredItem(int slot) {
        validateSlotIndex(slot);
        if (slot == DISCARD_SLOT) {
            return ItemStack.EMPTY;
        }
        return super.getStoredItem(STORAGE_SLOT);
    }

    @Override
    public void setStackInSlot(int slot, ItemStack stack) {
        validateSlotIndex(slot);
        if (slot == DISCARD_SLOT) {
            insertItem(DISCARD_SLOT, stack, false);
            return;
        }

        super.setStackInSlot(STORAGE_SLOT, stack);
    }

    @Override
    public int getCountInSlot(int slot) {
        validateSlotIndex(slot);
        if (slot == DISCARD_SLOT) {
            return 0;
        }
        return super.getCountInSlot(STORAGE_SLOT);
    }

    @Override
    public void setStoredItems(int slot, ItemStack stack, int newCount) {
        validateSlotIndex(slot);
        if (slot == DISCARD_SLOT) {
            if (stack.isEmpty() || newCount <= 0) {
                return;
            }

            insertItem(DISCARD_SLOT, stack.copyWithCount(newCount), false);
            return;
        }

        super.setStoredItems(STORAGE_SLOT, stack, newCount);
    }

    @Override
    protected void validateSlotIndex(int slot) {
        if (slot >= 0 && slot < SLOT_COUNT) {
            return;
        }

        throw new RuntimeException("Slot " + slot + " not in valid range - [0," + SLOT_COUNT + ')');
    }
}
