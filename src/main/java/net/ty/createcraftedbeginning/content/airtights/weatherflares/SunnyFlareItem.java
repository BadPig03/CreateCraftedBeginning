package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class SunnyFlareItem extends Item implements IWeatherFlare {
    public SunnyFlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public void setWeather(ServerLevel serverLevel, double ratio) {
        serverLevel.setWeatherParameters(Mth.ceil(DEFAULT_DURATION * ratio), 0, false, false);
    }
}
