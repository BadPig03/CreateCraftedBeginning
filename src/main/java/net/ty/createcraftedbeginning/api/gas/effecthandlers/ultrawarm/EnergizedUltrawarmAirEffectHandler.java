package net.ty.createcraftedbeginning.api.gas.effecthandlers.ultrawarm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

public class EnergizedUltrawarmAirEffectHandler extends UltrawarmAirEffectHandler {
    @Override
    public void apply(@NotNull Level level, @NotNull BlockPos pos, Direction direction, @NotNull Gas gasType) {
        applyEffects(level, pos, direction, gasType.getInflation(), gasType.getTint(), 5);
    }
}
