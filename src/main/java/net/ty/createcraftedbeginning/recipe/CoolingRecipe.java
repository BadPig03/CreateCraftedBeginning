package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CoolingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public CoolingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.COOLING, params);
    }



    private static FluidIngredient getFluidIngredient(Object recipe) {
        if (recipe instanceof CoolingRecipe cooling) {
            return cooling.getIngredientsFluid();
        } else if (recipe instanceof SuperCoolingRecipe superCooling) {
            return superCooling.getIngredientsFluid();
        }
        return FluidIngredient.EMPTY;
    }

    private static Ingredient getItemIngredient(Object recipe) {
        if (recipe instanceof CoolingRecipe cooling) {
            return cooling.getIngredientsItem();
        } else if (recipe instanceof SuperCoolingRecipe superCooling) {
            return superCooling.getIngredientsItem();
        }
        return Ingredient.EMPTY;
    }

    private static CoolingData processCoolingRecipe(Object recipe, ItemStack itemStack, FluidStack fluidStack) {
        boolean isFluidRecipe = false;
        int resultTime = 0;
        int requiredAmount = 0;

        if (recipe instanceof CoolingRecipe cooling) {
            isFluidRecipe = cooling.isIngredientsFluid();
            resultTime = cooling.getResultTime();
            requiredAmount = cooling.getRequiredAmount();
        } else if (recipe instanceof SuperCoolingRecipe superCooling) {
            isFluidRecipe = superCooling.isIngredientsFluid();
            resultTime = superCooling.getResultTime();
            requiredAmount = superCooling.getRequiredAmount();
        }

        CoolingData coolingData = new CoolingData(recipe instanceof SuperCoolingRecipe ? "powerful" : "normal", resultTime, requiredAmount);
        if (isFluidRecipe && fluidStack != null) {
            FluidIngredient fluidIngredient = getFluidIngredient(recipe);
            for (FluidStack ingredientFluid : fluidIngredient.getMatchingFluidStacks()) {
                if (FluidStack.isSameFluidSameComponents(fluidStack, ingredientFluid)) {
                    return coolingData;
                }
            }
        } else if (!isFluidRecipe && itemStack != null) {
            Ingredient ingredient = getItemIngredient(recipe);
            if (ingredient.test(itemStack)) {
                return coolingData;
            }
        }

        return new CoolingData("none", 0, 0);
    }

    private static CoolingData getCoolingDataFromRecipe(RecipeHolder<?> holder, ItemStack itemStack, FluidStack fluidStack) {
        if (holder.value() instanceof CoolingRecipe coolingRecipe) {
            return processCoolingRecipe(coolingRecipe, itemStack, fluidStack);
        } else if (holder.value() instanceof SuperCoolingRecipe superCoolingRecipe) {
            return processCoolingRecipe(superCoolingRecipe, itemStack, fluidStack);
        }
        return new CoolingData("none", 0, 0);
    }

    public static CoolingData getResultingCoolingTime(Level level, ItemStack itemStack, FluidStack fluidStack) {
        List<RecipeHolder<CoolingRecipe>> normalRecipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.COOLING.getType());
        List<RecipeHolder<SuperCoolingRecipe>> superRecipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.SUPER_COOLING.getType());

        for (RecipeHolder<SuperCoolingRecipe> holder : superRecipes) {
            CoolingData data = getCoolingDataFromRecipe(holder, itemStack, fluidStack);
            if (!"none".equals(data.type)) {
                return data;
            }
        }

        for (RecipeHolder<CoolingRecipe> holder : normalRecipes) {
            CoolingData data = getCoolingDataFromRecipe(holder, itemStack, fluidStack);
            if (!"none".equals(data.type)) {
                return data;
            }
        }

        return new CoolingData("none", 0, 0);
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level worldIn) {
        return true;
    }

    public boolean isIngredientsFluid() {
        return ingredients.isEmpty() && !fluidIngredients.isEmpty();
    }

    public FluidIngredient getIngredientsFluid() {
        return fluidIngredients.getFirst();
    }

    public Ingredient getIngredientsItem() {
        return ingredients.getFirst();
    }

    public int getResultTime() {
        return fluidResults.getFirst().getAmount();
    }

    public int getRequiredAmount() {
        if (isIngredientsFluid()) {
            return fluidIngredients.getFirst().getRequiredAmount();
        } else {
            return 1;
        }
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
    protected int getMaxFluidInputCount() {
        return 1;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 1;
    }

    public record CoolingData(String type, int time, int amount) {
    }
}
