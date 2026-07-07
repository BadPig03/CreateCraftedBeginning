package net.ty.createcraftedbeginning.api.armorhandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.armorhandlers.creative.CreativeAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ethereal.EnergizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ethereal.EtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ethereal.PressurizedEnergizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ethereal.PressurizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.moist.MoistAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.natural.EnergizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.natural.NaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.natural.PressurizedEnergizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.natural.PressurizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.sculk.SculkAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.spore.SporeAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ultrawarm.EnergizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ultrawarm.PressurizedEnergizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ultrawarm.PressurizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.api.armorhandlers.ultrawarm.UltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightArmorsHandlerEvent;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightArmorsHandlers {
    public static void register() {
        AirtightArmorsHandlerEvent.add(CCBGases.NATURAL_AIR.get(), new NaturalAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirArmorsHandler());

        AirtightArmorsHandlerEvent.add(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirArmorsHandler());

        AirtightArmorsHandlerEvent.add(CCBGases.ETHEREAL_AIR.get(), new EtherealAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirArmorsHandler());

        AirtightArmorsHandlerEvent.add(CCBGases.MOIST_AIR.get(), new MoistAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.SPORE_AIR.get(), new SporeAirArmorsHandler());
        AirtightArmorsHandlerEvent.add(CCBGases.SCULK_AIR.get(), new SculkAirArmorsHandler());

        AirtightArmorsHandlerEvent.add(CCBGases.CREATIVE_AIR.get(), new CreativeAirArmorsHandler());
    }
}
