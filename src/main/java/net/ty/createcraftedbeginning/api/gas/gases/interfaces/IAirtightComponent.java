package net.ty.createcraftedbeginning.api.gas.gases.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface IAirtightComponent {
    /**
     * Checks whether this value is airtight.
     *
     * @param currentPos        the current pos to use
     * @param currentState      the current state to use
     * @param oppositeDirection the opposite direction to use
     * @return {@code true} if this value is airtight; otherwise {@code false}
     */
    boolean isAirtight(BlockPos currentPos, BlockState currentState, Direction oppositeDirection);
}