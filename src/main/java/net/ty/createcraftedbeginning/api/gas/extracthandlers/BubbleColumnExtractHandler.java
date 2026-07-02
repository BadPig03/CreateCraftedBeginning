package net.ty.createcraftedbeginning.api.gas.extracthandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class BubbleColumnExtractHandler implements AirtightPipeExtractHandler {
    @Override
    public Gas extract(Level level, BlockPos pos, BlockState state) {
        return CCBGases.MOIST_AIR.get();
    }
}
