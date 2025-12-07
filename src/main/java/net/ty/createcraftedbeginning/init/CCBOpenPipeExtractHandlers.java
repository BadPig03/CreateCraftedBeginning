package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.minecraft.world.level.block.Block;
import net.ty.createcraftedbeginning.api.gas.extracthandlers.AirtightPipeExtractHandler;
import net.ty.createcraftedbeginning.api.gas.interfaces.GasOpenPipeExtractHandler;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

public class CCBOpenPipeExtractHandlers {
    public static void register() {
        SimpleRegistry<Block, GasOpenPipeExtractHandler> registry = GasOpenPipeExtractHandler.REGISTRY;

        registry.register(CCBBlocks.AIRTIGHT_PIPE_BLOCK.get(), new AirtightPipeExtractHandler());
    }
}
