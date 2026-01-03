package net.ty.createcraftedbeginning.init;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllTags.AllBlockTags;
import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.api.registry.SimpleRegistry.Provider;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.reactorkettle.ReactorKettleThermoregulator;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("unused")
public class CCBReactorKettleThermoregulators {
    public static void register() {
        SimpleRegistry<Block, ReactorKettleThermoregulator> registry = ReactorKettleThermoregulator.REGISTRY;

        registry.register(AllBlocks.BLAZE_BURNER.get(), ReactorKettleThermoregulator.BLAZE_BURNER);
        registry.register(CCBBlocks.BREEZE_COOLER_BLOCK.get(), ReactorKettleThermoregulator.BREEZE_COOLER);
        registry.registerProvider(Provider.forBlockTag(AllBlockTags.PASSIVE_BOILER_HEATERS.tag, (l, b, s) -> 0.11111111f));
    }

    public static int blazeBurner(Level level, BlockPos blockPos, @NotNull BlockState state) {
        if (!state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) {
            return ReactorKettleThermoregulator.NONE;
        }

        HeatLevel heatLevel = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
        return switch (heatLevel) {
            case NONE, SMOULDERING -> ReactorKettleThermoregulator.NONE;
            case FADING, KINDLED -> ReactorKettleThermoregulator.HEATED;
            case SEETHING -> ReactorKettleThermoregulator.SUPERHEATED;
        };
    }

    public static int breezeCooler(Level level, BlockPos blockPos, @NotNull BlockState state) {
        if (!state.hasProperty(BreezeCoolerBlock.FROST_LEVEL)) {
            return ReactorKettleThermoregulator.NONE;
        }

        FrostLevel frostLevel = state.getValue(BreezeCoolerBlock.FROST_LEVEL);
        return switch (frostLevel) {
            case RIMING -> ReactorKettleThermoregulator.NONE;
            case CHILLED -> ReactorKettleThermoregulator.CHILLED;
        };
    }
}
