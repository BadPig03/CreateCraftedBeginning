package net.ty.createcraftedbeginning.api.gas.reactorkettle;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.init.CCBReactorKettleThermoregulators;

@SuppressWarnings("unused")
@FunctionalInterface
public interface ReactorKettleThermoregulator {
    int SUPERHEATED = 3;
    int HEATED = 1;
    int NONE = 0;
    int CHILLED = -1;
    int SUPERCHILLED = -3;

    SimpleRegistry<Block, ReactorKettleThermoregulator> REGISTRY = SimpleRegistry.create();

    ReactorKettleThermoregulator BLAZE_BURNER = CCBReactorKettleThermoregulators::blazeBurner;

    ReactorKettleThermoregulator BREEZE_COOLER = CCBReactorKettleThermoregulators::breezeCooler;

    float getHeat(Level level, BlockPos blockPos, BlockState state);
}
