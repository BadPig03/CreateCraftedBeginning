package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface GasOpenPipeExtractHandler {
    SimpleRegistry<Block, GasOpenPipeExtractHandler> REGISTRY = SimpleRegistry.create();

    Gas extract(Level level, BlockPos pos, BlockState state);
}
