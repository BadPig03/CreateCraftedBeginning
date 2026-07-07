package net.ty.createcraftedbeginning.api.coolantshandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Blocks;
import net.ty.createcraftedbeginning.api.coolantshandlers.contents.BlueIceCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.contents.BreezeCoolerCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.contents.IceCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.contents.PackedIceCoolantHandler;
import net.ty.createcraftedbeginning.api.coolantshandlers.contents.PowderSnowCoolantHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightCoolantHandlerEvent;
import net.ty.createcraftedbeginning.registry.CCBBlocks;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightCoolantHandlers {
    public static void register() {
        AirtightCoolantHandlerEvent.add(Blocks.ICE, new IceCoolantHandler());
        AirtightCoolantHandlerEvent.add(Blocks.FROSTED_ICE, new IceCoolantHandler());
        AirtightCoolantHandlerEvent.add(Blocks.PACKED_ICE, new PackedIceCoolantHandler());
        AirtightCoolantHandlerEvent.add(Blocks.BLUE_ICE, new BlueIceCoolantHandler());
        AirtightCoolantHandlerEvent.add(Blocks.POWDER_SNOW, new PowderSnowCoolantHandler());
        AirtightCoolantHandlerEvent.add(CCBBlocks.BREEZE_COOLER_BLOCK.get(), new BreezeCoolerCoolantHandler());
    }
}