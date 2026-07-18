package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizationRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> {
    public EnergizationRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.ENERGIZATION, params);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return true;
    }

    public SizedGasIngredient getGasIngredient() {
        if (gasIngredients.isEmpty()) {
            throw new IllegalStateException("Energization Recipe has no gas ingredient!");
        }

        return gasIngredients.getFirst();
    }

    public GasStack getGasResult() {
        if (gasResults.isEmpty()) {
            throw new IllegalStateException("Energization Recipe has no gas result!");
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
