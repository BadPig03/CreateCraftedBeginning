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
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightDrainageHandlers {
    /**
     * Registers the built-in airtight drainage handlers.
     */
    public static void register() {
        AirtightDrainageHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), new NaturalAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new EnergizedNaturalAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedNaturalAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedEnergizedNaturalAirEffectHandler());

        AirtightDrainageHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), new UltrawarmAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new EnergizedUltrawarmAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedUltrawarmAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedEnergizedUltrawarmAirEffectHandler());

        AirtightDrainageHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), new EtherealAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new EnergizedEtherealAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEtherealAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEnergizedEtherealAirEffectHandler());

        AirtightDrainageHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), new MoistAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), new SporeAirEffectHandler());
        AirtightDrainageHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), new SculkAirEffectHandler());

        AirtightDrainageHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), new CreativeAirEffectHandler());
    }
}
