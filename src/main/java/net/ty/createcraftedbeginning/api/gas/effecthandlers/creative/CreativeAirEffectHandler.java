package net.ty.createcraftedbeginning.api.gas.effecthandlers.creative;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeEffectHandler;
import org.jetbrains.annotations.NotNull;

public class CreativeAirEffectHandler implements GasOpenPipeEffectHandler {
    @Override
    public void apply(Level level, BlockPos pos, Direction direction, @NotNull Gas gasType) {
        applyEffects(level, pos, direction, gasType.getInflation(), gasType.getTint());
    }

    protected void applyEffects(@NotNull Level level, BlockPos pos, Direction direction, float inflation, int color) {
        showOutline(level, pos, direction, inflation, color);
    }
}
