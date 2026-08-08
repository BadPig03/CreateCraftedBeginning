package net.ty.createcraftedbeginning.content.airtights.handlers.coolant;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.coolantshandlers.AirtightCoolantHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.handlers.coolant.contents.BlueIceCoolantHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.coolant.contents.BreezeCoolerCoolantHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.coolant.contents.IceCoolantHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.coolant.contents.PackedIceCoolantHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.coolant.contents.PowderSnowCoolantHandler;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightCoolantHandlers {
    /**
     * Registers the built-in airtight coolant handlers.
     */
    public static void register() {
        AirtightCoolantHandlerUtils.register(Blocks.ICE, new IceCoolantHandler());
        AirtightCoolantHandlerUtils.register(Blocks.FROSTED_ICE, new IceCoolantHandler());
        AirtightCoolantHandlerUtils.register(Blocks.PACKED_ICE, new PackedIceCoolantHandler());
        AirtightCoolantHandlerUtils.register(Blocks.BLUE_ICE, new BlueIceCoolantHandler());
        AirtightCoolantHandlerUtils.register(Blocks.POWDER_SNOW, new PowderSnowCoolantHandler());
        AirtightCoolantHandlerUtils.register(CCBBlocks.BREEZE_COOLER_BLOCK.get(), new BreezeCoolerCoolantHandler());
    }
}