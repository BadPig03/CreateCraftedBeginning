package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import net.minecraft.server.level.ServerLevel;

@FunctionalInterface
public interface IWeatherFlare {
    int DEFAULT_DURATION = 24000;

    void setWeather(ServerLevel serverLevel, double radio);
}
