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
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoolingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public CoolingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.COOLING, params);
    }

    private static FluidIngredient getFluidIngredient(@NotNull CoolingRecipe recipe) {
        return recipe.getIngredientsFluid();
    }

    private static Ingredient getItemIngredient(@NotNull CoolingRecipe recipe) {
        return recipe.getIngredientsItem();
    }

    private static CoolingData processCoolingRecipe(@NotNull CoolingRecipe recipe) {
        int resultTime = recipe.getResultTime();
        int requiredAmount = recipe.getRequiredAmount();

        return new CoolingData(resultTime, requiredAmount);
    }

    public static CoolingData getResultingCoolingTime(@NotNull Level level, @Nullable ItemStack itemStack, @Nullable FluidStack fluidStack) {
        List<RecipeHolder<CoolingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.COOLING.getType());
        for (RecipeHolder<CoolingRecipe> holder : recipes) {
            CoolingRecipe recipe = holder.value();

            if (recipe.isIngredientsFluid()) {
                if (itemStack != null || fluidStack == null) {
                    continue;
                }

                FluidStack currentFluidStack = getFluidIngredient(recipe).getMatchingFluidStacks().getFirst();
                if (!FluidStack.isSameFluid(currentFluidStack, fluidStack)) {
                    continue;
                }

                return processCoolingRecipe(recipe);
            } else {
                if (itemStack == null || fluidStack != null) {
                    continue;
                }

                ItemStack currentItemStack = getItemIngredient(recipe).getItems()[0];
                if (currentItemStack.getItem() != itemStack.getItem()) {
                    continue;
                }

                return processCoolingRecipe(recipe);
            }
        }

        return new CoolingData(0, 0);
    }

    public boolean isCreativeIceCream() {
        return getIngredientsItem().getItems()[0].getItem() instanceof CreativeIceCreamItem;
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

    public record CoolingData(int time, int amount) {
    }
}
