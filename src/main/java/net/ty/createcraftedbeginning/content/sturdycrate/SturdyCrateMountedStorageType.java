package net.ty.createcraftedbeginning.content.sturdycrate;

import com.simibubi.create.api.contraption.storage.item.MountedItemStorageType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import static net.ty.createcraftedbeginning.content.sturdycrate.SturdyCrateBlock.MAX_SLOT;

public class SturdyCrateMountedStorageType extends MountedItemStorageType<SturdyCrateMountedStorage> {
    public SturdyCrateMountedStorageType() {
        super(SturdyCrateMountedStorage.CODEC);
    }

    @Override
    @Nullable
    public SturdyCrateMountedStorage mount(Level level, BlockState state, BlockPos pos, @Nullable BlockEntity be) {
        if (be instanceof SturdyCrateBlockEntity crate) {
            ItemStack[] storedItems = new ItemStack[MAX_SLOT];
            for (int i = 0; i < MAX_SLOT; i++) {
                storedItems[i] = crate.inv.getStackInSlot(i);
            }
            ItemStack filterItem = crate.filtering.getFilter();
            return new SturdyCrateMountedStorage(storedItems, filterItem);
        }

        return null;
    }
}
