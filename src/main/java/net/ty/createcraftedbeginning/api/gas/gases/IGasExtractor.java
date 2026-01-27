package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface IGasExtractor {
    boolean canExtract(Level level, BlockState blockState, BlockPos blockPos, Direction direction);
}
