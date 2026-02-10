package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.AirtightArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.creative.CreativeAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal.EnergizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal.EtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal.PressurizedEnergizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ethereal.PressurizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.moist.MoistAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.natural.EnergizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.natural.NaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.natural.PressurizedEnergizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.natural.PressurizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.sculk.SculkAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.spore.SporeAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm.EnergizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm.PressurizedEnergizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm.PressurizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.armorhandlers.ultrawarm.UltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.data.CCBGases;

public class CCBAirtightArmorsHandlers {
    public static void register() {
        SimpleRegistry<Gas, AirtightArmorsHandler> registry = AirtightArmorsHandler.REGISTRY;

        registry.register(CCBGases.NATURAL_AIR.get(), new NaturalAirArmorsHandler());
        registry.register(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirArmorsHandler());
        registry.register(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirArmorsHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirArmorsHandler());

        registry.register(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirArmorsHandler());
        registry.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirArmorsHandler());
        registry.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirArmorsHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirArmorsHandler());

        registry.register(CCBGases.ETHEREAL_AIR.get(), new EtherealAirArmorsHandler());
        registry.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirArmorsHandler());
        registry.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirArmorsHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirArmorsHandler());

        registry.register(CCBGases.MOIST_AIR.get(), new MoistAirArmorsHandler());
        registry.register(CCBGases.SPORE_AIR.get(), new SporeAirArmorsHandler());
        registry.register(CCBGases.SCULK_AIR.get(), new SculkAirArmorsHandler());

        registry.register(CCBGases.CREATIVE_AIR.get(), new CreativeAirArmorsHandler());
    }
}
