package net.ty.createcraftedbeginning.api.drillhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.creative.CreativeDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ethereal.EnergizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ethereal.EtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ethereal.PressurizedEnergizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ethereal.PressurizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.moist.MoistAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.natural.EnergizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.natural.NaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.natural.PressurizedEnergizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.natural.PressurizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.sculk.SculkAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.spore.SporeAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ultrawarm.EnergizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ultrawarm.PressurizedEnergizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ultrawarm.PressurizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.api.drillhandlers.ultrawarm.UltrawarmAirDrillHandler;
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
