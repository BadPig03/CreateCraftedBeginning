package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.spore.SporeAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.creative.CreativeAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ethereal.EnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.natural.EnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ultrawarm.EnergizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ethereal.EtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.moist.MoistAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.natural.NaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ethereal.PressurizedEnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.natural.PressurizedEnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ultrawarm.PressurizedEnergizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ethereal.PressurizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.natural.PressurizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ultrawarm.PressurizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.ultrawarm.UltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.api.gas.cannonhandlers.AirtightCannonHandler;
import net.ty.createcraftedbeginning.data.CCBGases;

public class CCBAirtightCannonHandlers {
    public static void register() {
        SimpleRegistry<Gas, AirtightCannonHandler> registry = AirtightCannonHandler.REGISTRY;

        registry.register(CCBGases.NATURAL_AIR.get(), new NaturalAirCannonHandler());
        registry.register(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirCannonHandler());
        registry.register(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirCannonHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirCannonHandler());

        registry.register(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirCannonHandler());
        registry.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirCannonHandler());
        registry.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirCannonHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirCannonHandler());

        registry.register(CCBGases.ETHEREAL_AIR.get(), new EtherealAirCannonHandler());
        registry.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirCannonHandler());
        registry.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirCannonHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirCannonHandler());

        registry.register(CCBGases.MOIST_AIR.get(), new MoistAirCannonHandler());
        registry.register(CCBGases.SPORE_AIR.get(), new SporeAirCannonHandler());
        registry.register(CCBGases.CREATIVE_AIR.get(), new CreativeAirCannonHandler());
    }
}
