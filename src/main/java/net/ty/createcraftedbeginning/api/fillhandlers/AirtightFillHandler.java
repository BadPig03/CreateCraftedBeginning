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

    /**
     * Applies this operation to the supplied context.
     *
     * @param level the level in which the operation is performed
     * @param pos   the target block position
     * @param state the block state to inspect or process
     * @return the resulting gas
     */
    Gas apply(Level level, BlockPos pos, BlockState state);
}
