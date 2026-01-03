package net.ty.createcraftedbeginning.init;

import com.simibubi.create.api.registry.SimpleRegistry;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.NaturalAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.effecthandlers.UltrawarmAirEffectHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasOpenPipeEffectHandler;
import net.ty.createcraftedbeginning.data.CCBGases;

public class CCBOpenPipeEffectHandlers {
    public static void register() {
        SimpleRegistry<Gas, GasOpenPipeEffectHandler> registry = GasOpenPipeEffectHandler.REGISTRY;

        registry.register(CCBGases.NATURAL_AIR.get(), new NaturalAirEffectHandler());
        registry.register(CCBGases.ULTRAWARM_AIR.get(), new UltrawarmAirEffectHandler());
    }
}
