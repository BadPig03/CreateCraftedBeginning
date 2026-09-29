package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface WeatherFlare {
    int DEFAULT_DURATION = 24000;

    void setWeather(ServerLevel level, double ratio);
}
