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
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrillHandlerEvent;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightDrillHandlers {
    public static void register() {
        AirtightDrillHandlerEvent.add(CCBGases.NATURAL_AIR.get(), new NaturalAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirDrillHandler());

        AirtightDrillHandlerEvent.add(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirDrillHandler());

        AirtightDrillHandlerEvent.add(CCBGases.ETHEREAL_AIR.get(), new EtherealAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirDrillHandler());

        AirtightDrillHandlerEvent.add(CCBGases.MOIST_AIR.get(), new MoistAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.SPORE_AIR.get(), new SporeAirDrillHandler());
        AirtightDrillHandlerEvent.add(CCBGases.SCULK_AIR.get(), new SculkAirDrillHandler());

        AirtightDrillHandlerEvent.add(CCBGases.CREATIVE_AIR.get(), new CreativeDrillHandler());
    }
}
