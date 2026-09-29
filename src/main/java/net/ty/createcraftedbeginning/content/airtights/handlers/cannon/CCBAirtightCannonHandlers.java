package net.ty.createcraftedbeginning.content.airtights.handlers.cannon;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.cannonhandlers.AirtightCannonHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.creative.CreativeAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal.EnergizedEtherealAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ethereal.EtherealAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.moist.MoistAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural.EnergizedNaturalAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.natural.NaturalAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.sculk.SculkAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.spore.SporeAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.steam.SteamAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm.EnergizedUltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.cannon.ultrawarm.UltrawarmAirCannonHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBAirtightCannonHandlers {
    public static void register() {
        AirtightCannonHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new NaturalAirCannonHandler());
        AirtightCannonHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedNaturalAirCannonHandler());

        AirtightCannonHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new UltrawarmAirCannonHandler());
        AirtightCannonHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedUltrawarmAirCannonHandler());

        AirtightCannonHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EtherealAirCannonHandler());
        AirtightCannonHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedEtherealAirCannonHandler());

        AirtightCannonHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new MoistAirCannonHandler());
        AirtightCannonHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SporeAirCannonHandler());
        AirtightCannonHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SculkAirCannonHandler());

        AirtightCannonHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SteamAirCannonHandler());

        AirtightCannonHandlers.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new CreativeAirCannonHandler());
    }
}
