package net.ty.createcraftedbeginning.api.gas.weatherflares;

import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.IWeatherFlare;
import org.jetbrains.annotations.NotNull;

public class WeatherFlaresQueryUtils {
    public static boolean isValidFlare(@NotNull ItemStack flare) {
        return !flare.isEmpty() && flare.getItem() instanceof IWeatherFlare;
    }
}
