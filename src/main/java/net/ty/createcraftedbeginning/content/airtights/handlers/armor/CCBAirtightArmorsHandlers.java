package net.ty.createcraftedbeginning.content.airtights.handlers.armor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlerUtils;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.creative.CreativeAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal.EnergizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal.EtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal.PressurizedEnergizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal.PressurizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.moist.MoistAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.natural.EnergizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.natural.NaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.natural.PressurizedEnergizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.natural.PressurizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.sculk.SculkAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.spore.SporeAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ultrawarm.EnergizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ultrawarm.PressurizedEnergizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ultrawarm.PressurizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ultrawarm.UltrawarmAirArmorsHandler;
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
