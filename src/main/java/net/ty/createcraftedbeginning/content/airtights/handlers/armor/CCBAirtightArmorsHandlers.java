package net.ty.createcraftedbeginning.content.airtights.handlers.armor;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.armorhandlers.AirtightArmorsHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.creative.CreativeAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal.EnergizedEtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ethereal.EtherealAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.moist.MoistAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.natural.EnergizedNaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.natural.NaturalAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.sculk.SculkAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.spore.SporeAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.steam.SteamAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ultrawarm.EnergizedUltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.armor.ultrawarm.UltrawarmAirArmorsHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightArmorsHandlers {
    public static void register() {
        AirtightArmorsHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new NaturalAirArmorsHandler());
        AirtightArmorsHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedNaturalAirArmorsHandler());

        AirtightArmorsHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new UltrawarmAirArmorsHandler());
        AirtightArmorsHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedUltrawarmAirArmorsHandler());

        AirtightArmorsHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EtherealAirArmorsHandler());
        AirtightArmorsHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedEtherealAirArmorsHandler());

        AirtightArmorsHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new MoistAirArmorsHandler());
        AirtightArmorsHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SporeAirArmorsHandler());
        AirtightArmorsHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SculkAirArmorsHandler());

        AirtightArmorsHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SteamAirArmorsHandler());

        AirtightArmorsHandlers.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new CreativeAirArmorsHandler());
    }
}
