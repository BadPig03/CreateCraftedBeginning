package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.BooleanValue;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class AnchorFlareItem extends Item implements IWeatherFlare {
    public AnchorFlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public void setWeather(ServerLevel level, double ignoredRatio) {
        BooleanValue weatherCycle = level.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE);
        weatherCycle.set(!weatherCycle.get(), level.getServer());
    }
}
