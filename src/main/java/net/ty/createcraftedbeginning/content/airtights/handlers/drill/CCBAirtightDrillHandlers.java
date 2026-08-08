package net.ty.createcraftedbeginning.content.airtights.handlers.drill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.creative.CreativeDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ethereal.EnergizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ethereal.EtherealAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ethereal.PressurizedEnergizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ethereal.PressurizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.moist.MoistAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural.EnergizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural.NaturalAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural.PressurizedEnergizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural.PressurizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.sculk.SculkAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.spore.SporeAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm.EnergizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm.PressurizedEnergizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm.PressurizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm.UltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightDrillHandlers {
    /**
     * Registers the built-in airtight drill handlers.
     */
    public static void register() {
        AirtightDrillHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), new NaturalAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new EnergizedNaturalAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedNaturalAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedEnergizedNaturalAirDrillHandler());

        AirtightDrillHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), new UltrawarmAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new EnergizedUltrawarmAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedUltrawarmAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedEnergizedUltrawarmAirDrillHandler());

        AirtightDrillHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), new EtherealAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new EnergizedEtherealAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEtherealAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEnergizedEtherealAirDrillHandler());

        AirtightDrillHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), new MoistAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), new SporeAirDrillHandler());
        AirtightDrillHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), new SculkAirDrillHandler());

        AirtightDrillHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), new CreativeDrillHandler());
    }
}
