package net.ty.createcraftedbeginning.api.coolantshandlers.contents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandler;
import net.ty.createcraftedbeginning.content.airtights.aircompressor.CoolantEfficiency;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

import static net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FROST_LEVEL;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerCoolantHandler implements AirtightCoolantHandler {
    @Override
    public CoolantEfficiency getCoolantEfficiency(Level level, BlockPos pos, BlockState blockState) {
        if (blockState.getValue(FROST_LEVEL).isAtLeast(FrostLevel.CHILLED)) {
            return CoolantEfficiency.EXTREME;
        }
        return CoolantEfficiency.BASIC;
    }

    @Override
    public @Nullable BlockState getMeltBlockState(Level level, BlockPos pos, BlockState blockState) {
        return null;
    }
}
