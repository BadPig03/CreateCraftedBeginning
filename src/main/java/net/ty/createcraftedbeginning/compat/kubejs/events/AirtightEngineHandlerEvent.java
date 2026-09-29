package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.enginehandlers.AirtightEngineHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightEngineHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, double workFactor) {
        AirtightEngineHandlers.register(location, GameplayPressureProfiles.require(profileId), workFactor);
    }

    public void add(ResourceLocation location, ResourceLocation profileId, double workFactor, int maxLevel) {
        AirtightEngineHandlers.register(location, GameplayPressureProfiles.require(profileId), workFactor, maxLevel);
    }
}
