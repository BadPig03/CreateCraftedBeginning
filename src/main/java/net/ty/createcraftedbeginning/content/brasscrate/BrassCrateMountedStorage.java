package net.ty.createcraftedbeginning.content.brasscrate;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.mojang.serialization.MapCodec;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Arrays;

import static net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlock.SLOT_LIMIT;

public class BrassCrateMountedStorage extends MountedItemStorage {
    public static final MapCodec<BrassCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(
        instance -> instance.group(
            ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(storage -> Arrays.asList(storage.storedStacks)),
            ItemStack.CODEC.fieldOf("filterItem").forGetter(storage -> storage.filterItem)
        ).apply(instance, (stacks, filterItem) ->
            new BrassCrateMountedStorage(stacks.toArray(new ItemStack[0]), filterItem)
        )
    );

    private final ItemStack[] storedStacks = new ItemStack[MAX_SLOT];
    private final ItemStack filterItem;

    protected BrassCrateMountedStorage(MountedItemStorageType<?> type, ItemStack[] storedStacks, ItemStack filterItem) {
        super(type);
        this.filterItem = filterItem.copy();
        copyStacks(storedStacks);
    }

    public BrassCrateMountedStorage(ItemStack[] storedStacks, ItemStack filterItem) {
        this(CCBMountedStorage.BRASS_CRATE.get(), storedStacks, filterItem);
    }

    private void copyStacks(ItemStack[] source) {
        int count = Math.min(MAX_SLOT, source.length);
        for (int i = 0; i < count; i++) {
            storedStacks[i] = source[i].copy();
        }

        for (int i = count; i < MAX_SLOT; i++) {
            storedStacks[i] = ItemStack.EMPTY;
        }
    }

    private boolean isValidSlot(int slot) {
        return slot >= 0 && slot < MAX_SLOT;
    }

    private boolean canAcceptItem(ItemStack stack) {
        if (filterItem.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(filterItem, stack);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof BrassCrateBlockEntity crate) {
            crate.setStoredItems(storedStacks);
        }
    }

    @Override
    public int getSlots() {
        return MAX_SLOT;
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        if (isValidSlot(slot)) {
            return storedStacks[slot].copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (isValidSlot(slot)) {
            storedStacks[slot] = stack.copy();
        }
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (!canAcceptItem(stack)) {
            return false;
        }

        if (!stack.isEmpty()) {
            ItemStack reference = null;
            for (ItemStack storedStack : storedStacks) {
                if (!storedStack.isEmpty()) {
                    reference = storedStack;
                    break;
                }
            }

            return reference == null || ItemStack.isSameItemSameComponents(reference, stack);
        }
        return true;
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!isItemValid(slot, stack)) {
            return stack;
        }

        if (!isValidSlot(slot) || stack.isEmpty()) {
            return stack;
        }

        ItemStack existing = storedStacks[slot];

        if (existing.isEmpty()) {
            int toAdd = Math.min(SLOT_LIMIT, stack.getCount());
            if (!simulate) {
                storedStacks[slot] = stack.copyWithCount(toAdd);
            }
            return toAdd >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toAdd);
        }

        if (!ItemStack.isSameItemSameComponents(existing, stack)) {
            return stack;
        }

        int toAdd = Math.min(SLOT_LIMIT - existing.getCount(), stack.getCount());
        if (!simulate) {
            existing.grow(toAdd);
        }
        return toAdd >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toAdd);
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!isValidSlot(slot) || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = storedStacks[slot];
        if (stackInSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, stackInSlot.getCount());
        ItemStack extracted = stackInSlot.copyWithCount(toExtract);

        if (!simulate) {
            stackInSlot.shrink(toExtract);
            if (stackInSlot.isEmpty()) {
                storedStacks[slot] = ItemStack.EMPTY;
            }
        }

        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return SLOT_LIMIT;
    }
}
