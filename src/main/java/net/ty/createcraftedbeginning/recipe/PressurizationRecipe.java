package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressurizationRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> {
    private static final Map<RecipeManager, Map<GasStack, Optional<RecipeHolder<PressurizationRecipe>>>> RECIPE_CACHES = new WeakHashMap<>();

    public PressurizationRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.PRESSURIZATION, params);
    }

    public static Optional<PressurizationRecipe> findRecipe(Level level, GasStack input) {
        return findRecipeHolder(level, input).map(RecipeHolder::value);
    }

    public static synchronized Optional<RecipeHolder<PressurizationRecipe>> findRecipeHolder(Level level, GasStack input) {
        if (input.isEmpty()) {
            return Optional.empty();
        }

        RecipeManager manager = level.getRecipeManager();
        Map<GasStack, Optional<RecipeHolder<PressurizationRecipe>>> cache = RECIPE_CACHES.computeIfAbsent(manager, ignored -> new HashMap<>());
        return cache.computeIfAbsent(input.copyWithAmount(1), ignored -> findUncached(level, input));
    }

    private static Optional<RecipeHolder<PressurizationRecipe>> findUncached(Level level, GasStack input) {
        List<RecipeHolder<PressurizationRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.PRESSURIZATION.getType());
        for (RecipeHolder<PressurizationRecipe> holder : recipes) {
            PressurizationRecipe recipe = holder.value();
            if (!recipe.getGasIngredient().ingredient().test(input)) {
                continue;
            }

            return Optional.of(holder);
        }
        return Optional.empty();
    }

    public static synchronized void invalidateCaches() {
        RECIPE_CACHES.clear();
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return true;
    }

    public SizedGasIngredient getGasIngredient() {
        if (gasIngredients.isEmpty()) {
            throw new IllegalStateException("Pressurization Recipe has no gas ingredient!");
        }

        return gasIngredients.getFirst();
    }

    public GasStack getGasResult() {
        if (gasResults.isEmpty()) {
            throw new IllegalStateException("Pressurization Recipe has no gas result!");
        }

        return gasResults.getFirst();
    }

    @Override
    protected int getMaxInputCount() {
        return 0;
    }

    @Override
    protected int getMaxOutputCount() {
        return 0;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 1;
    }

    @Override
    protected int getMaxGasOutputCount() {
        return 1;
    }

    @Override
    protected void validateSpecial(List<String> errors) {
        if (gasIngredients.size() != 1) {
            errors.add("Pressurization recipes must have exactly one gas input.");
        }

        if (gasResults.size() != 1) {
            errors.add("Pressurization recipes must have exactly one gas output.");
        }
        else if (gasResults.getFirst().isEmpty()) {
            errors.add("Pressurization recipe gas output must not be empty.");
        }
    }
}
