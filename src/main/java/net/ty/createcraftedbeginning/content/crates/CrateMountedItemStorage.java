package net.ty.createcraftedbeginning.content.crates;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrateMountedItemStorage extends MountedItemStorage {
    protected ItemStack content;
    protected int count;
    protected final int maxCount;

    protected CrateMountedItemStorage(MountedItemStorageType<?> type, ItemStack content, int count, int maxCount) {
        super(type);
        this.content = content;
        this.count = count;
        this.maxCount = maxCount;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
    }

    @Override
    public int getSlots() {
        return 1;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot != 0) {
            return;
        }

        content = stack.copyWithCount(1);
        count = stack.isEmpty() ? 0 : 1;
    }

    @Override
    public @NotNull ItemStack getStackInSlot(int slot) {
        return slot != 0 || content.isEmpty() || count == 0 ? ItemStack.EMPTY : content.copyWithCount(count);
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (slot != 0 || !isItemValid(slot, stack)) {
            return stack;
        }

        if (content.isEmpty()) {
            int newCount = Math.min(stack.getCount(), maxCount);
            if (!simulate) {
                content = stack.copyWithCount(1);
                count = newCount;
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
        }

        int remaining = stack.getCount() - toInsert;
        return remaining > 0 ? stack.copyWithCount(remaining) : ItemStack.EMPTY;
    }

    @Override
    public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != 0 || amount <= 0 || content.isEmpty() || count == 0) {
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
        }

        return result;
    }

    @Override
    public int getSlotLimit(int slot) {
        return slot != 0 ? 0 : maxCount;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == 0 && (content.isEmpty() || ItemStack.isSameItemSameComponents(content, stack));
    }
}

