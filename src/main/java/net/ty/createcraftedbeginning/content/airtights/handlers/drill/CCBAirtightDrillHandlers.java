package net.ty.createcraftedbeginning.content.airtights.handlers.drill;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.creative.CreativeDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ethereal.EnergizedEtherealAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ethereal.EtherealAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.moist.MoistAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural.EnergizedNaturalAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.natural.NaturalAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.sculk.SculkAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.spore.SporeAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.steam.SteamAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm.EnergizedUltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.drill.ultrawarm.UltrawarmAirDrillHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightDrillHandlers {
    public static void register() {
        AirtightDrillHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new NaturalAirDrillHandler());
        AirtightDrillHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedNaturalAirDrillHandler());

        AirtightDrillHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new UltrawarmAirDrillHandler());
        AirtightDrillHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedUltrawarmAirDrillHandler());

        AirtightDrillHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EtherealAirDrillHandler());
        AirtightDrillHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedEtherealAirDrillHandler());

        AirtightDrillHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new MoistAirDrillHandler());
        AirtightDrillHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SporeAirDrillHandler());
        AirtightDrillHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SculkAirDrillHandler());

        AirtightDrillHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SteamAirDrillHandler());

        AirtightDrillHandlers.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new CreativeDrillHandler());
    }
}
