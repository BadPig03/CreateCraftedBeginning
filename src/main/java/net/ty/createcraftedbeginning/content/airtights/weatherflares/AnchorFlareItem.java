package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;
import net.ty.createcraftedbeginning.api.weatherflares.IWeatherFlare;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnchorFlareItem extends Item implements IWeatherFlare {
    public AnchorFlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public void setWeather(ServerLevel level, double ignoredRatio) {
        BooleanValue weatherCycleRule = level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE);
        weatherCycleRule.set(!weatherCycleRule.get(), level.getServer());
    }
}
