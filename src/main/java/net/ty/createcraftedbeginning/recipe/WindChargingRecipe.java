package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.content.icecreams.CreativeIceCreamItem;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class WindChargingRecipe extends StandardProcessingRecipe<SingleRecipeInput> {
    public WindChargingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.WIND_CHARGING, params);
    }

    private static Ingredient getItemIngredient(@NotNull WindChargingRecipe recipe) {
        return recipe.getIngredientsItem();
    }

    private static @NotNull WindChargingData processWindChargingRecipe(@NotNull WindChargingRecipe recipe) {
        int resultTime = recipe.getResultTime();
        int requiredAmount = recipe.getRequiredAmount();
        boolean isBadFood = recipe.isBadFood();
        boolean isMilky = recipe.isMilkyItem();

        return new WindChargingData(resultTime, requiredAmount, isBadFood, isMilky);
    }

    public static @NotNull WindChargingData getResultingWindChargingTime(@NotNull Level level, ItemStack itemStack) {
        List<RecipeHolder<WindChargingRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.WIND_CHARGING.getType());

        for (RecipeHolder<WindChargingRecipe> holder : recipes) {
            WindChargingRecipe recipe = holder.value();
            ItemStack currentItemStack = getItemIngredient(recipe).getItems()[0];
            if (currentItemStack.getItem() != itemStack.getItem()) {
                continue;
            }

            return processWindChargingRecipe(recipe);
        }

        return new WindChargingData(0, 0, false, false);
    }

    public boolean isBadFood() {
        if (results.isEmpty()) {
            return false;
        }
        return results.getFirst().getStack().getItem() == Items.POISONOUS_POTATO;
    }

    public boolean isMilkyItem() {
        if (results.isEmpty()) {
            return false;
        }
        return results.getFirst().getStack().getItem() == Items.MILK_BUCKET;
    }

    public boolean isCreativeIceCream() {
        return getIngredientsItem().getItems()[0].getItem() instanceof CreativeIceCreamItem;
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level worldIn) {
        return true;
    }

    public Ingredient getIngredientsItem() {
        return ingredients.getFirst();
    }

    public int getResultTime() {
        return fluidResults.getFirst().getAmount();
    }

    public int getRequiredAmount() {
        return 1;
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 2;
    }

    @Override
    protected int getMaxFluidOutputCount() {
        return 1;
    }

    public record WindChargingData(int time, int amount, boolean isBadFood, boolean isMilky) {
    }
}
