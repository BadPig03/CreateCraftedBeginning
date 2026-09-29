package net.ty.createcraftedbeginning.gas.network;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface GasConnectable {
    boolean canConnectOnFace(BlockPos currentPos, BlockState currentState, Direction localFace);

    default boolean propagatesGasNeighborUpdates() {
        return true;
    }

    default boolean blocksAtmosphere(BlockPos pos, BlockState state, Direction face) {
        return false;
    }
}
