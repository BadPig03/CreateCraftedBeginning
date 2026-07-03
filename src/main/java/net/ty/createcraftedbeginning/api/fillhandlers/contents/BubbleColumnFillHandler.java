package net.ty.createcraftedbeginning.api.fillhandlers.contents;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BubbleColumnFillHandler implements AirtightFillHandler {
    @Override
    public Gas apply(Level level, BlockPos pos, BlockState state) {
        return CCBGases.MOIST_AIR.get();
    }
}
