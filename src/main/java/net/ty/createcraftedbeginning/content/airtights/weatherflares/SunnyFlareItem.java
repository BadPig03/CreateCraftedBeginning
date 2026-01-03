package net.ty.createcraftedbeginning.content.airtights.weatherflares;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.NotNull;

public class SunnyFlareItem extends Item implements IWeatherFlare {
    public SunnyFlareItem(Properties properties) {
        super(properties);
    }

    @Override
    public void setWeather(@NotNull ServerLevel serverLevel, double ratio) {
        serverLevel.setWeatherParameters(Mth.ceil(DEFAULT_DURATION * ratio), 0, false, false);
    }
}
