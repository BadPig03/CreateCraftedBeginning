package net.ty.createcraftedbeginning.api.thermoregulatorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class DefaultThermoregulatorHandler implements AirtightThermoregulatorHandler {
    @Override
    public float getHeat(Level level, BlockPos pos, BlockState state) {
        return 0;
    }
}
