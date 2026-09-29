package net.ty.createcraftedbeginning.registry.registrate;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class IceCreamRegistration {
    private IceCreamRegistration() {
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> iceCreamCone() {
        return builder -> builder.properties(properties -> properties.food(new Builder().nutrition(4).saturationModifier(0.6F).build()));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> iceCream() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).food(iceCreamFood(4, 0.6F))).tag(CCBItemTags.ICE_CREAMS.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> flavoredIceCream(int nutrition, float saturationModifier) {
        return builder -> builder.properties(properties -> properties.stacksTo(16).food(iceCreamFood(nutrition, saturationModifier))).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> buildersTeaIceCream() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).food(new Builder().nutrition(4).saturationModifier(0.6F).alwaysEdible().effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0, false, false, false), 1).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> creativeIceCream() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).rarity(Rarity.EPIC).food(iceCreamFood(20, 1)));
    }

    private static FoodProperties iceCreamFood(int nutrition, float saturationModifier) {
        return new Builder().nutrition(nutrition).saturationModifier(saturationModifier).alwaysEdible().build();
    }
}
