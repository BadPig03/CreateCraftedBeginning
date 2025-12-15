package net.ty.createcraftedbeginning.data;

import com.simibubi.create.content.equipment.BuildersTeaItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.FoodProperties.PossibleEffect;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.OminousBottleItem;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.icecreams.MilkIceCreamItem;
import net.ty.createcraftedbeginning.recipe.generators.WindChargingRecipeGen;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBWindChargingRecipes extends WindChargingRecipeGen {
    GeneratedRecipe CAKE = create("cake", b -> b.require(Items.CAKE).duration(3068));
    GeneratedRecipe MILK_BUCKET = create("milk_bucket", b -> b.require(Items.MILK_BUCKET).duration(0));
    GeneratedRecipe MILK_ICE_CREAM = create("milk_ice_cream", b -> b.require(CCBItems.MILK_ICE_CREAM).duration(0));

    public CCBWindChargingRecipes(PackOutput output, CompletableFuture<Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);
        addFoodRecipes(registries);
    }

    private static boolean isFoodInvalid(Item item) {
        return item instanceof OminousBottleItem || item instanceof MilkIceCreamItem || item instanceof BuildersTeaItem;
    }

    private void addFoodRecipes(@NotNull CompletableFuture<Provider> registriesFuture) {
        registriesFuture.thenAccept(registries -> {
            HolderLookup<Item> items = registries.lookupOrThrow(Registries.ITEM);
            items.listElements().forEach(holder -> {
                Item item = holder.value();
                FoodProperties properties = item.getFoodProperties(new ItemStack(item), null);
                if (properties == null || isFoodInvalid(item)) {
                    return;
                }

                int nutrition = properties.nutrition();
                float saturation = properties.saturation();
                int effectsCount = 0;
                List<PossibleEffect> effectList = properties.effects();
                if (!effectList.isEmpty()) {
                    for (PossibleEffect effect : effectList) {
                        MobEffectCategory category = effect.effect().getEffect().value().getCategory();
                        int amplifier = effect.effect().getAmplifier() + 1;
                        if (category == MobEffectCategory.BENEFICIAL) {
                            effectsCount = effectsCount + amplifier;
                        }
                        else if (category == MobEffectCategory.HARMFUL) {
                            effectsCount = effectsCount - amplifier;
                        }
                    }
                }

                int time = Mth.ceil(Math.pow(0.5 * nutrition + saturation, 1.5) * 100 * (Mth.abs(effectsCount) + 1));
                String itemName = item.getDescriptionId().split("\\.")[2];
                create(itemName, effectsCount < 0 ? b -> b.require(item).duration(-time * 2) : b -> b.require(item).duration(time));
            });
        });
    }
}
