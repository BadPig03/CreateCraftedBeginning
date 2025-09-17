package net.ty.createcraftedbeginning.content.crates.sturdycrate;

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
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.crates.sturdycrate.SturdyCrateBlock.SLOT_LIMIT;

public class SturdyCrateMountedStorage extends MountedItemStorage {
    public static final MapCodec<SturdyCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.CODEC.listOf().fieldOf("stacks").forGetter(storage -> Arrays.asList(storage.storedStacks)), ItemStack.CODEC.fieldOf("filterItem").forGetter(storage -> storage.filterItem)).apply(instance, (stacks, filterItem) -> new SturdyCrateMountedStorage(stacks.toArray(new ItemStack[0]), filterItem)));

    private final ItemStack[] storedStacks = new ItemStack[MAX_SLOT];
    private final ItemStack filterItem;

    protected SturdyCrateMountedStorage(MountedItemStorageType<?> type, ItemStack[] storedStacks, @NotNull ItemStack filterItem) {
        super(type);
        this.filterItem = filterItem.copy();
        copyStacks(storedStacks);
    }

    public SturdyCrateMountedStorage(ItemStack[] storedStacks, ItemStack filterItem) {
        this(CCBMountedStorage.STURDY_CRATE.get(), storedStacks, filterItem);
    }

    private void copyStacks(ItemStack @NotNull [] source) {
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

    private boolean canAcceptItem(@NotNull ItemStack stack) {
        if (!stack.getItem().canFitInsideContainerItems()) {
            return false;
        }

        if (filterItem.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(filterItem, stack);
    }

    @Contract(pure = true)
    private @Nullable ItemStack findFirstNonEmptyItem() {
        for (ItemStack stack : storedStacks) {
            if (!stack.isEmpty()) {
                return stack;
            }
        }
        return null;
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof SturdyCrateBlockEntity crate) {
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
        return canAcceptItem(stack);
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (!isValidSlot(slot) || stack.isEmpty()) {
            return stack;
        }

        if (!canAcceptItem(stack)) {
            return stack;
        }

        ItemStack sampleStack = findFirstNonEmptyItem();
        if (sampleStack != null && !ItemStack.isSameItemSameComponents(sampleStack, stack)) {
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
