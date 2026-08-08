package net.ty.createcraftedbeginning.content.airtights.handlers.cannon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.creative.CreativeAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal.EnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal.EtherealAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal.PressurizedEnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal.PressurizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.moist.MoistAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural.EnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural.NaturalAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural.PressurizedEnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural.PressurizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.sculk.SculkAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.spore.SporeAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm.EnergizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm.PressurizedEnergizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm.PressurizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm.UltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightCannonHandlers {
    /**
     * Registers the built-in airtight cannon handlers.
     */
    public static void register() {
        AirtightCannonHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), new NaturalAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new EnergizedNaturalAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedNaturalAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedEnergizedNaturalAirCannonHandler());

        AirtightCannonHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), new UltrawarmAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new EnergizedUltrawarmAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedUltrawarmAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedEnergizedUltrawarmAirCannonHandler());

        AirtightCannonHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), new EtherealAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new EnergizedEtherealAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEtherealAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEnergizedEtherealAirCannonHandler());

        AirtightCannonHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), new MoistAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), new SporeAirCannonHandler());
        AirtightCannonHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), new SculkAirCannonHandler());

        AirtightCannonHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), new CreativeAirCannonHandler());
    }
}
