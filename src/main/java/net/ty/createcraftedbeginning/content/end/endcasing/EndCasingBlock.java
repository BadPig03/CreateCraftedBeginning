package net.ty.createcraftedbeginning.content.end.endcasing;

import com.simibubi.create.content.decoration.encasing.CasingBlock;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class EndCasingBlock extends CasingBlock {
    private static final int PLACEMENT_HELPER_ID = PlacementHelpers.register(new NozzlePlacementHelper());

    public EndCasingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getLightEmission(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos) {
        return 15;
    }

    @Override
    protected @NotNull ItemInteractionResult useItemOn(@NotNull ItemStack stack, @NotNull BlockState state, @NotNull Level level, @NotNull BlockPos pos, @NotNull Player player, @NotNull InteractionHand hand, @NotNull BlockHitResult hitResult) {
        IPlacementHelper placementHelper = PlacementHelpers.get(PLACEMENT_HELPER_ID);
        if (placementHelper.matchesItem(stack)) {
            return placementHelper.getOffset(player, level, state, pos, hitResult).placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);
        }

        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    private static class NozzlePlacementHelper implements IPlacementHelper {
        @Contract(pure = true)
        @Override
        public @NotNull Predicate<ItemStack> getItemPredicate() {
            return CCBBlocks.END_INCINERATION_BLOWER_BLOCK::isIn;
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

            return PlacementOffset.success(newPos, s -> CCBBlocks.END_INCINERATION_BLOWER_BLOCK.get().defaultBlockState());
        }
    }
}
