package net.ty.createcraftedbeginning.api.thermoregulatorhandlers.contents;

import com.simibubi.create.content.processing.burner.BlazeBurnerBlock;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BlazeBurnerThermoregulatorHandler implements AirtightThermoregulatorHandler {
    @Override
    public float getHeat(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(BlazeBurnerBlock.HEAT_LEVEL)) {
            return NONE;
        }

        HeatLevel heatLevel = state.getValue(BlazeBurnerBlock.HEAT_LEVEL);
        return switch (heatLevel) {
            case NONE, SMOULDERING -> NONE;
            case FADING, KINDLED -> HEATED;
            case SEETHING -> SUPERHEATED;
        };
    }
}
