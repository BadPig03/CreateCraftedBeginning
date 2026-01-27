package net.ty.createcraftedbeginning.api.gas.extracthandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.common.Tags.Biomes;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeExtractHandler;
import net.ty.createcraftedbeginning.data.CCBGases;
import org.jetbrains.annotations.NotNull;

public class AirExtractHandler implements GasOpenPipeExtractHandler {
    @Override
    public Gas extract(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        if (level.getBiome(pos).is(Biomes.IS_MUSHROOM)) {
            return CCBGases.SPORE_AIR.get();
        }

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
}