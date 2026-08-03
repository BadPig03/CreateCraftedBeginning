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
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ResidueGenerationRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> {
    private static final Map<RecipeManager, Map<GasStack, ResidueOutput>> OUTPUT_CACHES = new WeakHashMap<>();

    public ResidueGenerationRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.RESIDUE_GENERATION, params);
    }

    public static synchronized ResidueOutput findOutput(Level level, GasStack gasStack) {
        if (gasStack.isEmpty()) {
            return ResidueOutput.EMPTY;
        }

        RecipeManager manager = level.getRecipeManager();
        Map<GasStack, ResidueOutput> cache = OUTPUT_CACHES.computeIfAbsent(manager, ignored -> new HashMap<>());
        GasStack cacheKey = gasStack.copyWithAmount(1);
        return cache.computeIfAbsent(cacheKey, key -> findOutputUncached(level, key));
    }

    private static ResidueOutput findOutputUncached(Level level, GasStack gasStack) {
        List<RecipeHolder<ResidueGenerationRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.RESIDUE_GENERATION.getType());
        for (RecipeHolder<ResidueGenerationRecipe> holder : recipes) {
            ResidueGenerationRecipe recipe = holder.value();
            if (recipe.isIngredientEmpty() || !recipe.getIngredientsGas().ingredient().test(gasStack)) {
                continue;
            }

            boolean hasItemOutput = !recipe.getRollableResults().isEmpty();
            boolean hasFluidOutput = !recipe.getFluidResults().isEmpty();
            if (hasItemOutput == hasFluidOutput) {
                continue;
            }

            if (hasFluidOutput) {
                return ResidueOutput.fluid(recipe.getFluidResults().getFirst());
            }
            return ResidueOutput.item(recipe.getResultItem(level.registryAccess()));
        }
        return ResidueOutput.EMPTY;
    }

    public static synchronized void invalidateCaches() {
        OUTPUT_CACHES.clear();
    }

    @Override
    public List<String> validate() {
        List<String> errors = super.validate();
        if (gasIngredients.size() != 1) {
            errors.add("Residue generation recipes must have exactly one gas input.");
        }

        int outputTypes = (results.isEmpty() ? 0 : 1) + (fluidResults.isEmpty() ? 0 : 1);
        if (outputTypes == 1) {
            return errors;
        }

        errors.add("Residue generation recipes must output exactly one item or one fluid, never both.");
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

    public boolean isIngredientEmpty() {
        return gasIngredients.isEmpty();
    }

    public record ResidueOutput(ItemStack itemStack, FluidStack fluidStack) {
        public static final ResidueOutput EMPTY = new ResidueOutput(ItemStack.EMPTY, FluidStack.EMPTY);

        public ResidueOutput {
            itemStack = itemStack.isEmpty() ? ItemStack.EMPTY : itemStack.copyWithCount(1);
            fluidStack = fluidStack.isEmpty() ? FluidStack.EMPTY : fluidStack.copyWithAmount(1);
            if (!itemStack.isEmpty() && !fluidStack.isEmpty()) {
                throw new IllegalArgumentException("A residue output cannot contain both an item and a fluid.");
            }
        }

        public static ResidueOutput item(ItemStack itemStack) {
            return itemStack.isEmpty() ? EMPTY : new ResidueOutput(itemStack, FluidStack.EMPTY);
        }

        public static ResidueOutput fluid(FluidStack fluidStack) {
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
