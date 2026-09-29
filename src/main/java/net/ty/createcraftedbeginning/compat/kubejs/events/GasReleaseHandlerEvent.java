package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.ty.createcraftedbeginning.api.gas.pressure.GameplayPressureProfiles;
import net.ty.createcraftedbeginning.api.gasreleasehandlers.GasReleaseContext;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSHandlerAdapters;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasReleaseHandlerEvent implements KubeEvent {
    public void add(ResourceLocation location, ResourceLocation profileId, boolean shouldOutline, ReleaseHandler handler) {
        KubeJSHandlerAdapters.registerGasRelease(location, GameplayPressureProfiles.require(profileId), shouldOutline, handler);
    }

    @FunctionalInterface
    public interface ReleaseHandler {
        void apply(GasReleaseContext context);
    }
}
