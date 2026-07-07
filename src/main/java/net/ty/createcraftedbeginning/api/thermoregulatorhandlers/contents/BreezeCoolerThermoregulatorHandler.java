package net.ty.createcraftedbeginning.api.thermoregulatorhandlers.contents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock;
import net.ty.createcraftedbeginning.content.breezes.breezecooler.BreezeCoolerBlock.FrostLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BreezeCoolerThermoregulatorHandler implements AirtightThermoregulatorHandler {
    @Override
    public float getHeat(Level level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(BreezeCoolerBlock.FROST_LEVEL)) {
            return NONE;
        }

        FrostLevel frostLevel = state.getValue(BreezeCoolerBlock.FROST_LEVEL);
        return switch (frostLevel) {
            case RIMING -> NONE;
            case CHILLED -> CHILLED;
        };
    }
}
