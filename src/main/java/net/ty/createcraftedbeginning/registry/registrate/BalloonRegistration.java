package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllTags.AllItemTags;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class BalloonRegistration {
    private BalloonRegistration() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> balloon() {
        return builder -> builder.properties(properties -> properties.stacksTo(1)).tag(AllItemTags.PACKAGES.tag).lang("Balloon").setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> rareBalloon() {
        return builder -> builder.properties(properties -> properties.stacksTo(1)).tag(AllItemTags.PACKAGES.tag).lang("Rare Balloon").setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }
}
