package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeParams;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;
import net.ty.createcraftedbeginning.recipe.pressure.PressureRequirement;

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
            return errors;
        }

        GasRecipeRequirement gas = getGasRequirement();
        long gasAmount = gas.amount();
        if (gasAmount != 1) {
            errors.add("Residue gas input must be a unit template; got " + gasAmount + " GU.");
        }

        PressureRequirement pressure = gas.pressure();
        pressure.minimumPressurePa().ifPresent(value -> errors.add("Residue mappings must not specify minimum pressure; got " + value + " Pa."));
        pressure.maximumPressurePa().ifPresent(value -> errors.add("Residue mappings must not specify maximum pressure; got " + value + " Pa."));
        if (!results.isEmpty() && !fluidResults.isEmpty()) {
            errors.add("Residue generation recipes may output at most one item or one fluid, never both.");
        }

        if (!results.isEmpty()) {
            ProcessingOutput item = results.getFirst();
            int count = item.getStack().getCount();
            float chance = item.getChance();
            if (count != 1 || chance != 1) {
                errors.add("Item residue must be a unit template with chance 1; got count=" + count + ", chance=" + chance + '.');
            }
        }

        if (fluidResults.isEmpty()) {
            return errors;
        }

        int amount = fluidResults.getFirst().getAmount();
        if (amount != 1) {
            errors.add("Fluid residue must be a unit template; got " + amount + " mB.");
        }

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
