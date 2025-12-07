package net.ty.createcraftedbeginning.content.crates.andesitecrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import org.jetbrains.annotations.Nullable;


public class AndesiteCrateMountedStorageType extends MountedItemStorageType<AndesiteCrateMountedStorage> {
    public AndesiteCrateMountedStorageType() {
        super(AndesiteCrateMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public AndesiteCrateMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof AndesiteCrateBlockEntity crate)) {
            return null;
        }

        CrateItemStackHandler handler = crate.getHandler();
        return new AndesiteCrateMountedStorage(handler.getStackInSlot(0), handler.getCountInSlot(0), handler.getSlotLimit(0));
    }
}
