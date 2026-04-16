package net.ty.createcraftedbeginning.content.end.endsculksilencer;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class EndSculkSilencerPlacementHelper implements IPlacementHelper {
    @Contract(pure = true)
    @Override
    public @NotNull Predicate<ItemStack> getItemPredicate() {
        return CCBBlocks.END_SCULK_SILENCER_BLOCK::isIn;
    }

    @Contract(pure = true)
    @Override
    public @NotNull Predicate<BlockState> getStatePredicate() {
        return CCBBlocks.END_CASING_BLOCK::has;
    }

    @Contract(pure = true)
    @Override
    public @NotNull PlacementOffset getOffset(@NotNull Player player, @NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull BlockHitResult ray) {
        BlockPos newPos = pos.above();
        if (!level.getBlockState(newPos).canBeReplaced()) {
            return PlacementOffset.fail();
        }

        return PlacementOffset.success(newPos, s -> CCBBlocks.END_SCULK_SILENCER_BLOCK.get().defaultBlockState());
    }
}
