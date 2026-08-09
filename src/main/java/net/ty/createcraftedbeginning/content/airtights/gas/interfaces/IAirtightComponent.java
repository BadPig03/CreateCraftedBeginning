package net.ty.createcraftedbeginning.content.airtights.gas.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface IAirtightComponent {
    boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection);
}