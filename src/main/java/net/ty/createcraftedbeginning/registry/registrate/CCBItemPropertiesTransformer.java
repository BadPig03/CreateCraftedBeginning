package net.ty.createcraftedbeginning.registry.registrate;

import com.simibubi.create.AllTags.AllItemTags;
import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;
import com.tterrag.registrate.util.nullness.NonNullFunction;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.Builder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.Tags.Items;
import net.ty.createcraftedbeginning.registry.CCBTags.CCBItemTags;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class CCBItemPropertiesTransformer {
    private CCBItemPropertiesTransformer() {
    }

    private static FoodProperties iceCreamFood(int nutrition, float saturationModifier) {
        return new Builder().nutrition(nutrition).saturationModifier(saturationModifier).alwaysEdible().build();
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
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> breezeCore() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).rarity(Rarity.EPIC));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> incompleteTeslaTurbineRotor() {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.UNCOMMON).fireResistant());
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> teslaTurbineRotor() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON).fireResistant());
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasInjectionChamberFilter() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).fireResistant());
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> incompleteAirtightEquipment() {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.EPIC).fireResistant());
    }

    @SafeVarargs
    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> airtightEquipment(TagKey<Item>... enchantmentTags) {
        return builder -> {
            ItemBuilder<T, P> result = builder.properties(properties -> properties.rarity(Rarity.EPIC).fireResistant().stacksTo(1));
            if (enchantmentTags.length == 0) {
                return result;
            }
            return result.tag(Items.ENCHANTABLES).tag(enchantmentTags);
        };
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> airtightArmor(TagKey<Item> armorEnchantmentTag) {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.EPIC).fireResistant().stacksTo(1)).tag(CCBItemTags.AIRTIGHT_ARMOR.tag, Items.ENCHANTABLES, armorEnchantmentTag, ItemTags.VANISHING_ENCHANTABLE, ItemTags.EQUIPPABLE_ENCHANTABLE);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> stack16() {
        return builder -> builder.properties(properties -> properties.stacksTo(16));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> weatherFlare() {
        return builder -> builder.properties(properties -> properties.stacksTo(16)).tag(CCBItemTags.WEATHER_FLARE.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> uncommon() {
        return builder -> builder.properties(properties -> properties.rarity(Rarity.UNCOMMON));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> anchorFlare() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).rarity(Rarity.UNCOMMON)).tag(CCBItemTags.WEATHER_FLARE.tag);
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
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> iceCreamCone() {
        return builder -> builder.properties(properties -> properties.food(new Builder().nutrition(4).saturationModifier(0.6f).build()));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> iceCream() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).food(iceCreamFood(4, 0.6f))).tag(CCBItemTags.ICE_CREAMS.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> flavoredIceCream(int nutrition, float saturationModifier) {
        return builder -> builder.properties(properties -> properties.stacksTo(16).food(iceCreamFood(nutrition, saturationModifier))).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> buildersTeaIceCream() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).food(new Builder().nutrition(4).saturationModifier(0.6f).alwaysEdible().effect(() -> new MobEffectInstance(MobEffects.DIG_SPEED, 3600, 0, false, false, false), 1).build())).tag(CCBItemTags.ICE_CREAM_WITH_FLAVOR.tag).tag(CCBItemTags.ICE_CREAMS.tag);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> creativeIceCream() {
        return builder -> builder.properties(properties -> properties.stacksTo(16).rarity(Rarity.EPIC).food(iceCreamFood(20, 1)));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> balloon() {
        return builder -> builder.properties(properties -> properties.stacksTo(1)).tag(AllItemTags.PACKAGES.tag).lang("Balloon").setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> rareBalloon() {
        return builder -> builder.properties(properties -> properties.stacksTo(1)).tag(AllItemTags.PACKAGES.tag).lang("Rare Balloon").setData(ProviderType.LANG, NonNullBiConsumer.noop());
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> stack1() {
        return builder -> builder.properties(properties -> properties.stacksTo(1));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasCanisterPack() {
        return builder -> builder.properties(properties -> properties.stacksTo(1).fireResistant().rarity(Rarity.UNCOMMON));
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> gasCanister() {
        return builder -> builder.properties(properties -> properties.stacksTo(1).fireResistant()).tag(Items.ENCHANTABLES, CCBItemTags.GAS_CANISTER_ENCHANTABLE.tag, AllItemTags.PRESSURIZED_AIR_SOURCES.tag, ItemTags.VANISHING_ENCHANTABLE);
    }

    @Contract(pure = true)
    public static <T extends Item, P> @NotNull NonNullFunction<ItemBuilder<T, P>, ItemBuilder<T, P>> creativeGasCanister() {
        return builder -> builder.properties(properties -> properties.stacksTo(1).fireResistant().rarity(Rarity.EPIC));
    }
}
