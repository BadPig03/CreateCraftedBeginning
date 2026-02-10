package net.ty.createcraftedbeginning.content.airtights.creativeairtighttank;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface ICreativeGasContainer {
    boolean isCreative(Level level, BlockState blockState, BlockPos blockPos);
}
