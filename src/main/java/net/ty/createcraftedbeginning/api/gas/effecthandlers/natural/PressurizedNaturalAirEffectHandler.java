package net.ty.createcraftedbeginning.api.gas.effecthandlers.natural;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import org.jetbrains.annotations.NotNull;

public class PressurizedNaturalAirEffectHandler extends NaturalAirEffectHandler {
    @Override
    public void apply(Level level, BlockPos pos, Direction direction, @NotNull Gas gasType) {
        applyEffects(level, pos, direction, gasType.getInflation(), gasType.getTint(), 10);
    }
}
