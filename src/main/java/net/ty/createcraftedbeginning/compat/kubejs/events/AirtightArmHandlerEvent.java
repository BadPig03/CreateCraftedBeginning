package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.armhandlers.AirtightArmHandlers;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSToolHandlerPatches;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AirtightArmHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, float consumption, float blockRange, float entityRange, float knockback) {
        AirtightArmHandlers.register(location, GameplayPressureProfiles.require(profileId), consumption, blockRange, entityRange, knockback);
    }

    public void modify(ResourceLocation location, ResourceLocation profileId, @Nullable Object changes) {
        try {
            AirtightArmHandlers.modify(location, GameplayPressureProfiles.require(profileId), KubeJSToolHandlerPatches.readArm(changes));
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to modify airtight arm handler for gas '{}' and gameplay pressure profile '{}': {}", location, profileId, failure.getMessage());
        }
    }
}
