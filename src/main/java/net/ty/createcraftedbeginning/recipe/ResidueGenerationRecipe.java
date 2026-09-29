package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeParams;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueGenerationRecipe extends StandardGasProcessingRecipe<SingleRecipeInput> {

    ResidueGenerationRecipe(GasProcessingRecipeParams params) {
        super(CCBRecipeTypes.RESIDUE_GENERATION, params);
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        if (gasIngredients.size() != 1) {
            errors.add("Residue generation recipes must have exactly one gas input.");
        }

        int outputTypeCount = (results.isEmpty() ? 0 : 1) + (fluidResults.isEmpty() ? 0 : 1);
        if (outputTypeCount <= 1) {
            return errors;
        }

        errors.add("Residue generation recipes may output at most one item or one fluid, never both.");
        return errors;
    }

    @Override
    protected int getMaxInputCount() {
        return 0;
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
    protected int getMaxGasInputCount() {
        return 1;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return true;
    }

    public GasRecipeRequirement getGasRequirement() {
        if (getGasRequirements().isEmpty()) {
            throw new IllegalStateException("Residue generation recipe has no gas requirement.");
        }

        return getGasRequirements().getFirst();
    }

    public boolean hasResidueOutput() {
        return !results.isEmpty() || !fluidResults.isEmpty();
    }
}
