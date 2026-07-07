package net.ty.createcraftedbeginning.api.thermoregulatorhandlers.contents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.thermoregulatorhandlers.AirtightThermoregulatorHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PassiveBoilerHeatersThermoregulatorHandler implements AirtightThermoregulatorHandler {
    @Override
    public float getHeat(Level level, BlockPos pos, BlockState state) {
        return 0.11111111f;
    }
}
