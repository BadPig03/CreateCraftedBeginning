package net.ty.createcraftedbeginning.api.gas.coolantstrategy;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity.CoolantEfficiency;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowderSnowCoolantStrategy implements CoolantStrategyHandler {
    @Override
    public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
        return CoolantEfficiency.EXTREME;
    }

    @Override
    public BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
        return Blocks.AIR.defaultBlockState();
    }
}
