package net.ty.createcraftedbeginning.api.coolantstrategy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.AirCompressorBlockEntity.CoolantEfficiency;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import org.jetbrains.annotations.NotNull;

import static net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FROST_LEVEL;

public class BreezeCoolerCoolantStrategy implements CoolantStrategyHandler {
    @Override
    public @NotNull CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, @NotNull BlockState blockState) {
        if (!(blockState.getBlock() instanceof BreezeCoolerBlock)) {
            return CoolantEfficiency.NONE;
        }

        FrostLevel frostLevel = blockState.getValue(FROST_LEVEL);
        return frostLevel.isAtLeast(FrostLevel.CHILLED) ? CoolantEfficiency.EXTREME : CoolantEfficiency.BASIC;
    }

    @Override
    public BlockState getMeltBlockState(Level level, BlockPos pos, @NotNull BlockState blockState) {
        return null;
    }
}
