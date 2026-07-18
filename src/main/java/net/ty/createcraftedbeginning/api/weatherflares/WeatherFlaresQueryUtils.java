package net.ty.createcraftedbeginning.api.weatherflares;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.ty.createcraftedbeginning.content.airtights.weatherflares.IWeatherFlare;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class WeatherFlaresQueryUtils {
    /**
     * Checks whether the supplied item stack is a valid weather flare.
     *
     * @param flare the weather flare item stack
     * @return {@code true} if the supplied item stack is a valid weather flare; otherwise {@code false}
     */
    public static boolean isValidFlare(ItemStack flare) {
        return !flare.isEmpty() && flare.getItem() instanceof IWeatherFlare;
    }
}
