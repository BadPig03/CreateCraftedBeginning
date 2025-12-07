package net.ty.createcraftedbeginning.content.crates.brasscrate;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import com.simibubi.create.content.logistics.filter.FilterItem;
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

public class BrassCrateMountedStorage extends CrateMountedItemStorage {
    public static final MapCodec<BrassCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.CODEC.fieldOf("content").forGetter(storage -> storage.content), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(storage -> storage.count), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("maxCount").forGetter(storage -> storage.maxCount), ItemStack.CODEC.fieldOf("filterItem").forGetter(storage -> storage.filterItem)).apply(instance, BrassCrateMountedStorage::new));

    private final ItemStack filterItem;

    public BrassCrateMountedStorage(ItemStack content, int count, int maxCount, ItemStack filterItem) {
        this(CCBMountedStorage.BRASS_CRATE.get(), content, count, maxCount, filterItem);
    }

    protected BrassCrateMountedStorage(MountedItemStorageType<?> type, @NotNull ItemStack content, int count, int maxCount, @NotNull ItemStack filterItem) {
        super(type, content, count, maxCount);
        this.filterItem = filterItem.copy();
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof BrassCrateBlockEntity crate)) {
            return;
        }

        crate.setStoredItems(content, count);
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack) {
        return slot == 0 && (filterItem.isEmpty() || FilterItem.testDirect(filterItem, stack, false)) && (content.isEmpty() || ItemStack.isSameItemSameComponents(content, stack));
    }
}
