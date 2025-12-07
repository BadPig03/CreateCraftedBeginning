package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.AirtightHandheldDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.creative.CreativeDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal.EnergizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal.EtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal.PressurizedEnergizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ethereal.PressurizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.moist.MoistAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.natural.EnergizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.natural.NaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.natural.PressurizedEnergizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.natural.PressurizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ultrawarm.EnergizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ultrawarm.PressurizedEnergizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ultrawarm.PressurizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.api.gas.drillhandlers.ultrawarm.UltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.data.CCBGases;

public class CCBAirtightHandheldDrillHandlers {
    public static void register() {
        SimpleRegistry<Gas, AirtightHandheldDrillHandler> registry = AirtightHandheldDrillHandler.REGISTRY;

        registry.register(CCBGases.NATURAL_AIR.get(), new NaturalAirDrillHandler());
        registry.register(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirDrillHandler());
        registry.register(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirDrillHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirDrillHandler());

        registry.register(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirDrillHandler());
        registry.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirDrillHandler());
        registry.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirDrillHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirDrillHandler());

        registry.register(CCBGases.ETHEREAL_AIR.get(), new EtherealAirDrillHandler());
        registry.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirDrillHandler());
        registry.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirDrillHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirDrillHandler());

        registry.register(CCBGases.MOIST_AIR.get(), new MoistAirDrillHandler());
        registry.register(CCBGases.CREATIVE_AIR.get(), new CreativeDrillHandler());
    }
}
