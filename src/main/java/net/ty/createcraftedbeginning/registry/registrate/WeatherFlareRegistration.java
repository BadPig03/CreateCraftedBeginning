package net.ty.createcraftedbeginning.registry.registrate;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class WeatherFlareRegistration {
    private WeatherFlareRegistration() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> weatherFlare() {
        return builder -> builder.properties(properties -> properties.stacksTo(16)).tag(CCBItemTags.WEATHER_FLARE.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> anchorFlare() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON)).tag(CCBItemTags.WEATHER_FLARE.tag);
    }
}
