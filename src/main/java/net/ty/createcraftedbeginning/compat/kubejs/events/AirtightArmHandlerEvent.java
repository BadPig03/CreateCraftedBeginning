package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, float consumption, float blockRange, float entityRange, float knockback) {
        AirtightArmHandlers.register(location, GameplayPressureProfiles.require(profileId), consumption, blockRange, entityRange, knockback);
    }
}
