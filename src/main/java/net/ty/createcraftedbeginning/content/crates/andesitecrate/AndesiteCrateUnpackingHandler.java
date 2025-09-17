package net.ty.createcraftedbeginning.content.crates.andesitecrate;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public enum AndesiteCrateUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(@NotNull Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AndesiteCrateBlockEntity crate)) {
            return false;
        }

        ItemStackHandler inv = crate.getInv();
        ItemStack crateReference = null;
        for (int i = 0; i < inv.getSlots(); i++) {
            ItemStack inSlot = inv.getStackInSlot(i);
            if (!inSlot.isEmpty()) {
                crateReference = inSlot;
                break;
            }
        }

        ItemStack packageReference = null;
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }

            if (crateReference != null) {
                if (!ItemStack.isSameItemSameComponents(crateReference, stack)) {
                    return false;
                }
            } else {
                if (packageReference == null) {
                    packageReference = stack;
                } else if (!ItemStack.isSameItemSameComponents(packageReference, stack)) {
                    return false;
                }
            }
        }

        return UnpackingHandler.DEFAULT.unpack(level, pos, state, side, items, orderContext, simulate);
    }
}
