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
import net.ty.createcraftedbeginning.data.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightArmorsHandlers {
    /**
     * Registers the built-in airtight armors handlers.
     */
    public static void register() {
        AirtightArmorsHandlerUtils.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), new NaturalAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new EnergizedNaturalAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.PRESSURIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedNaturalAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get().getResourceLocation(), new PressurizedEnergizedNaturalAirArmorsHandler());

        AirtightArmorsHandlerUtils.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), new UltrawarmAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new EnergizedUltrawarmAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedUltrawarmAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), new PressurizedEnergizedUltrawarmAirArmorsHandler());

        AirtightArmorsHandlerUtils.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), new EtherealAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new EnergizedEtherealAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEtherealAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), new PressurizedEnergizedEtherealAirArmorsHandler());

        AirtightArmorsHandlerUtils.register(CCBGases.MOIST_AIR.get().getResourceLocation(), new MoistAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.SPORE_AIR.get().getResourceLocation(), new SporeAirArmorsHandler());
        AirtightArmorsHandlerUtils.register(CCBGases.SCULK_AIR.get().getResourceLocation(), new SculkAirArmorsHandler());

        AirtightArmorsHandlerUtils.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), new CreativeAirArmorsHandler());
    }
}
