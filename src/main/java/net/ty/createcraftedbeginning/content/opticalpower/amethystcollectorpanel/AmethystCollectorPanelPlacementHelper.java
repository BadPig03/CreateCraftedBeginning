package net.ty.createcraftedbeginning.content.opticalpower.amethystcollectorpanel;

import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class AmethystCollectorPanelPlacementHelper implements IPlacementHelper {
    static boolean canExtendArray(Level level, BlockPos origin, BlockPos placementPos) {
        int y = origin.getY();
        if (y != placementPos.getY() || origin.distManhattan(placementPos) != 1 || !level.isLoaded(origin) || !level.isLoaded(placementPos)) {
            return false;
        }

        WorldBorder worldBorder = level.getWorldBorder();
        if (!worldBorder.isWithinBounds(placementPos) || level.isOutsideBuildHeight(placementPos) || !(level.getBlockState(origin).getBlock() instanceof AmethystCollectorPanelBlock) || !level.getBlockState(placementPos).canBeReplaced()) {
            return false;
        }

        AmethystCollectorPanelRectangle bounds = AmethystCollectorPanelGeometry.findPlacementBounds(level, origin, placementPos);
        if (bounds == null) {
            return false;
        }

        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(bounds.minX(), y, bounds.minZ()), new BlockPos(bounds.maxX(), y, bounds.maxZ()))) {
            if (!level.isLoaded(pos) || !worldBorder.isWithinBounds(pos)) {
                return false;
            }

            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof AmethystCollectorPanelBlock || state.canBeReplaced()) {
                continue;
            }

            return false;
        }
        return true;
    }

    @Override
    public Predicate<ItemStack> getItemPredicate() {
        return CCBBlocks.AMETHYST_COLLECTOR_PANEL_BLOCK::isIn;
    }

    @Override
    public Predicate<BlockState> getStatePredicate() {
        return state -> state.getBlock() instanceof AmethystCollectorPanelBlock;
    }

    @Override
    public PlacementOffset getOffset(Player player, Level level, BlockState state, BlockPos pos, BlockHitResult ray) {
        if (player.isShiftKeyDown() || !player.mayBuild()) {
            return PlacementOffset.fail();
        }

        List<Direction> directions = IPlacementHelper.orderedByDistanceExceptAxis(pos, ray.getLocation(), Axis.Y);
        for (Direction direction : directions) {
            BlockPos placementPos = pos.relative(direction);
            if (!canExtendArray(level, pos, placementPos)) {
                continue;
            }

            return PlacementOffset.success(placementPos);
        }
        return PlacementOffset.fail();
    }
}
