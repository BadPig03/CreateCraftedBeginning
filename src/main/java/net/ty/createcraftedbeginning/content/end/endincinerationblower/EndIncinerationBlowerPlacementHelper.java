package net.ty.createcraftedbeginning.content.end.endincinerationblower;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.content.end.endcasing.EndCasingBlock;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EndIncinerationBlowerPlacementHelper implements IPlacementHelper {
    @Contract(pure = true)
    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return CCBBlocks.END_INCINERATION_BLOWER_BLOCK::isIn;
    }

    @Contract(pure = true)
    @Override
    public Predicate<BlockState> getStatePredicate() {
        return state -> state.getBlock() instanceof EndCasingBlock;
    }

    @Contract(pure = true)
    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray) {
        BlockPos newPos = pos.above();
        if (!level.getBlockState(newPos).canBeReplaced()) {
            return PlacementOffset.fail();
        }

        return PlacementOffset.success(newPos, s -> CCBBlocks.END_INCINERATION_BLOWER_BLOCK.get().defaultBlockState());
    }
}
