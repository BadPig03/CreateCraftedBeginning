package net.ty.createcraftedbeginning.api.coolantstrategy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity.CoolantEfficiency;
import org.jetbrains.annotations.NotNull;

public class DowngradeCoolantStrategy implements CoolantStrategyHandler {
    @Override
    public @NotNull CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, @NotNull BlockState blockState) {
        return CoolantEfficiency.ADVANCED;
    }

    @Override
    public BlockState getMeltBlockState(Level level, BlockPos pos, @NotNull BlockState blockState) {
        return blockState.is(Blocks.BLUE_ICE) ? Blocks.PACKED_ICE.defaultBlockState() : Blocks.ICE.defaultBlockState();
    }
}
