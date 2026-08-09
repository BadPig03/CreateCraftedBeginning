package net.ty.createcraftedbeginning.content.airtights.handlers.fill;

import com.simibubi.create.api.registry.SimpleRegistry.Provider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.AirtightFillHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.handlers.fill.contents.AirFillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.fill.contents.BubbleColumnFillHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightFillHandlers {
    public static void register() {
        AirtightFillHandlerUtils.register(Blocks.BUBBLE_COLUMN, new BubbleColumnFillHandler());

        AirtightFillHandler.REGISTRY.registerProvider(Provider.forBlockTag(BlockTags.AIR, new AirFillHandler()));
    }
}
