package net.ty.createcraftedbeginning.api.drainagehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultDrainageHandler implements AirtightDrainageHandler {
    @Override
    public float getInflation() {
        return 1;
    }

    @Override
    public void apply(Level level, BlockPos pos, Direction direction, Gas gasType) {
        showOutline(level, pos, direction, getInflation(), gasType.getTint());
    }
}
