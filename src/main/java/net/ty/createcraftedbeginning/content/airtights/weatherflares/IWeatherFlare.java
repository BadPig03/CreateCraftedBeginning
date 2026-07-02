package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@FunctionalInterface
public interface IWeatherFlare {
    int DEFAULT_DURATION = 24000;

    void setWeather(ServerLevel serverLevel, double radio);
}
