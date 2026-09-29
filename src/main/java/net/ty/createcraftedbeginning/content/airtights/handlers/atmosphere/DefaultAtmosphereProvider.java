package net.ty.createcraftedbeginning.content.airtights.handlers.atmosphere;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.neoforge.common.Tags;
import net.ty.createcraftedbeginning.api.atmosphere.AtmosphereProvider;
import net.ty.createcraftedbeginning.api.gas.Gas;
import net.ty.createcraftedbeginning.api.gas.GasPressure;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.OptionalLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultAtmosphereProvider implements AtmosphereProvider {
    @Override
    public Gas resolveComposition(Level level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return Gas.EMPTY_GAS_HOLDER.value();
        }

        BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.BUBBLE_COLUMN)) {
            return CCBGases.MOIST_AIR.get();
        }

        if (!state.is(BlockTags.AIR)) {
            return Gas.EMPTY_GAS_HOLDER.value();
        }

        Holder<Biome> biome = level.getBiome(pos);
        if (biome.is(Tags.Biomes.IS_MUSHROOM)) {
            return CCBGases.SPORE_AIR.get();
        }

        if (biome.is(Biomes.DEEP_DARK)) {
            return CCBGases.SCULK_AIR.get();
        }

        DimensionType dimensionType = level.dimensionType();
        if (dimensionType.ultraWarm()) {
            return CCBGases.ULTRAWARM_AIR.get();
        }

        if (dimensionType.natural()) {
            return CCBGases.NATURAL_AIR.get();
        }

        return CCBGases.ETHEREAL_AIR.get();
    }

    @Override
    public OptionalLong resolvePressurePa(Level level, BlockPos pos) {
        float atmospheres;
        if (Level.NETHER == level.dimension()) {
            atmospheres = CCBConfig.server().gas.atmosphere.netherPressure.getF();
        }
        else if (Level.END == level.dimension()) {
            atmospheres = CCBConfig.server().gas.atmosphere.endPressure.getF();
        }
        else {
            atmospheres = CCBConfig.server().gas.atmosphere.overworldPressure.getF();
        }
        return OptionalLong.of(GasPressure.pascals(atmospheres));
    }
}
