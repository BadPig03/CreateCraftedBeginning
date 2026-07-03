package net.ty.createcraftedbeginning.api.fillhandlers;

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
public interface AirtightFillHandler {
    SimpleRegistry<Block, AirtightFillHandler> REGISTRY = SimpleRegistry.create();

    Gas apply(Level level, BlockPos pos, BlockState state);
}
