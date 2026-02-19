package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoolingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public CoolingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.COOLING, params);
    }

    public static @NotNull CoolingData getCoolingTime(@NotNull Level level, @Nullable ItemStack itemStack, @Nullable FluidStack fluidStack) {
        List<RecipeHolder<CoolingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.COOLING.getType());
        for (RecipeHolder<CoolingRecipe> holder : recipes) {
            CoolingRecipe recipe = holder.value();
            if (recipe.isFluidIngredients()) {
                if (itemStack != null || fluidStack == null) {
                    continue;
                }
                if (!recipe.getFluidIngredient().ingredient().test(fluidStack)) {
                    continue;
                }
            }
            else {
                if (itemStack == null || fluidStack != null) {
                    continue;
                }
                if (!recipe.getIngredient().test(itemStack)) {
                    continue;
                }
            }

            return new CoolingData(recipe.processingDuration, recipe.isFluidIngredients() ? recipe.fluidIngredients.getFirst().amount() : 1);
        }

        return new CoolingData(0, 0);
    }

    public SizedFluidIngredient getFluidIngredient() {
        return fluidIngredients.getFirst();
    }

    public Ingredient getIngredient() {
        return ingredients.getFirst();
    }

    public boolean isFluidIngredients() {
        return ingredients.isEmpty() && !fluidIngredients.isEmpty();
    }

    public boolean isCreativeIceCream() {
        return getIngredient().getItems()[0].getItem() instanceof CreativeIceCreamItem;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level level) {
        return true;
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 0;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;
    }

    public record CoolingData(int time, int amount) {}
}
