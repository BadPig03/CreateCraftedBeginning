package net.ty.createcraftedbeginning.compat.jei.utils;

import com.simibubi.create.Create;
import com.simibubi.create.content.kinetics.fan.processing.HauntingRecipe;
import com.simibubi.create.content.kinetics.fan.processing.SplashingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Builder;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe.Factory;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.BlastingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberUtils;
import net.ty.createcraftedbeginning.recipe.ChillingRecipe;
import net.ty.createcraftedbeginning.registry.CCBItems;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FanProcessingFilterRecipeUtils {
    private static final ResourceLocation SPLASHING = Create.asResource("splashing");
    private static final ResourceLocation SMOKING = Create.asResource("smoking");
    private static final ResourceLocation BLASTING = Create.asResource("blasting");
    private static final ResourceLocation HAUNTING = Create.asResource("haunting");
    private static final ResourceLocation CHILLING = CreateCraftedBeginning.asResource("chilling");

    private static final RecipeType<RecipeHolder<SplashingRecipe>> FAN_WASHING = RecipeType.createRecipeHolderType(Create.asResource("fan_washing"));
    private static final RecipeType<RecipeHolder<SmokingRecipe>> FAN_SMOKING = RecipeType.createRecipeHolderType(Create.asResource("fan_smoking"));
    private static final RecipeType<RecipeHolder<AbstractCookingRecipe>> FAN_BLASTING = RecipeType.createRecipeHolderType(Create.asResource("fan_blasting"));
    private static final RecipeType<RecipeHolder<HauntingRecipe>> FAN_HAUNTING = RecipeType.createRecipeHolderType(Create.asResource("fan_haunting"));
    private static final RecipeType<RecipeHolder<ChillingRecipe>> FAN_CHILLING = RecipeType.createRecipeHolderType(CreateCraftedBeginning.asResource("chilling"));

    private FanProcessingFilterRecipeUtils() {
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registerStandardRecipe(registration, FAN_WASHING, SPLASHING, SplashingRecipe::new);
        registerSmokingRecipe(registration);
        registerBlastingRecipe(registration);
        registerStandardRecipe(registration, FAN_HAUNTING, HAUNTING, HauntingRecipe::new);
        registerStandardRecipe(registration, FAN_CHILLING, CHILLING, ChillingRecipe::new);
    }

    private static <R extends StandardProcessingRecipe<?>> void registerStandardRecipe(IRecipeRegistration registration, RecipeType<RecipeHolder<R>> jeiRecipeType, ResourceLocation fanProcessingTypeId, Factory<R> factory) {
        ItemStack result = createProcessedFilter(fanProcessingTypeId);
        if (result.isEmpty()) {
            return;
        }

        ResourceLocation recipeId = displayRecipeId(fanProcessingTypeId);
        R recipe = new Builder<>(factory, recipeId).withItemIngredients(Ingredient.of(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get())).withSingleItemOutput(result).build();
        registration.addRecipes(jeiRecipeType, List.of(new RecipeHolder<>(recipeId, recipe)));
    }

    private static void registerSmokingRecipe(IRecipeRegistration registration) {
        ItemStack result = createProcessedFilter(SMOKING);
        if (result.isEmpty()) {
            return;
        }

        ResourceLocation recipeId = displayRecipeId(SMOKING);
        SmokingRecipe recipe = new SmokingRecipe("", CookingBookCategory.MISC, Ingredient.of(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get()), result, 0, 100);
        registration.addRecipes(FAN_SMOKING, List.of(new RecipeHolder<>(recipeId, recipe)));
    }

    private static void registerBlastingRecipe(IRecipeRegistration registration) {
        ItemStack result = createProcessedFilter(BLASTING);
        if (result.isEmpty()) {
            return;
        }

        ResourceLocation recipeId = displayRecipeId(BLASTING);
        BlastingRecipe recipe = new BlastingRecipe("", CookingBookCategory.MISC, Ingredient.of(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get()), result, 0, 100);
        RecipeHolder<AbstractCookingRecipe> holder = new RecipeHolder<>(recipeId, recipe);
        registration.addRecipes(FAN_BLASTING, List.of(holder));
    }

    private static ItemStack createProcessedFilter(ResourceLocation fanProcessingTypeId) {
        return GasInjectionChamberUtils.getFanProcessingType(fanProcessingTypeId).map(type -> GasInjectionChamberUtils.create(new ItemStack(CCBItems.GAS_INJECTION_CHAMBER_FILTER.get()), type)).orElse(ItemStack.EMPTY);
    }

    private static ResourceLocation displayRecipeId(ResourceLocation fanProcessingTypeId) {
        return CreateCraftedBeginning.asResource("jei/filter/" + fanProcessingTypeId.getNamespace() + '/' + fanProcessingTypeId.getPath());
    }
}
