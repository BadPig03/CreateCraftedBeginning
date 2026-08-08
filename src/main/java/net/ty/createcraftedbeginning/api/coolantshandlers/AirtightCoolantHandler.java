package net.ty.createcraftedbeginning.api.coolantshandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface AirtightCoolantHandler {
    SimpleRegistry<Block, AirtightCoolantHandler> REGISTRY = SimpleRegistry.create();

    /**
     * Returns the coolant efficiency.
     *
     * @param level      the level in which the operation is performed
     * @param pos        the target block position
     * @param blockState the block state to inspect or process
     * @return the coolant efficiency
     */
    CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState);

    /**
     * Returns the melt block state.
     *
     * @param level      the level in which the operation is performed
     * @param pos        the target block position
     * @param blockState the block state to inspect or process
     * @return the melt block state
     */
    @Nullable BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState);
}
