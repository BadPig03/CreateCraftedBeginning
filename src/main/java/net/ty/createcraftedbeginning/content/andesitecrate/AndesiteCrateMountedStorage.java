package net.ty.createcraftedbeginning.content.andesitecrate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorage;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlock.SLOT_LIMIT;

public class AndesiteCrateMountedStorage extends MountedItemStorage {
    public static final MapCodec<AndesiteCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(storage -> Arrays.asList(storage.storedStacks))).apply(instance, stacks -> new AndesiteCrateMountedStorage(stacks.toArray(new ItemStack[0]))));

    private final ItemStack[] storedStacks = new ItemStack[MAX_SLOT];

    protected AndesiteCrateMountedStorage(MountedItemStorageType<?> type, ItemStack[] storedStacks) {
        super(type);
        for (int i = 0; i < MAX_SLOT; i++) {
            this.storedStacks[i] = storedStacks[i].copy();
        }
    }

    public AndesiteCrateMountedStorage(ItemStack[] storedStacks) {
        this(CCBMountedStorage.ANDESITE_CRATE.get(), storedStacks);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof AndesiteCrateBlockEntity crate) {
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
        if (slot >= 0 && slot < MAX_SLOT) {
            return storedStacks[slot].copy();
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot >= 0 && slot < MAX_SLOT) {
            storedStacks[slot] = stack.copy();
        }
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        ItemStack sampleStack = findFirstNonEmptyItem();

        if (sampleStack == null) {
            return true;
        }

        return ItemStack.isSameItemSameComponents(sampleStack, stack);
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || slot < 0 || slot >= MAX_SLOT) {
            return stack;
        }

        ItemStack sampleStack = findFirstNonEmptyItem();

        if (sampleStack != null && !ItemStack.isSameItemSameComponents(sampleStack, stack)) {
            return stack;
        }

        return insertToSpecificSlot(slot, stack, simulate);
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot < 0 || slot >= MAX_SLOT || amount <= 0) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = storedStacks[slot];
        if (stackInSlot.isEmpty()) {
            return ItemStack.EMPTY;
        }

        return performExtraction(slot, stackInSlot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return SLOT_LIMIT;
    }

    private ItemStack findFirstNonEmptyItem() {
        for (ItemStack stack : storedStacks) {
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    private ItemStack insertToSpecificSlot(int slot, ItemStack stack, boolean simulate) {
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
            storedStacks[slot].grow(toAdd);
        }
        return toAdd >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toAdd);
    }

    private ItemStack performExtraction(int slot, ItemStack stack, int amount, boolean simulate) {
        int toExtract = Math.min(amount, stack.getCount());
        ItemStack extracted = stack.copyWithCount(toExtract);

        if (!simulate) {
            stack.shrink(toExtract);
            if (stack.isEmpty()) {
                storedStacks[slot] = ItemStack.EMPTY;
            }
        }

        return extracted;
    }
}
