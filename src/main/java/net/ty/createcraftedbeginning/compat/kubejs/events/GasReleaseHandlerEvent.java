package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSHandlerAdapters;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasReleaseHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, boolean shouldOutline, ReleaseHandler handler) {
        KubeJSHandlerAdapters.registerGasRelease(location, GameplayPressureProfiles.require(profileId), shouldOutline, handler);
    }

    public void add(Context context, ResourceLocation location, ResourceLocation profileId, @Nullable Object options) {
        try {
            KubeJSHandlerAdapters.registerGasRelease(context, location, GameplayPressureProfiles.require(profileId), options);
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to register configured gas release handler for gas '{}' and gameplay pressure profile '{}': {}", location, profileId, failure.getMessage());
        }
    }

    @FunctionalInterface
    public interface ReleaseHandler {
        void apply(GasReleaseContext context);
    }
}
