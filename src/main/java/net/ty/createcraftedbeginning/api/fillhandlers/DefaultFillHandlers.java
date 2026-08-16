package net.ty.createcraftedbeginning.api.fillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class DefaultFillHandlers implements AirtightFillHandler {
    @Override
    public Gas apply(Level level, BlockPos pos, BlockState state) {
        return Gas.EMPTY_GAS_HOLDER.value();
    }
}
