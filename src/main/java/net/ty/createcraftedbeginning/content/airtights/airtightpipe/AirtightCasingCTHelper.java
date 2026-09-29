package net.ty.createcraftedbeginning.content.airtights.airtightpipe;

import net.createmod.catnip.data.Iterate;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.ty.createcraftedbeginning.content.airtights.airtightmeters.AbstractAirtightMeterBlock;
import net.ty.createcraftedbeginning.gas.network.GasConnectable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightCasingCTHelper {
    private AirtightCasingCTHelper() {
    }

    public static boolean isCasingComponent(BlockState state) {
        if (state.getBlock() instanceof AirtightPipeBlock) {
            return state.getValue(AirtightPipeBlock.CASED);
        }

        return state.getBlock() instanceof AbstractAirtightMeterBlock;
    }

    public static boolean isCasingSurface(BlockState state, Direction face) {
        if (!isCasingComponent(state)) {
            return false;
        }

        Axis pipeAxis = state.getValue(BlockStateProperties.AXIS);
        return face.getAxis() != pipeAxis && (!(state.getBlock() instanceof AbstractAirtightMeterBlock) || !AbstractAirtightMeterBlock.isDisplayFace(state, face));
    }

    public static boolean hasCasingNeighbourOnSurface(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction face) {
        if (!isCasingSurface(state, face)) {
            return false;
        }

        Axis pipeAxis = state.getValue(BlockStateProperties.AXIS);
        for (Direction direction : Iterate.directions) {
            if (direction.getAxis() != pipeAxis || !hasCasingConnection(level, pos, state, direction, face)) {
                continue;
            }

            return true;
        }
        return false;
    }

    public static boolean connects(BlockState state, BlockState other, BlockPos pos, BlockPos otherPos, Direction face) {
        if (!isCasingSurface(state, face) || !isCasingSurface(other, face)) {
            return false;
        }

        Direction connectionDirection = null;
        for (Direction direction : Iterate.directions) {
            if (!pos.relative(direction).equals(otherPos)) {
                continue;
            }

            connectionDirection = direction;
            break;
        }
        return connectionDirection != null && connectionDirection.getAxis() == state.getValue(BlockStateProperties.AXIS) && hasCasingConnection(pos, state, otherPos, other, connectionDirection, face);
    }

    private static boolean hasCasingConnection(BlockAndTintGetter level, BlockPos pos, BlockState state, Direction direction, Direction face) {
        BlockPos otherPos = pos.relative(direction);
        BlockState otherState = level.getBlockState(otherPos);
        return hasCasingConnection(pos, state, otherPos, otherState, direction, face);
    }

    private static boolean hasCasingConnection(BlockPos pos, BlockState state, BlockPos otherPos, BlockState otherState, Direction direction, Direction face) {
        return isCasingSurface(state, face) && isCasingSurface(otherState, face) && direction.getAxis() == state.getValue(BlockStateProperties.AXIS) && canConnectOnFace(pos, state, direction) && canConnectOnFace(otherPos, otherState, direction.getOpposite());
    }

    private static boolean canConnectOnFace(BlockPos pos, BlockState state, Direction face) {
        return state.getBlock() instanceof GasConnectable component && component.canConnectOnFace(pos, state, face);
    }
}
