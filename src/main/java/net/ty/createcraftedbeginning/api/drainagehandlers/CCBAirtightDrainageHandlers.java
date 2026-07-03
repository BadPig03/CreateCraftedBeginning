package net.ty.createcraftedbeginning.api.drainagehandlers;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drainagehandlers.creative.CreativeAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ethereal.EnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ethereal.EtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ethereal.PressurizedEnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ethereal.PressurizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.moist.MoistAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.natural.EnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.natural.NaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.natural.PressurizedEnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.natural.PressurizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.sculk.SculkAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.spore.SporeAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ultrawarm.EnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ultrawarm.PressurizedEnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ultrawarm.PressurizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.drainagehandlers.ultrawarm.UltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.compat.kubejs.events.AirtightDrainageHandlerEvent;
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightDrainageHandlers {
    public static void register() {
        AirtightDrainageHandlerEvent.add(CCBGases.NATURAL_AIR.get(), new NaturalAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirEffectHandler());

        AirtightDrainageHandlerEvent.add(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirEffectHandler());

        AirtightDrainageHandlerEvent.add(CCBGases.ETHEREAL_AIR.get(), new EtherealAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirEffectHandler());

        AirtightDrainageHandlerEvent.add(CCBGases.MOIST_AIR.get(), new MoistAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.SPORE_AIR.get(), new SporeAirEffectHandler());
        AirtightDrainageHandlerEvent.add(CCBGases.SCULK_AIR.get(), new SculkAirEffectHandler());

        AirtightDrainageHandlerEvent.add(CCBGases.CREATIVE_AIR.get(), new CreativeAirEffectHandler());
    }
}
