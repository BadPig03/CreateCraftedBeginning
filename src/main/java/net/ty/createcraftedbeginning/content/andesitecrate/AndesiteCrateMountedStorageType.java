package net.ty.createcraftedbeginning.content.andesitecrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static net.ty.createcraftedbeginning.content.andesitecrate.AndesiteCrateBlock.MAX_SLOT;

public class AndesiteCrateMountedStorageType extends MountedItemStorageType<AndesiteCrateMountedStorage> {
    public AndesiteCrateMountedStorageType() {
        super(AndesiteCrateMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public AndesiteCrateMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof AndesiteCrateBlockEntity crate) {
            ItemStack[] storedItems = new ItemStack[MAX_SLOT];
            for (int i = 0; i < MAX_SLOT; i++) {
                storedItems[i] = crate.inv.getStackInSlot(i);
            }
            return new AndesiteCrateMountedStorage(storedItems);
        }

        return null;
    }
}
