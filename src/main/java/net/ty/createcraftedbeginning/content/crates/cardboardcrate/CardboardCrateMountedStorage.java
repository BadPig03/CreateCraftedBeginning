package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateMountedItemStorage;
import net.ty.createcraftedbeginning.registry.CCBMountedStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CardboardCrateMountedStorage extends CrateMountedItemStorage {
    public static final MapCodec<CardboardCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.CODEC.fieldOf("content").forGetter(storage -> storage.content), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(storage -> storage.count), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("maxCount").forGetter(storage -> storage.maxCount)).apply(instance, CardboardCrateMountedStorage::new));

    public CardboardCrateMountedStorage(ItemStack content, int count, int maxCount) {
        this(CCBMountedStorage.CARDBOARD_CRATE.get(), content, count, maxCount);
    }

    protected CardboardCrateMountedStorage(MountedItemStorageType<?> type, @NotNull ItemStack content, int count, int maxCount) {
        super(type, content, count, maxCount);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof CardboardCrateBlockEntity crate)) {
            return;
        }

        crate.setStoredItems(content, count);
    }

    @Override
    public int getSlots() {
        return 2;
    }

    @Override
    public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (content.isEmpty()) {
            int newCount = Math.min(stack.getCount(), maxCount);
            if (!simulate) {
                content = stack.copyWithCount(1);
                count = newCount;
            }
            return ItemStack.EMPTY;
        }

        if (ItemStack.isSameItemSameComponents(content, stack)) {
            int space = maxCount - count;
            if (space <= 0) {
                return ItemStack.EMPTY;
            }

            int toInsert = Math.min(stack.getCount(), space);
            if (!simulate) {
                count += toInsert;
            }

            return ItemStack.EMPTY;
        }

        if (!simulate) {
            content = stack.copyWithCount(1);
            count = Math.min(stack.getCount(), maxCount);
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == 0;
    }
}
