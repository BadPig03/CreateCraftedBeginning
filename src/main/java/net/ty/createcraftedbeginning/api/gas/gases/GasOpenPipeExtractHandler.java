package net.ty.createcraftedbeginning.api.gas.gases;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.ty.createcraftedbeginning.data.CCBGases;
import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface GasOpenPipeExtractHandler {
    int MAX_CAPACITY = 1000;

    SimpleRegistry<Block, GasOpenPipeExtractHandler> REGISTRY = SimpleRegistry.create();

    default Gas getDimensionUniqueAir(@NotNull Level level) {
        DimensionType dimensionType = level.dimensionType();
        if (dimensionType.ultraWarm()) {
            return CCBGases.ULTRAWARM_AIR.get();
        }
        else if (dimensionType.natural()) {
            return CCBGases.NATURAL_AIR.get();
        }
        else {
            return CCBGases.ETHEREAL_AIR.get();
        }
    }

    GasStack extract(Level level, BlockPos pos, BlockState state, Direction direction);
}
