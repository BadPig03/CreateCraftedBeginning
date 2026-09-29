package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeParams;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class EnergizationRecipe extends StandardGasProcessingRecipe<SingleRecipeInput> {
    EnergizationRecipe(GasProcessingRecipeParams params) {
        super(CCBRecipeTypes.ENERGIZATION, params);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return true;
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

    public GasRecipeRequirement getGasRequirement() {
        if (getGasRequirements().isEmpty()) {
            throw new IllegalStateException("Energization recipe has no gas requirement.");
        }

        return getGasRequirements().getFirst();
    }

    public GasStack getGasResult() {
        if (gasResults.isEmpty()) {
            throw new IllegalStateException("Energization recipe has no gas result.");
        }

        return gasResults.getFirst();
    }
}
