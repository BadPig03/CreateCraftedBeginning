package net.ty.createcraftedbeginning.content.airtights.handlers.drainage;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drainagehandlers.AirtightDrainageHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.creative.CreativeAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ethereal.EnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ethereal.EtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ethereal.PressurizedEnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ethereal.PressurizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.moist.MoistAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.natural.EnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.natural.NaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.natural.PressurizedEnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.natural.PressurizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.sculk.SculkAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.spore.SporeAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ultrawarm.EnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ultrawarm.PressurizedEnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ultrawarm.PressurizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drainage.ultrawarm.UltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightDrainageHandlers {
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
