package net.ty.createcraftedbeginning.compat.kubejs.events;

import dev.latvian.mods.kubejs.event.KubeEvent;
import dev.latvian.mods.rhino.Context;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareContext;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareEffect;
import net.ty.createcraftedbeginning.api.weatherflares.WeatherFlareHandlers;
import net.ty.createcraftedbeginning.compat.kubejs.KubeJSWeatherFlareHandler;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WeatherFlareHandlerEvent implements KubeEvent {
    public void add(Context context, ResourceLocation dimension, @Nullable Object options) {
        try {
            WeatherFlareHandlers.register(ResourceKey.create(Registries.DIMENSION, dimension), new KubeJSWeatherFlareHandler(context, dimension, options));
        }
        catch (IllegalArgumentException | IllegalStateException failure) {
            CCBAPI.LOGGER.error("Failed to register weather flare handler for dimension '{}': {}", dimension, failure.getMessage());
        }
    }

    @FunctionalInterface
    public interface CanApply {
        @Nullable
        Object test(ServerLevel level, Vec3 pos, WeatherFlareEffect effect);
    }

    @FunctionalInterface
    public interface Capture {
        @Nullable
        Object capture(WeatherFlareContext context);
    }

    @FunctionalInterface
    public interface Apply {
        @Nullable
        Object apply(WeatherFlareContext context, @Nullable Object snapshot);
    }

    @FunctionalInterface
    public interface Restore {
        void restore(WeatherFlareContext context, @Nullable Object snapshot);
    }
}
