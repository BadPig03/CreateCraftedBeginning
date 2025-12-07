package net.ty.createcraftedbeginning.api.gas.extracthandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.interfaces.GasOpenPipeExtractHandler;
import net.ty.createcraftedbeginning.data.CCBGases;
import org.jetbrains.annotations.NotNull;

public class AirtightPipeExtractHandler implements GasOpenPipeExtractHandler {
    @Override
    public GasStack extract(@NotNull Level level, @NotNull BlockPos pos, BlockState state, Direction direction) {
        BlockState otherState = level.getBlockState(pos.relative(direction));
        if (otherState.isAir()) {
            return new GasStack(getDimensionUniqueAir(level), MAX_CAPACITY);
        }

        return otherState.is(Blocks.BUBBLE_COLUMN) ? new GasStack(CCBGases.MOIST_AIR.get(), MAX_CAPACITY) : GasStack.EMPTY;
    }
}
