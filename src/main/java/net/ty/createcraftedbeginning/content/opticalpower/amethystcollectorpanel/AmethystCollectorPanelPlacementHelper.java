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
import net.minecraft.world.phys.BlockHitResult;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.function.Predicate;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AmethystCollectorPanelPlacementHelper implements IPlacementHelper {
    public static boolean canExtendArray(Level level, BlockPos origin, BlockPos placementPos) {
        if (origin.getY() != placementPos.getY() || origin.distManhattan(placementPos) != 1 || !level.isLoaded(origin) || !level.isLoaded(placementPos) || !level.getWorldBorder().isWithinBounds(placementPos) || level.isOutsideBuildHeight(placementPos) || !(level.getBlockState(origin).getBlock() instanceof AmethystCollectorPanelBlock) || !level.getBlockState(placementPos).canBeReplaced()) {
            return false;
        }

        AmethystCollectorPanelGeometry geometry = AmethystCollectorPanelGeometry.withPlacement(level, origin, placementPos);
        if (!geometry.topologyValid()) {
            return false;
        }

        AmethystCollectorPanelRectangle bounds = AmethystCollectorPanelGeometry.rectangleFromKnownValidDependencies(geometry.dependencies());
        if (bounds == null) {
            return false;
        }

        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(bounds.minX(), origin.getY(), bounds.minZ()), new BlockPos(bounds.maxX(), origin.getY(), bounds.maxZ()))) {
            if (!level.isLoaded(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
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
