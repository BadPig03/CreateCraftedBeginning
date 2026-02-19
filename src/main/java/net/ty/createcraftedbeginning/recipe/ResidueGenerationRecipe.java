package net.ty.createcraftedbeginning.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.ty.createcraftedbeginning.api.gas.gases.Gas;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ResidueGenerationRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> {
    public ResidueGenerationRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.RESIDUE_GENERATION, params);
    }

    public static FluidStack getRequiredFluid(@NotNull Level level, Gas gasType) {
        List<RecipeHolder<ResidueGenerationRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.RESIDUE_GENERATION.getType());
        for (RecipeHolder<ResidueGenerationRecipe> holder : recipes) {
            ResidueGenerationRecipe recipe = holder.value();
            if (recipe.getFluidResults().isEmpty() || !recipe.getIngredientsGas().test(gasType)) {
                continue;
            }

            return recipe.getFluidResults().getFirst();
        }
        return FluidStack.EMPTY;
    }

    public static ItemStack getRequiredItem(@NotNull Level level, Gas gasType) {
        List<RecipeHolder<ResidueGenerationRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.RESIDUE_GENERATION.getType());
        for (RecipeHolder<ResidueGenerationRecipe> holder : recipes) {
            ResidueGenerationRecipe recipe = holder.value();
            if (!recipe.getFluidResults().isEmpty() || !recipe.getIngredientsGas().test(new GasStack(gasType, FluidType.BUCKET_VOLUME))) {
                continue;
            }

            return recipe.getResultItem(level.registryAccess());
        }
        return ItemStack.EMPTY;
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 1;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput input, @NotNull Level level) {
        return true;
    }

    public SizedGasIngredient getIngredientsGas() {
        if (isIngredientEmpty()) {
            throw new IllegalStateException("Residue Generation Recipe has no gas ingredient!");
        }

        return gasIngredients.getFirst();
    }

    public boolean isIngredientEmpty() {
        return gasIngredients.isEmpty();
    }
}
