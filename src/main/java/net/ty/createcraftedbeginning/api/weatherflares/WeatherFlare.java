package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface WeatherFlare {
    int DEFAULT_DURATION = 24000;

    void setWeather(ServerLevel level, double ratio);

    /** Applies this flare using a single-item copy of its fired stack, including data components. */
    default void setWeather(ServerLevel level, ItemStack flareStack, double ratio) {
        setWeather(level, ratio);
    }

    /** Opts into dimension handlers; null preserves the existing direct callback contract. */
    @Nullable
    default WeatherFlareEffect getWeatherEffect() {
        return null;
    }
}
