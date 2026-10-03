package net.ty.createcraftedbeginning.recipe;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.pressure.GasPressureCompartment;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractFluid;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractGas;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractItem;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrieFinder;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class GasInjectionRecipeLookup {
    private static final Object RECIPE_CACHE_KEY = new Object();
    private final Level level;
    private final GasPressureCompartment gasSource;

    public GasInjectionRecipeLookup(Level level, GasPressureCompartment gasSource) {
        this.level = level;
        this.gasSource = gasSource;
    }

    public static void invalidateRecipeCaches() {
        AirtightRecipeTrieFinder.invalidateFailures(RECIPE_CACHE_KEY);
    }

    public Optional<RecipeMatch> findRecipeMatch(ItemStack itemStack) {
        GasStack gasStack = gasSource.getGasStack();
        if (itemStack.isEmpty() || gasStack.isEmpty()) {
            return Optional.empty();
        }

        SingleRecipeInput recipeInput = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipeHolder = SequencedAssemblyRecipe.getRecipe(level, recipeInput, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndGas(recipeInput));
        if (assemblyRecipeHolder.isPresent()) {
            return Optional.of(new RecipeMatch(assemblyRecipeHolder.get().value(), true));
        }

        if (!AirtightRecipeTrieFinder.hasFailed(RECIPE_CACHE_KEY, level)) {
            try {
                return findItemInTrie(itemStack, recipeInput);
            }
            catch (ExecutionException | UncheckedExecutionException exception) {

                disableRecipeTrie(exception);
            }
        }
        return findItemLinear(recipeInput);
    }

    public Optional<RecipeMatch> findFluidRecipeMatch(IFluidHandler fluids) {
        GasStack gasStack = gasSource.getGasStack();
        if (gasStack.isEmpty() || fluids.getTanks() <= 0) {
            return Optional.empty();
        }

        if (!AirtightRecipeTrieFinder.hasFailed(RECIPE_CACHE_KEY, level)) {
            try {
                return findFluidInTrie(fluids);
            }
            catch (ExecutionException | UncheckedExecutionException exception) {

                disableRecipeTrie(exception);
            }
        }
        return findFluidLinear(fluids);
    }

    public Optional<RecipeMatch> findBasinRecipeMatch(IItemHandler items, IFluidHandler fluids) {
        GasStack gasStack = gasSource.getGasStack();
        if (gasStack.isEmpty()) {
            return Optional.empty();
        }

        if (!AirtightRecipeTrieFinder.hasFailed(RECIPE_CACHE_KEY, level)) {
            try {
                return findBasinInTrie(items, fluids);
            }
            catch (ExecutionException | UncheckedExecutionException exception) {

                disableRecipeTrie(exception);
            }
        }
        return findBasinLinear(items, fluids);
    }

    public record RecipeMatch(GasInjectionRecipe recipe, boolean sequencedAssembly) {}

    private Optional<RecipeMatch> findItemInTrie(ItemStack itemStack, SingleRecipeInput input) throws ExecutionException {
        GasStack gasStack = gasSource.getGasStack();
        AirtightRecipeTrie<?> recipeTrie = getRecipeTrie();
        Set<AbstractVariant> lookupVariants = new HashSet<>();
        lookupVariants.add(new AbstractItem(itemStack.getItem()));
        lookupVariants.add(new AbstractGas(gasStack.getGasType()));
        for (Recipe<?> candidateRecipe : recipeTrie.lookup(lookupVariants)) {
            if (!(candidateRecipe instanceof GasInjectionRecipe injectionRecipe) || !injectionRecipe.canProcessOnBelt() || !injectionRecipe.matches(input, level) || !injectionRecipe.matchesGas(gasSource)) {
                continue;
            }

            return Optional.of(new RecipeMatch(injectionRecipe, false));
        }

        return Optional.empty();
    }

    private Optional<RecipeMatch> findBasinInTrie(IItemHandler items, IFluidHandler fluids) throws ExecutionException {
        AirtightRecipeTrie<?> recipeTrie = getRecipeTrie();
        Set<AbstractVariant> lookupVariants = AirtightRecipeTrie.getVariants(items, fluids, gasSource);
        for (Recipe<?> candidateRecipe : recipeTrie.lookup(lookupVariants)) {
            if (!(candidateRecipe instanceof GasInjectionRecipe injectionRecipe) || !injectionRecipe.matchesBasinInput(items, fluids) || !injectionRecipe.matchesGas(gasSource)) {
                continue;
            }

            return Optional.of(new RecipeMatch(injectionRecipe, false));
        }

        return Optional.empty();
    }

    private Optional<RecipeMatch> findFluidInTrie(IFluidHandler fluids) throws ExecutionException {
        GasStack gasStack = gasSource.getGasStack();
        AirtightRecipeTrie<?> recipeTrie = getRecipeTrie();
        Set<AbstractVariant> lookupVariants = new HashSet<>();
        for (int tankIndex = 0; tankIndex < fluids.getTanks(); tankIndex++) {
            FluidStack fluidStack = fluids.getFluidInTank(tankIndex);
            if (fluidStack.isEmpty()) {
                continue;
            }

            lookupVariants.add(new AbstractFluid(fluidStack.getFluid()));
        }
        if (lookupVariants.isEmpty()) {
            return Optional.empty();
        }

        lookupVariants.add(new AbstractGas(gasStack.getGasType()));
        for (Recipe<?> candidateRecipe : recipeTrie.lookup(lookupVariants)) {
            if (!(candidateRecipe instanceof GasInjectionRecipe injectionRecipe) || !injectionRecipe.isFluidInjection() || !injectionRecipe.matchesFluid(fluids) || !injectionRecipe.matchesGas(gasSource)) {
                continue;
            }

            return Optional.of(new RecipeMatch(injectionRecipe, false));
        }

        return Optional.empty();
    }

    private AirtightRecipeTrie<?> getRecipeTrie() throws ExecutionException {
        return AirtightRecipeTrieFinder.get(RECIPE_CACHE_KEY, level, recipeHolder -> recipeHolder.value() instanceof GasInjectionRecipe);
    }

    private Optional<RecipeMatch> findItemLinear(SingleRecipeInput input) {
        for (RecipeHolder<? extends Recipe<?>> recipeHolder : RecipeFinder.get(RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof GasInjectionRecipe)) {
            if (!(recipeHolder.value() instanceof GasInjectionRecipe injectionRecipe) || !injectionRecipe.canProcessOnBelt() || !injectionRecipe.matches(input, level) || !injectionRecipe.matchesGas(gasSource)) {
                continue;
            }

            return Optional.of(new RecipeMatch(injectionRecipe, false));
        }

        return Optional.empty();
    }

    private Optional<RecipeMatch> findBasinLinear(IItemHandler items, IFluidHandler fluids) {
        for (RecipeHolder<? extends Recipe<?>> recipeHolder : RecipeFinder.get(RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof GasInjectionRecipe)) {
            if (!(recipeHolder.value() instanceof GasInjectionRecipe injectionRecipe) || !injectionRecipe.matchesBasinInput(items, fluids) || !injectionRecipe.matchesGas(gasSource)) {
                continue;
            }

            return Optional.of(new RecipeMatch(injectionRecipe, false));
        }

        return Optional.empty();
    }

    private Optional<RecipeMatch> findFluidLinear(IFluidHandler fluids) {
        for (RecipeHolder<? extends Recipe<?>> recipeHolder : RecipeFinder.get(RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof GasInjectionRecipe)) {
            if (!(recipeHolder.value() instanceof GasInjectionRecipe injectionRecipe) || !injectionRecipe.isFluidInjection() || !injectionRecipe.matchesFluid(fluids) || !injectionRecipe.matchesGas(gasSource)) {
                continue;
            }

            return Optional.of(new RecipeMatch(injectionRecipe, false));
        }

        return Optional.empty();
    }

    private void disableRecipeTrie(Exception exception) {
        if (!AirtightRecipeTrieFinder.recordFailure(RECIPE_CACHE_KEY, level)) {
            return;
        }

        CCBAPI.LOGGER.error("Failed to build the gas injection recipe trie; falling back to a linear recipe search until recipes are reloaded.", exception);
    }

    @Contract(pure = true)
    private Predicate<RecipeHolder<GasInjectionRecipe>> matchItemAndGas(SingleRecipeInput input) {
        return recipeHolder -> {
            GasInjectionRecipe recipe = recipeHolder.value();
            return recipe.canProcessOnBelt() && recipe.matches(input, level) && recipe.matchesGas(gasSource);
        };
    }
}
