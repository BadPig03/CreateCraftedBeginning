package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ethereal.EnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.natural.EnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.sculk.SculkAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ultrawarm.EnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ethereal.EtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.moist.MoistAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.natural.NaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ethereal.PressurizedEnergizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.natural.PressurizedEnergizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ultrawarm.PressurizedEnergizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ethereal.PressurizedEtherealAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.natural.PressurizedNaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ultrawarm.PressurizedUltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.spore.SporeAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.ultrawarm.UltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.creative.CreativeAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeEffectHandler;
import net.ty.createcraftedbeginning.data.CCBGases;

public class CCBOpenPipeEffectHandlers {
    public static void register() {
        SimpleRegistry<Gas, GasOpenPipeEffectHandler> registry = GasOpenPipeEffectHandler.REGISTRY;

        registry.register(CCBGases.NATURAL_AIR.get(), new NaturalAirEffectHandler());
        registry.register(CCBGases.ENERGIZED_NATURAL_AIR.get(), new EnergizedNaturalAirEffectHandler());
        registry.register(CCBGases.PRESSURIZED_NATURAL_AIR.get(), new PressurizedNaturalAirEffectHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_NATURAL_AIR.get(), new PressurizedEnergizedNaturalAirEffectHandler());

        registry.register(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirEffectHandler());
        registry.register(CCBGases.ENERGIZED_ULTRAWARM_AIR.get(), new EnergizedUltrawarmAirEffectHandler());
        registry.register(CCBGases.PRESSURIZED_ULTRAWARM_AIR.get(), new PressurizedUltrawarmAirEffectHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ULTRAWARM_AIR.get(), new PressurizedEnergizedUltrawarmAirEffectHandler());

        registry.register(CCBGases.ETHEREAL_AIR.get(), new EtherealAirEffectHandler());
        registry.register(CCBGases.ENERGIZED_ETHEREAL_AIR.get(), new EnergizedEtherealAirEffectHandler());
        registry.register(CCBGases.PRESSURIZED_ETHEREAL_AIR.get(), new PressurizedEtherealAirEffectHandler());
        registry.register(CCBGases.PRESSURIZED_ENERGIZED_ETHEREAL_AIR.get(), new PressurizedEnergizedEtherealAirEffectHandler());

        registry.register(CCBGases.MOIST_AIR.get(), new MoistAirEffectHandler());
        registry.register(CCBGases.SPORE_AIR.get(), new SporeAirEffectHandler());
        registry.register(CCBGases.SCULK_AIR.get(), new SculkAirEffectHandler());

        registry.register(CCBGases.CREATIVE_AIR.get(), new CreativeAirEffectHandler());
    }
}
