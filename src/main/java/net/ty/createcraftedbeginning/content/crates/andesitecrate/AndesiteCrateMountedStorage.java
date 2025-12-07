package net.ty.createcraftedbeginning.content.crates.andesitecrate;

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

public class AndesiteCrateMountedStorage extends CrateMountedItemStorage {
    public static final MapCodec<AndesiteCrateMountedStorage> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(ItemStack.CODEC.fieldOf("content").forGetter(storage -> storage.content), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("count").forGetter(storage -> storage.count), ExtraCodecs.NON_NEGATIVE_INT.fieldOf("maxCount").forGetter(storage -> storage.maxCount)).apply(instance, AndesiteCrateMountedStorage::new));

    public AndesiteCrateMountedStorage(ItemStack content, int count, int maxCount) {
        this(CCBMountedStorage.ANDESITE_CRATE.get(), content, count, maxCount);
    }

    protected AndesiteCrateMountedStorage(MountedItemStorageType<?> type, @NotNull ItemStack content, int count, int maxCount) {
        super(type, content, count, maxCount);
    }

    @Override
    public void unmount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof AndesiteCrateBlockEntity crate)) {
            return;
        }

        crate.setStoredItems(content, count);
    }
}
