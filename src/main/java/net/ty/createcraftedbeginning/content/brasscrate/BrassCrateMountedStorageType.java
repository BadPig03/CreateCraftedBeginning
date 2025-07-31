package net.ty.createcraftedbeginning.content.brasscrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import org.jetbrains.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static net.ty.createcraftedbeginning.content.brasscrate.BrassCrateBlock.MAX_SLOT;

public class BrassCrateMountedStorageType extends MountedItemStorageType<BrassCrateMountedStorage> {
    public BrassCrateMountedStorageType() {
        super(BrassCrateMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public BrassCrateMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof BrassCrateBlockEntity crate) {
            ItemStack[] storedItems = new ItemStack[MAX_SLOT];
            for (int i = 0; i < MAX_SLOT; i++) {
                storedItems[i] = crate.inv.getStackInSlot(i);
            }
            ItemStack filterItem = crate.filtering.getFilter();
            return new BrassCrateMountedStorage(storedItems, filterItem);
        }

        return null;
    }
}
