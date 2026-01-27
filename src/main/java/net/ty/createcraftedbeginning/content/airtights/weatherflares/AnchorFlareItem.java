package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.NotNull;

public class AnchorFlareItem extends Item implements IWeatherFlare {
    public AnchorFlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public void setWeather(@NotNull ServerLevel serverLevel, double radio) {
        serverLevel.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(radio == WeatherFlareProjectileEntity.MIN_DELTA_MOVEMENT_LENGTH, serverLevel.getServer());
    }
}
