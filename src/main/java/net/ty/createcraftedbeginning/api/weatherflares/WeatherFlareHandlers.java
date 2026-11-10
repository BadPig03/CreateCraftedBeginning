package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class WeatherFlareHandlers {
    private static final Map<ResourceKey<Level>, WeatherFlareHandler> HANDLERS = new HashMap<>();

    private WeatherFlareHandlers() {
    }

    public static void register(ResourceKey<Level> dimension, WeatherFlareHandler handler) {
        Objects.requireNonNull(dimension, "Weather flare dimension must not be null.");
        Objects.requireNonNull(handler, "Weather flare handler must not be null.");
        if (HANDLERS.putIfAbsent(dimension, handler) != null) {
            throw new IllegalStateException("Weather flare handler already registered for dimension '" + dimension.location() + "'.");
        }
    }

    @Nullable
    public static WeatherFlareHandler resolve(ResourceKey<Level> dimension) {
        WeatherFlareHandler handler = HANDLERS.get(dimension);
        if (handler != null) {
            return handler;
        }

        if (dimension == Level.OVERWORLD) {
            return VanillaWeatherFlareHandler.INSTANCE;
        }

        return null;
    }
}
