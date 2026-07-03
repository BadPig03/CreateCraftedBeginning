package net.ty.createcraftedbeginning.api.fillhandlers;

import com.simibubi.create.api.registry.SimpleRegistry.Provider;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.fillhandlers.contents.AirFillHandler;
import net.ty.createcraftedbeginning.api.fillhandlers.contents.BubbleColumnFillHandler;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightFillHandlers {
    public static void register() {
        AirtightFillHandlerUtils.register(Blocks.BUBBLE_COLUMN, new BubbleColumnFillHandler());

        AirtightFillHandler.REGISTRY.registerProvider(Provider.forBlockTag(BlockTags.AIR, new AirFillHandler()));
    }
}
