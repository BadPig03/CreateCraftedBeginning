package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.api.gas.GasStack;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ResidueRecipeLookup {
    private static final Map<RecipeManager, Map<GasStack, ResidueOutput>> OUTPUT_CACHES = new WeakHashMap<>();

    private ResidueRecipeLookup() {
    }

    public static synchronized ResidueOutput findOutput(Level level, GasStack gasStack) {
        if (gasStack.isEmpty()) {
            return ResidueOutput.EMPTY;
        }

        RecipeManager manager = level.getRecipeManager();
        Map<GasStack, ResidueOutput> outputCache = OUTPUT_CACHES.computeIfAbsent(manager, ignored -> new HashMap<>());
        return outputCache.computeIfAbsent(gasStack.copyWithAmount(1), normalizedGas -> {
            for (RecipeHolder<ResidueGenerationRecipe> recipeHolder : manager.<SingleRecipeInput, ResidueGenerationRecipe>getAllRecipesFor(CCBRecipeTypes.RESIDUE_GENERATION.getType())) {
                ResidueGenerationRecipe recipe = recipeHolder.value();
                if (recipe.getGasRequirements().isEmpty() || !recipe.getGasRequirement().ingredient().test(normalizedGas)) {
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
                    return new ResidueOutput(ItemStack.EMPTY, recipe.getFluidResults().getFirst());
                }

                return new ResidueOutput(recipe.getResultItem(level.registryAccess()), FluidStack.EMPTY);
            }

            return ResidueOutput.EMPTY;
        });
    }

    public static synchronized void invalidateCaches() {
        OUTPUT_CACHES.clear();
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

        @Override
        public ItemStack itemStack() {
            return itemStack.copy();
        }

        @Override
        public FluidStack fluidStack() {
            return fluidStack.copy();
        }

        public boolean hasItem() {
            return !itemStack.isEmpty();
        }

        public boolean hasFluid() {
            return !fluidStack.isEmpty();
        }
    }
}
