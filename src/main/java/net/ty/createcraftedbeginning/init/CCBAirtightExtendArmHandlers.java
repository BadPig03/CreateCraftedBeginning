package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.ty.createcraftedbeginning.api.gas.armhandlers.AirtightExtendArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.creative.CreativeAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.ethereal.EnergizedEtherealAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.ethereal.EtherealAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.ethereal.PressurizedEnergizedEtherealAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.ethereal.PressurizedEtherealAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.moist.MoistAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.natural.EnergizedNaturalAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.natural.NaturalAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.natural.PressurizedEnergizedNaturalAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.natural.PressurizedNaturalAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.spore.SporeAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.ultrawarm.EnergizedUltrawarmAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.ultrawarm.PressurizedEnergizedUltrawarmAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.armhandlers.ultrawarm.PressurizedUltrawarmAirArmHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBGases;

public class CCBAirtightExtendArmHandlers {
    public static void register() {
        SimpleRegistry<Gas, AirtightExtendArmHandler> registry = AirtightExtendArmHandler.REGISTRY;

        registry.register(CCBGases.NATURAL_AIR.get(), new NaturalAirArmHandler());
        registry.register(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirArmHandler());
        registry.register(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirArmHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirArmHandler());

        registry.register(CCBGases.ULTRAWARM_AIR.get(), new NaturalAirArmHandler());
        registry.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirArmHandler());
        registry.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirArmHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirArmHandler());

        registry.register(CCBGases.ETHEREAL_AIR.get(), new EtherealAirArmHandler());
        registry.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirArmHandler());
        registry.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirArmHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirArmHandler());

        registry.register(CCBGases.MOIST_AIR.get(), new MoistAirArmHandler());
        registry.register(CCBGases.SPORE_AIR.get(), new SporeAirArmHandler());
        registry.register(CCBGases.CREATIVE_AIR.get(), new CreativeAirArmHandler());
    }
}
