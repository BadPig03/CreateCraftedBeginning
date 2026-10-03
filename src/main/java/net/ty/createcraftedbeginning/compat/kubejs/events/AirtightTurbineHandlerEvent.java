package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.turbinehandlers.AirtightTurbineHandlers;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightTurbineHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, float baseLevel, float maxLevel) {
        AirtightTurbineHandlers.register(location, baseLevel, maxLevel);
    }
}
