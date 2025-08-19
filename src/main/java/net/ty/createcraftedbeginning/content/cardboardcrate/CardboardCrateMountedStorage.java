package net.ty.createcraftedbeginning.content.cardboardcrate;

import com.mojang.serialization.MapCodec;
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

import static net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlock.MAX_SLOT;
import static net.ty.createcraftedbeginning.content.cardboardcrate.CardboardCrateBlock.SLOT_LIMIT;

public class CardboardCrateMountedStorage extends MountedItemStorage {
    public static final MapCodec<CardboardCrateMountedStorage> CODEC = ItemStack.OPTIONAL_CODEC.xmap(CardboardCrateMountedStorage::new, storage -> storage.storedStack.copy()).fieldOf("value");

    private static final int STORAGE_SLOT = 0;
    private ItemStack storedStack;

    protected CardboardCrateMountedStorage(MountedItemStorageType<?> type, ItemStack storedStack) {
        super(type);
        this.storedStack = storedStack.copy();
    }

    public CardboardCrateMountedStorage(ItemStack storedStack) {
        this(CCBMountedStorage.CARDBOARD_CRATE.get(), storedStack);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof CardboardCrateBlockEntity crate) {
            crate.setStoredItem(storedStack);
        }
    }

    @Override
    public int getSlots() {
        return MAX_SLOT;
    }

    @Override
    @NotNull
    public ItemStack getStackInSlot(int slot) {
        return slot == STORAGE_SLOT ? storedStack.copy() : ItemStack.EMPTY;
    }

    @Override
    public void setStackInSlot(int slot, @NotNull ItemStack stack) {
        if (slot == STORAGE_SLOT) {
            storedStack = stack.copy();
        }
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == STORAGE_SLOT;
    }

    @Override
    @NotNull
    public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (storedStack.isEmpty()) {
            int maxInsert = Math.min(stack.getCount(), SLOT_LIMIT);
            if (!simulate) {
                storedStack = stack.copyWithCount(maxInsert);
            }
            return maxInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - maxInsert);
        }

        if (ItemStack.isSameItemSameComponents(storedStack, stack)) {
            int maxStackSize = Math.min(storedStack.getMaxStackSize(), SLOT_LIMIT);
            int availableSpace = maxStackSize - storedStack.getCount();

            if (availableSpace <= 0) {
                return ItemStack.EMPTY;
            }

            int toInsert = Math.min(stack.getCount(), availableSpace);
            if (!simulate) {
                storedStack.grow(toInsert);
            }
            return toInsert >= stack.getCount() ? ItemStack.EMPTY : stack.copyWithCount(stack.getCount() - toInsert);
        }

        if (!simulate) {
            int maxInsert = Math.min(stack.getCount(), SLOT_LIMIT);
            storedStack = stack.copyWithCount(maxInsert);
        }
        return ItemStack.EMPTY;
    }

    @Override
    @NotNull
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (slot != STORAGE_SLOT || storedStack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        int toExtract = Math.min(amount, storedStack.getCount());
        ItemStack extracted = storedStack.copyWithCount(toExtract);

        if (!simulate) {
            storedStack.shrink(toExtract);
        }

        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return SLOT_LIMIT;
    }
}
