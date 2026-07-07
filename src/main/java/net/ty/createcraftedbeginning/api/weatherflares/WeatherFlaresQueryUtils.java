package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.IWeatherFlare;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WeatherFlaresQueryUtils {
    public static boolean isValidFlare(ItemStack flare) {
        return !flare.isEmpty() && flare.getItem() instanceof IWeatherFlare;
    }
}
