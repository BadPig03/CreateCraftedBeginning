package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface AirtightThermoregulatorHandler {
    int SUPERHEATED = 3;
    int HEATED = 1;
    int NONE = 0;
    int CHILLED = -1;
    int SUPERCHILLED = -3;

    SimpleRegistry<Block, AirtightThermoregulatorHandler> REGISTRY = SimpleRegistry.create();

    float getHeat(Level level, BlockPos pos, BlockState state);
}
