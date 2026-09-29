package net.ty.createcraftedbeginning.content.airtights.handlers.release;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseHandlers;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.creative.CreativeAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ethereal.EnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ethereal.EtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ethereal.HighPressureEnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ethereal.HighPressureEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.moist.HighPressureMoistAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.moist.MoistAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.natural.EnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.natural.HighPressureEnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.natural.HighPressureNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.natural.NaturalAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.sculk.HighPressureSculkAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.sculk.SculkAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.spore.HighPressureSporeAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.spore.SporeAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.steam.HighPressureSteamEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.steam.SteamEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ultrawarm.EnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ultrawarm.HighPressureEnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ultrawarm.HighPressureUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.content.airtights.handlers.release.ultrawarm.UltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.registry.gas.CCBGases;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class CCBGasReleaseHandlers {
    public static void register() {
        GasReleaseHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new NaturalAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureNaturalAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedNaturalAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ENERGIZED_NATURAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureEnergizedNaturalAirEffectHandler());

        GasReleaseHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new UltrawarmAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureUltrawarmAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedUltrawarmAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureEnergizedUltrawarmAirEffectHandler());

        GasReleaseHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EtherealAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureEtherealAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new EnergizedEtherealAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureEnergizedEtherealAirEffectHandler());

        GasReleaseHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new MoistAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.MOIST_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureMoistAirEffectHandler());

        GasReleaseHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SporeAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.SPORE_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureSporeAirEffectHandler());

        GasReleaseHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SculkAirEffectHandler());
        GasReleaseHandlers.register(CCBGases.SCULK_AIR.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureSculkAirEffectHandler());

        GasReleaseHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new SteamEffectHandler());
        GasReleaseHandlers.register(CCBGases.STEAM.get().getResourceLocation(), GameplayPressureProfiles.HIGH_PRESSURE, new HighPressureSteamEffectHandler());

        GasReleaseHandlers.register(CCBGases.CREATIVE_AIR.get().getResourceLocation(), GameplayPressureProfiles.NORMAL, new CreativeAirEffectHandler());
    }
}
