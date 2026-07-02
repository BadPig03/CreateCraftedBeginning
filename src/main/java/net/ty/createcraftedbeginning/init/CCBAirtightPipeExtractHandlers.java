package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import com.simibubi.create.api.registry.SimpleRegistry.Provider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.gas.extracthandlers.AirExtractHandler;
import net.ty.createcraftedbeginning.api.gas.extracthandlers.BubbleColumnExtractHandler;
import net.ty.createcraftedbeginning.api.gas.extracthandlers.AirtightPipeExtractHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightPipeExtractHandlers {
    public static void register() {
        SimpleRegistry<Block, AirtightPipeExtractHandler> registry = AirtightPipeExtractHandler.REGISTRY;

        registry.registerProvider(Provider.forBlockTag(BlockTags.AIR, new AirExtractHandler()));
        registry.register(Blocks.BUBBLE_COLUMN, new BubbleColumnExtractHandler());
    }
}
