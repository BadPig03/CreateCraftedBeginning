package net.ty.createcraftedbeginning.content.crates.brasscrate;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateContainersUtils;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("UnstableApiUsage")
public enum BrassCrateUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof BrassCrateBlockEntity crate)) {
            return false;
        }

        CrateItemStackHandler handler = crate.getHandler();
        ItemStack content = handler.getStackInSlot(0);
        if (content.isEmpty()) {
            content = items.getFirst();
        }

        FilteringBehaviour filter = crate.getFilteringBehaviour();
        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }

            if (filter.test(stack) && ItemStack.isSameItemSameComponents(stack, content)) {
                continue;
            }

            return false;
        }
        return CrateContainersUtils.defaultUnpack(level, pos, items, simulate);
    }
}
