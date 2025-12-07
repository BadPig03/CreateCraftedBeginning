package net.ty.createcraftedbeginning.content.crates.cardboardcrate;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public enum CardboardCrateUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(@NotNull Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof CardboardCrateBlockEntity crate)) {
            return false;
        }

        CrateItemStackHandler handler = crate.getHandler();
        items.forEach(stack -> handler.insertItem(0, stack, simulate));
        return true;
    }
}
