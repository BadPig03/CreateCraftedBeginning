package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner.Plan;
import net.ty.createcraftedbeginning.recipe.interfaces.ReactorKettleRecipeContext;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ReactorKettleCraftPreparation {
    private ReactorKettleCraftPreparation() {
    }

    public static Optional<Plan> prepare(ReactorKettleRecipeContext kettle, ReactorKettleRecipe recipe) {
        ReactorKettleCraftPlanner planner = new ReactorKettleCraftPlanner(kettle, recipe);
        return planner.planInputs().flatMap(inputs -> planner.acceptOutputs(inputs, new ReactorKettleRecipeOutputs(recipe).roll(inputs)));
    }
}
