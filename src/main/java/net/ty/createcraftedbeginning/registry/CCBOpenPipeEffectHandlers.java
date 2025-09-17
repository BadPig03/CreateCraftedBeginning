package net.ty.createcraftedbeginning.registry;

import net.ty.createcraftedbeginning.api.gas.effecthandlers.NaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.UltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.interfaces.GasOpenPipeEffectHandler;

public class CCBOpenPipeEffectHandlers {
    public static void register() {
        GasOpenPipeEffectHandler.REGISTRY.register(CCBGases.NATURAL_AIR.get(), new NaturalAirEffectHandler());
        GasOpenPipeEffectHandler.REGISTRY.register(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirEffectHandler());
    }
}
