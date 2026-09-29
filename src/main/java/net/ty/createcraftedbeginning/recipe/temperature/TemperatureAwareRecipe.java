package net.ty.createcraftedbeginning.recipe.temperature;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface TemperatureAwareRecipe {
    TemperatureRecipeData getTemperatureRecipeData();

    default TemperatureCondition getTemperatureCondition() {
        return getTemperatureRecipeData().condition();
    }

    default TemperatureMatching getTemperatureMatching() {
        return getTemperatureRecipeData().matching();
    }
}
