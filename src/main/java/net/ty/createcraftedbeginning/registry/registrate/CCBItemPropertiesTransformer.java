package net.ty.createcraftedbeginning.registry.registrate;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBItemPropertiesTransformer {
    private CCBItemPropertiesTransformer() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> defaultProperties() {
        return builder -> builder;
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> fireResistant() {
        return builder -> builder.properties(Properties::fireResistant);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> epic() {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.EPIC));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> stack16() {
        return builder -> builder.properties(properties -> properties.stacksTo(16));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> uncommon() {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.UNCOMMON));
    }

    @SafeVarargs
    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> tags(TagKey<Item>... tags) {
        return builder -> builder.tag(tags);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> uncommonMaterial(TagKey<Item> tag) {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.UNCOMMON)).tag(tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> stack1() {
        return builder -> builder.properties(properties -> properties.stacksTo(1));
    }
}
