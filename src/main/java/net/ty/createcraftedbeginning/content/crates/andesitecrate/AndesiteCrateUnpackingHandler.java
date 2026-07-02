package net.ty.createcraftedbeginning.content.crates.andesitecrate;

import com.simibubi.create.api.packager.unpacking.UnpackingHandler;
import com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts;
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
public enum AndesiteCrateUnpackingHandler implements UnpackingHandler {
    INSTANCE;

    @Override
    public boolean unpack(Level level, BlockPos pos, BlockState state, Direction side, List<ItemStack> items, @Nullable PackageOrderWithCrafts orderContext, boolean simulate) {
        if (!(level.getBlockEntity(pos) instanceof AndesiteCrateBlockEntity crate)) {
            return false;
        }

        CrateItemStackHandler handler = crate.getHandler();
        ItemStack content = handler.getStackInSlot(0);
        if (content.isEmpty()) {
            content = items.getFirst();
        }

        for (ItemStack stack : items) {
            if (stack.isEmpty()) {
                continue;
            }

            if (!ItemStack.isSameItemSameComponents(stack, content)) {
                return false;
            }
        }

        return CrateContainersUtils.defaultUnpack(level, pos, items, simulate);
    }
}
