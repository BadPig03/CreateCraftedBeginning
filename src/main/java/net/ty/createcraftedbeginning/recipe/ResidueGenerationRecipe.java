package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueGenerationRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> {
    private static final Map<RecipeManager, Map<GasStack, ResidueOutput>> OUTPUT_CACHES = new WeakHashMap<>();

    ResidueGenerationRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.RESIDUE_GENERATION, params);
    }

    public static synchronized ResidueOutput findOutput(Level level, GasStack gasStack) {
        if (gasStack.isEmpty()) {
            return ResidueOutput.EMPTY;
        }

        Map<GasStack, ResidueOutput> outputCache = OUTPUT_CACHES.computeIfAbsent(level.getRecipeManager(), ignored -> new HashMap<>());
        return outputCache.computeIfAbsent(gasStack.copyWithAmount(1), normalizedGas -> findOutputUncached(level, normalizedGas));
    }

    public static synchronized void invalidateCaches() {
        OUTPUT_CACHES.clear();
    }

    private static ResidueOutput findOutputUncached(Level level, GasStack gasStack) {
        for (RecipeHolder<ResidueGenerationRecipe> recipeHolder : level.getRecipeManager().<SingleRecipeInput, ResidueGenerationRecipe>getAllRecipesFor(CCBRecipeTypes.RESIDUE_GENERATION.getType())) {
            ResidueGenerationRecipe recipe = recipeHolder.value();
            if (recipe.isIngredientEmpty() || !recipe.getIngredientsGas().ingredient().test(gasStack)) {
                continue;
            }

            boolean hasItemOutput = !recipe.getRollableResults().isEmpty();
            boolean hasFluidOutput = !recipe.getFluidResults().isEmpty();
            if (hasItemOutput && hasFluidOutput) {
                continue;
            }
            if (!hasItemOutput && !hasFluidOutput) {
                return ResidueOutput.EMPTY;
            }

            if (hasFluidOutput) {
                return ResidueOutput.fluid(recipe.getFluidResults().getFirst());
            }
            return ResidueOutput.item(recipe.getResultItem(level.registryAccess()));
        }
        return ResidueOutput.EMPTY;
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

    public SizedGasIngredient getIngredientsGas() {
        if (isIngredientEmpty()) {
            throw new IllegalStateException("Residue Generation Recipe has no gas ingredient!");
        }

        return gasIngredients.getFirst();
    }

    private boolean isIngredientEmpty() {
        return gasIngredients.isEmpty();
    }

    public boolean hasResidueOutput() {
        return !results.isEmpty() || !fluidResults.isEmpty();
    }

    public record ResidueOutput(ItemStack itemStack, FluidStack fluidStack) {
        private static final ResidueOutput EMPTY = new ResidueOutput(ItemStack.EMPTY, FluidStack.EMPTY);

        public ResidueOutput {
            itemStack = itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copyWithCount(1);
            fluidStack = fluidStack.isEmpty() ? FluidStack.EMPTY : fluidStack.copyWithAmount(1);
            if (!itemStack.isEmpty() && !fluidStack.isEmpty()) {
                throw new IllegalArgumentException("A residue output cannot contain both an item and a fluid.");
            }
        }

        private static ResidueOutput item(ItemStack itemStack) {
            return itemStack.isEmpty() ? EMPTY : new ResidueOutput(itemStack, FluidStack.EMPTY);
        }

        private static ResidueOutput fluid(FluidStack fluidStack) {
            return fluidStack.isEmpty() ? EMPTY : new ResidueOutput(ItemStack.EMPTY, fluidStack);
        }

        public boolean hasItem() {
            return !itemStack.isEmpty();
        }

        public boolean hasFluid() {
            return !fluidStack.isEmpty();
        }
    }
}
