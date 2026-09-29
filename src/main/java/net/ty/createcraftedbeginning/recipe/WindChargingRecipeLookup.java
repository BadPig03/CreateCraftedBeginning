package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.recipe.WindChargingRecipe.WindChargingAction;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class WindChargingRecipeLookup {

    private WindChargingRecipeLookup() {
    }

    public static WindChargingData resolveWindChargingData(Level level, ItemStack itemStack) {
        RecipeHolder<WindChargingRecipe> bestMatch = null;
        for (RecipeHolder<WindChargingRecipe> recipeHolder : level.getRecipeManager().<SingleRecipeInput, WindChargingRecipe>getAllRecipesFor(CCBRecipeTypes.WIND_CHARGING.getType())) {
            WindChargingRecipe recipe = recipeHolder.value();
            if (!recipe.getIngredient().test(itemStack)) {
                continue;
            }

            if (bestMatch == null) {
                bestMatch = recipeHolder;
                continue;
            }

            int priority = Integer.compare(recipe.getPriority(), bestMatch.value().getPriority());
            if (priority < 0 || priority == 0 && recipeHolder.id().toString().compareTo(bestMatch.id().toString()) >= 0) {
                continue;
            }

            bestMatch = recipeHolder;
        }

        if (bestMatch == null) {
            return new WindChargingFoodValue(itemStack).calculate();
        }

        WindChargingRecipe recipe = bestMatch.value();
        WindChargingAction action = recipe.getAction();
        int chargingTime = action == WindChargingAction.CHARGE ? recipe.getProcessingDuration() : 0;
        ItemStack recipeResult = recipe.getResultItem(level.registryAccess()).copy();
        return new WindChargingData(action, chargingTime, 1, recipeResult);
    }

    public record WindChargingData(WindChargingAction action, int time, int amount, ItemStack recipeResult) {
        public WindChargingData {
            recipeResult = recipeResult.copy();
        }

        @Override
        public ItemStack recipeResult() {
            return recipeResult.copy();
        }
    }
}
