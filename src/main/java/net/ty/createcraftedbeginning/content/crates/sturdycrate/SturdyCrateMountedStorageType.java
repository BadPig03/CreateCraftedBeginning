package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class SturdyCrateMountedStorageType extends MountedItemStorageType<SturdyCrateMountedStorage> {
    public SturdyCrateMountedStorageType() {
        super(SturdyCrateMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public SturdyCrateMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof SturdyCrateBlockEntity crate)) {
            return null;
        }

        CrateItemStackHandler handler = crate.getHandler();
        return new SturdyCrateMountedStorage(handler.getStackInSlot(0), handler.getCountInSlot(0), handler.getSlotLimit(0), crate.getFilteringBehaviour().getFilter());
    }
}
