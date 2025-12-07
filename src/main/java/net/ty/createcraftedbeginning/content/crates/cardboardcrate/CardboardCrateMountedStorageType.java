package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class CardboardCrateMountedStorageType extends MountedItemStorageType<CardboardCrateMountedStorage> {
    public CardboardCrateMountedStorageType() {
        super(CardboardCrateMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public CardboardCrateMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (!(be instanceof CardboardCrateBlockEntity crate)) {
            return null;
        }

        CrateItemStackHandler handler = crate.getHandler();
        return new CardboardCrateMountedStorage(handler.getStackInSlot(0), handler.getCountInSlot(0), handler.getSlotLimit(0));
    }
}
