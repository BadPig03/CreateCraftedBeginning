package net.ty.createcraftedbeginning.gas.visual;

import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.GasCapabilities;
import net.ty.createcraftedbeginning.gas.behaviour.GasTransportBehaviour;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.EnumSet;
import java.util.Set;

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
            GasTransportBehaviour transport = BlockEntityBehaviour.get(level, adjacentPos, GasTransportBehaviour.TYPE);
            boolean hasConnection = transport != null && transport.canConnectOnFace(adjacentState, oppositeDirection);
            if (!hasConnection && !GasCapabilities.hasBlockHandler(level, adjacentPos, oppositeDirection)) {
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
        Axis lookingAxis = context.getNearestLookingDirection().getAxis();
        if (availableAxes.isEmpty()) {
            return chooseUnconnectedAxis(context);
        }

        if (availableAxes.contains(lookingAxis)) {
            return lookingAxis;
        }

        Axis clickedAxis = context.getClickedFace().getAxis();
        if (availableAxes.contains(clickedAxis)) {
            return clickedAxis;
        }

        for (Direction direction : context.getNearestLookingDirections()) {
            if (!availableAxes.contains(direction.getAxis())) {
                continue;
            }

            return direction.getAxis();
        }
        return chooseUnconnectedAxis(context);
    }

    public static Direction chooseOutputDirection(BlockPlaceContext context, Axis axis, Set<Direction> connectedDirections, boolean preferAwayFromSingleConnection) {
        if (preferAwayFromSingleConnection) {
            Direction onlyConnection = null;
            for (Direction direction : connectedDirections) {
                if (direction.getAxis() != axis) {
                    continue;
                }

                if (onlyConnection != null) {
                    onlyConnection = null;
                    break;
                }

                onlyConnection = direction;
            }

            if (onlyConnection != null) {
                return onlyConnection.getOpposite();
            }
        }

        Direction horizontalDirection = context.getHorizontalDirection();
        if (axis != Axis.Y && horizontalDirection.getAxis() != axis) {
            Direction right = horizontalDirection.getClockWise();
            if (right.getAxis() == axis) {
                return right;
            }
        }

        for (Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() != axis) {
                continue;
            }

            return direction;
        }
        return Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
    }

    private static Axis chooseUnconnectedAxis(BlockPlaceContext context) {
        Axis lookingAxis = context.getNearestLookingDirection().getAxis();
        if (lookingAxis == Axis.Y) {
            return Axis.Y;
        }

        if (context.getHorizontalDirection().getAxis() == Axis.X) {
            return Axis.Z;
        }

        return Axis.X;
    }
}
