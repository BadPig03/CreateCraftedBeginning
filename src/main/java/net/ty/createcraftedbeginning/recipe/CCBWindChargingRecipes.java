package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.equipment.BuildersTeaItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.OminousBottleItem;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.icecreams.MilkIceCreamItem;
import net.ty.createcraftedbeginning.registry.CCBFluids;
import net.ty.createcraftedbeginning.registry.CCBItems;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class CCBWindChargingRecipes extends WindChargingRecipeGen {
    GeneratedRecipe CAKE = create("cake", b -> b.require(Items.CAKE).output(CCBFluids.VIRTUAL_TIME.get(), 3068));
    GeneratedRecipe MILK_BUCKET = create("milk_bucket", b -> b.require(Items.MILK_BUCKET).output(Items.MILK_BUCKET).output(CCBFluids.VIRTUAL_TIME.get(), 32767));
    GeneratedRecipe MILK_ICE_CREAM = create("milk_ice_cream", b -> b.require(CCBItems.MILK_ICE_CREAM).output(Items.MILK_BUCKET).output(CCBFluids.VIRTUAL_TIME.get(), 32767));

    public CCBWindChargingRecipes(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, CreateCraftedBeginning.MOD_ID);

        addFoodRecipes(registries);
    }

    private void addFoodRecipes(@NotNull CompletableFuture<HolderLookup.Provider> registriesFuture) {
        registriesFuture.thenAccept(registries -> {
            HolderLookup<Item> items = registries.lookupOrThrow(Registries.ITEM);

            items.listElements().forEach(holder -> {
                Item item = holder.value();

                FoodProperties properties = item.getFoodProperties(new ItemStack(item), null);
                if (properties != null && !isFoodInvalid(item)) {
                    int nutrition = properties.nutrition();
                    float saturation = properties.saturation();
                    int effectsCount = 0;

                    List<FoodProperties.PossibleEffect> effectList = properties.effects();
                    if (!effectList.isEmpty()) {
                        for (FoodProperties.PossibleEffect effect : effectList) {
                            MobEffect mobEffect = effect.effect().getEffect().value();
                            MobEffectCategory category = mobEffect.getCategory();
                            int amplifier = effect.effect().getAmplifier() + 1;
                            if (category == MobEffectCategory.BENEFICIAL) {
                                effectsCount = effectsCount + amplifier;
                            } else if (category == MobEffectCategory.HARMFUL) {
                                effectsCount = effectsCount - amplifier;
                            }
                        }
                    }

                    int multiplier = Mth.abs(effectsCount) + 1;
                    int time = Mth.ceil(Math.pow(0.5 * nutrition + saturation, 1.5) * 100 * multiplier);
                    String itemName = item.getDescriptionId().split("\\.")[2];
                    if (effectsCount < 0) {
                        create(itemName, b -> b.require(item).output(Items.POISONOUS_POTATO).output(CCBFluids.VIRTUAL_TIME.get(), time * 2));
                    } else {
                        create(itemName, b -> b.require(item).output(CCBFluids.VIRTUAL_TIME.get(), time));
                    }
                }
            });
        });
    }

    private boolean isFoodInvalid(Item item) {
        return item instanceof OminousBottleItem || item instanceof MilkIceCreamItem || item instanceof BuildersTeaItem;
    }
}
