package net.ty.createcraftedbeginning.content.crates.brasscrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class BrassCrateMountedStorageType extends MountedItemStorageType<BrassCrateMountedStorage> {
    public BrassCrateMountedStorageType() {
        super(BrassCrateMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public BrassCrateMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof BrassCrateBlockEntity crate)) {
            return null;
        }

        CrateItemStackHandler handler = crate.getHandler();
        return new BrassCrateMountedStorage(handler.getStackInSlot(0), handler.getCountInSlot(0), handler.getSlotLimit(0), crate.getFilteringBehaviour().getFilter());
    }
}
