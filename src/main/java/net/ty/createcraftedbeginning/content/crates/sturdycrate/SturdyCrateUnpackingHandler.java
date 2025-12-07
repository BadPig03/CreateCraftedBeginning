package net.ty.createcraftedbeginning.content.crates.sturdycrate;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.crates.CrateContainersUtils;
import net.ty.createcraftedbeginning.content.crates.CrateItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public enum SturdyCrateUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(@NotNull Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof SturdyCrateBlockEntity crate)) {
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

            if (!stack.getItem().canFitInsideContainerItems()) {
                return false;
            }
            if (filter != null && !filter.test(stack)) {
                return false;
            }

            if (!ItemStack.isSameItemSameComponents(stack, content)) {
                return false;
            }
        }
        return CrateContainersUtils.defaultUnpack(level, pos, items, simulate);
    }
}
