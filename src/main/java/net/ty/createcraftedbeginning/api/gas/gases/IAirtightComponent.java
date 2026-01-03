package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IAirtightComponent {
    boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection);
}