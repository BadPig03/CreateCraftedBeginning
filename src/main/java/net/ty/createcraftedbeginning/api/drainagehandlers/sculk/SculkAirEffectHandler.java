package net.ty.createcraftedbeginning.api.drainagehandlers.sculk;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SculkAirEffectHandler implements AirtightDrainageHandler {
    @Override
    public float getInflation() {
        return 1;
    }

    @Override
    public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
        applyEffects(level, pos, direction, getInflation(), gasType.getTint());
    }

    protected void applyEffects(Level level, BlockPos pos, Direction direction, float inflation, int color) {
        showOutline(level, pos, direction, inflation, color);
    }
}
