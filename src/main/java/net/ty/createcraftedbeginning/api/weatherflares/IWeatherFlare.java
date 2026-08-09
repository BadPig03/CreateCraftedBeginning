package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface IWeatherFlare {
    int DEFAULT_DURATION = 24000;

    /**
     * Applies this weather flare's weather effect to the supplied server level.
     *
     * @param level the server level whose weather should be changed
     * @param ratio the normalized effect ratio used to scale the weather change
     */
    void setWeather(ServerLevel level, double ratio);
}
