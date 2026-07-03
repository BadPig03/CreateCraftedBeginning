package net.ty.createcraftedbeginning.api.drainagehandlers.ultrawarm;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizedUltrawarmAirEffectHandler extends UltrawarmAirEffectHandler {
    @Override
    public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
        applyEffects(level, pos, direction, getInflation(), gasType.getTint(), 5);
    }
}
