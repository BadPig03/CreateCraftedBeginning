package net.ty.createcraftedbeginning.api.gas.extracthandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeExtractHandler;
import net.ty.createcraftedbeginning.data.CCBGases;
import org.jetbrains.annotations.NotNull;

public class AirExtractHandler implements GasOpenPipeExtractHandler {
    @Override
    public Gas extract(@NotNull Level level, @NotNull BlockPos pos, @NotNull BlockState state) {
        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(Tags.Biomes.IS_MUSHROOM)) {
            return CCBGases.SPORE_AIR.get();
        }
        else if (biome.is(Biomes.DEEP_DARK)) {
            return CCBGases.SCULK_AIR.get();
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