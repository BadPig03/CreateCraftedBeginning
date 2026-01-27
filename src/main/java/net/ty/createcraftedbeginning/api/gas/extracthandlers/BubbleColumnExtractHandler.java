package net.ty.createcraftedbeginning.api.gas.extracthandlers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeExtractHandler;
import net.ty.createcraftedbeginning.data.CCBGases;

public class BubbleColumnExtractHandler implements GasOpenPipeExtractHandler {
    @Override
    public Gas extract(Level level, BlockPos pos, BlockState state) {
        return CCBGases.MOIST_AIR.get();
    }
}
