package net.ty.createcraftedbeginning.gas.visual;

import com.simibubi.create.content.decoration.bracket.BracketedBlockEntityBehaviour;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeBlock;
import net.ty.createcraftedbeginning.content.airtights.airtightpipe.AirtightPipeConnections;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;

import java.util.EnumSet;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasPlacementHelper {
    private GasPlacementHelper() {
    }

    public static EnumSet<Direction> getConnectableDirections(Level level, BlockPos pos) {
        EnumSet<Direction> directions = EnumSet.noneOf(Direction.class);
        for (Direction direction : Iterate.directions) {
            BlockPos adjacentPos = pos.relative(direction);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            Direction oppositeDirection = direction.getOpposite();
            if (adjacentState.getBlock() instanceof AirtightPipeBlock) {
                BracketedBlockEntityBehaviour bracket = BlockEntityBehaviour.get(level, adjacentPos, BracketedBlockEntityBehaviour.TYPE);
                boolean fixed = adjacentState.getValue(AirtightPipeBlock.CASED) || bracket != null && bracket.isBracketPresent();
                if (!fixed || AirtightPipeConnections.isOpen(adjacentState, oppositeDirection)) {
                    directions.add(direction);
                }
                continue;
            }

            if (!(adjacentState.getBlock() instanceof GasConnectable connectable && connectable.canConnectOnFace(adjacentPos, adjacentState, oppositeDirection)) && !GasCapabilities.hasBlockHandler(level, adjacentPos, oppositeDirection)) {
                continue;
            }

            directions.add(direction);
        }
        return directions;
    }

    public static EnumSet<Axis> getConnectableAxes(Set<Direction> directions) {
        EnumSet<Axis> axes = EnumSet.noneOf(Axis.class);
        for (Direction direction : directions) {
            axes.add(direction.getAxis());
        }
        return axes;
    }

    public static Axis chooseAxis(BlockPlaceContext context, Set<Axis> availableAxes) {
        if (availableAxes.size() == 1) {
            return availableAxes.iterator().next();
        }

        if (context.getNearestLookingDirections()[0].getAxis().isHorizontal()) {
            return Axis.Y;
        }

        return context.getHorizontalDirection().getAxis();
    }

    public static @Nullable Direction getPreferredShaft(Level level, BlockPos pos) {
        Direction preferred = null;
        for (Direction side : Iterate.directions) {
            BlockPos neighborPos = pos.relative(side);
            BlockState neighbor = level.getBlockState(neighborPos);
            if (!(neighbor.getBlock() instanceof IRotate rotating) || !rotating.hasShaftTowards(level, neighborPos, neighbor, side.getOpposite())) {
                continue;
            }

            if (preferred != null && preferred.getAxis() != side.getAxis()) {
                return null;
            }

            preferred = side;
        }
        return preferred;
    }

    public static Direction choosePumpFacing(BlockPlaceContext context) {
        return choosePumpFacing(context, true);
    }

    public static Direction choosePumpFacing(BlockPlaceContext context, boolean alignShaft) {
        boolean sneaking = context.getPlayer() != null && context.getPlayer().isShiftKeyDown();
        Direction looking = context.getNearestLookingDirection();
        if (sneaking) {
            return looking;
        }

        Direction target = looking.getOpposite();
        Direction facing = target;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction shaft = null;
        if (alignShaft) {
            shaft = getPreferredShaft(level, pos);
        }
        if (alignShaft && shaft != null) {
            facing = shaft.getOpposite();
        }

        Direction best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (Direction side : getConnectableDirections(level, pos)) {
            int distance = side.getNormal().distManhattan(target.getNormal());
            if (distance > bestDistance) {
                continue;
            }

            bestDistance = distance;
            best = side;
        }
        if (best != null && best.getAxis() != target.getAxis()) {
            return best;
        }

        return facing;
    }
}
