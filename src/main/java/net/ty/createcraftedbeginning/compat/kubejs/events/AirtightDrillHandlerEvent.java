package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.drillhandlers.AirtightDrillHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightDrillHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, int damage, float consumption) {
        AirtightDrillHandlers.register(location, GameplayPressureProfiles.require(profileId), damage, consumption);
    }
}
