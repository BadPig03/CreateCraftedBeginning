package net.ty.createcraftedbeginning.api.gas.extracthandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface AirtightPipeExtractHandler {
    SimpleRegistry<Block, AirtightPipeExtractHandler> REGISTRY = SimpleRegistry.create();

    Gas extract(Level level, BlockPos pos, BlockState state);
}
