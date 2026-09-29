package net.ty.createcraftedbeginning.recipe.gas;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@FunctionalInterface
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface GasAwareRecipe {
    GasRecipeData getGasRecipeData();

    default List<GasRecipeRequirement> getGasRequirements() {
        return getGasRecipeData().requirements();
    }
}
