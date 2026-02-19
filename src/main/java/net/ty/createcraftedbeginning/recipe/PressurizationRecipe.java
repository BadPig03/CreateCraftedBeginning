package net.ty.createcraftedbeginning.recipe;

import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PressurizationRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> {
    public PressurizationRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.PRESSURIZATION, params);
    }

    public static @NotNull Gas getResultGasType(@NotNull Level level, Gas gasType) {
        List<RecipeHolder<PressurizationRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.PRESSURIZATION.getType());
        for (RecipeHolder<PressurizationRecipe> holder : recipes) {
            PressurizationRecipe recipe = holder.value();
            if (!recipe.getGasIngredient().test(gasType)) {
                continue;
            }

            return recipe.getGasResult().getGasType();
        }

        return Gas.EMPTY_GAS_HOLDER.value();
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
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
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 1;
    }

    @Override
    protected int getMaxGasOutputCount() {
        return 1;
    }
}
