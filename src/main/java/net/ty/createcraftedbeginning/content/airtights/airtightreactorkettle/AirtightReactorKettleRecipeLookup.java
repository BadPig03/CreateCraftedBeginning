package net.ty.createcraftedbeginning.content.airtights.airtightreactorkettle;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.MechanicalPressBlockEntity;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.recipe.ReactorKettleBrewingRecipes;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ReactorKettleMixingRecipe;
import net.ty.createcraftedbeginning.recipe.ReactorKettleRecipe;
import net.ty.createcraftedbeginning.recipe.gas.GasRecipeRequirement;
import net.ty.createcraftedbeginning.recipe.temperature.TemperatureMatching;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightRecipeTrieFinder;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightReactorKettleRecipeLookup {
    private static final Object CRAFTING_RECIPE_CACHE_KEY = new Object();
    private static final Object MIXING_RECIPE_CACHE_KEY = new Object();
    private static final Object REACTOR_KETTLE_RECIPE_CACHE_KEY = new Object();
    private static final AtomicLong RECIPE_CACHE_VERSION = new AtomicLong();

    private AirtightReactorKettleRecipeLookup() {
    }

    public static void invalidateRecipeCaches() {
        ReactorKettleBrewingRecipes.invalidateCaches();
        AirtightRecipeTrieFinder.invalidateFailures(REACTOR_KETTLE_RECIPE_CACHE_KEY);
        AirtightRecipeTrieFinder.invalidateFailures(MIXING_RECIPE_CACHE_KEY);
        RECIPE_CACHE_VERSION.incrementAndGet();
    }

    @Internal
    public static Optional<ReactorKettleRecipe> getMatchingRecipe(AirtightReactorKettleBlockEntity kettle) {
        if (kettle.isEmpty()) {
            return Optional.empty();
        }

        Level level = kettle.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        if (AirtightRecipeTrieFinder.hasFailed(REACTOR_KETTLE_RECIPE_CACHE_KEY, level)) {
            return findMatchingLinearRecipe(kettle, level);
        }

        try {
            IItemHandler availableItems = kettle.getAvailableItems();
            IFluidHandler availableFluids = kettle.getAvailableFluids();
            GasHandler availableGases = kettle.getAvailableGases();
            AirtightRecipeTrie<?> trie = AirtightRecipeTrieFinder.get(REACTOR_KETTLE_RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof ReactorKettleRecipe);
            Set<AbstractVariant> availableVariants = AirtightRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            ReactorKettleRecipe bestMatch = null;
            RecipeMatchPriority bestPriority = null;
            for (Recipe<?> candidate : trie.lookup(availableVariants)) {
                if (!(candidate instanceof ReactorKettleRecipe recipe) || !new ReactorKettleCraftPlanner(kettle, recipe).matches()) {
                    continue;
                }

                RecipeMatchPriority matchPriority = getMatchPriority(kettle, recipe);
                if (bestPriority != null && matchPriority.compareTo(bestPriority) <= 0) {
                    continue;
                }

                bestMatch = recipe;
                bestPriority = matchPriority;
            }
            return Optional.ofNullable(bestMatch);
        }
        catch (ExecutionException | UncheckedExecutionException exception) {
            if (AirtightRecipeTrieFinder.recordFailure(REACTOR_KETTLE_RECIPE_CACHE_KEY, level)) {
                CCBAPI.LOGGER.error("Failed to build the airtight reactor kettle recipe trie; falling back to a linear recipe search until recipes are reloaded.", exception);
            }
        }

        return findMatchingLinearRecipe(kettle, level);
    }

    @Internal
    public static Optional<ReactorKettleRecipe> getMatchingMixingRecipe(AirtightReactorKettleBlockEntity kettle) {
        Level level = kettle.getLevel();
        if (level == null || kettle.isEmpty() || !CCBConfig.server().machines.airtightReactorKettle.enableAutomaticMixingRecipes.get()) {
            return Optional.empty();
        }

        if (!AirtightRecipeTrieFinder.hasFailed(MIXING_RECIPE_CACHE_KEY, level)) {
            try {
                AirtightRecipeTrie<?> trie = AirtightRecipeTrieFinder.get(MIXING_RECIPE_CACHE_KEY, level, ReactorKettleMixingRecipe::isSupported, holder -> ReactorKettleMixingRecipe.convert(holder).value());
                Set<AbstractVariant> variants = AirtightRecipeTrie.getVariants(kettle.getAvailableItems(), kettle.getAvailableFluids(), kettle.getAvailableGases());
                return findMatchingMixingRecipe(kettle, trie.lookup(variants));
            }
            catch (ExecutionException | UncheckedExecutionException exception) {
                if (AirtightRecipeTrieFinder.recordFailure(MIXING_RECIPE_CACHE_KEY, level)) {
                    CCBAPI.LOGGER.error("Failed to build the airtight reactor kettle mixing recipe trie; falling back to a linear recipe search until recipes are reloaded.", exception);
                }
            }
        }

        List<ReactorKettleRecipe> recipes = RecipeFinder.get(MIXING_RECIPE_CACHE_KEY, level, ReactorKettleMixingRecipe::isSupported).stream().map(holder -> ReactorKettleMixingRecipe.convert(holder).value()).toList();
        return findMatchingMixingRecipe(kettle, recipes);
    }

    @Internal
    public static Optional<ReactorKettleRecipe> getMatchingBrewingRecipe(AirtightReactorKettleBlockEntity kettle) {
        Level level = kettle.getLevel();
        if (level == null || kettle.isEmpty() || !CCBConfig.server().machines.airtightReactorKettle.enableAutomaticBrewingRecipes.get()) {
            return Optional.empty();
        }

        return findMatchingMixingRecipe(kettle, ReactorKettleBrewingRecipes.getCandidates(level, kettle.getAvailableItems()));
    }

    static long getRecipeCacheVersion() {
        return RECIPE_CACHE_VERSION.get();
    }

    static Optional<RecipeHolder<CraftingRecipe>> getMatchingCraftingRecipe(AirtightReactorKettleBlockEntity kettle) {
        Level level = kettle.getLevel();
        if (level == null || kettle.getInventories().getFirst().isEmpty()) {
            return Optional.empty();
        }

        List<RecipeHolder<? extends Recipe<?>>> candidates = RecipeFinder.get(CRAFTING_RECIPE_CACHE_KEY, level, holder -> {
            if (AllRecipeTypes.shouldIgnoreInAutomation(holder)) {
                return false;
            }

            Recipe<?> recipe = holder.value();
            if (!(recipe instanceof ShapelessRecipe)) {
                return false;
            }

            int ingredientCount = 0;
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.isEmpty() || ++ingredientCount <= 1) {
                    continue;
                }

                break;
            }

            return ingredientCount > 1 && !MechanicalPressBlockEntity.canCompress(recipe);
        });
        for (RecipeHolder<? extends Recipe<?>> holder : candidates) {
            if (!(holder.value() instanceof CraftingRecipe craftingRecipe)) {
                continue;
            }

            ItemStack preview = craftingRecipe.getResultItem(level.registryAccess());
            if (preview.isEmpty() || !kettle.testRecipeFilter(preview) || !AirtightReactorKettleMixingPlanner.matches(kettle, craftingRecipe)) {
                continue;
            }

            return Optional.of(new RecipeHolder<>(holder.id(), craftingRecipe));
        }

        return Optional.empty();
    }

    private static Optional<ReactorKettleRecipe> findMatchingLinearRecipe(AirtightReactorKettleBlockEntity kettle, Level level) {
        ReactorKettleRecipe bestMatch = null;
        RecipeMatchPriority bestPriority = null;
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(REACTOR_KETTLE_RECIPE_CACHE_KEY, level, recipe -> recipe.value() instanceof ReactorKettleRecipe)) {
            if (!(holder.value() instanceof ReactorKettleRecipe recipe) || !new ReactorKettleCraftPlanner(kettle, recipe).matches()) {
                continue;
            }

            RecipeMatchPriority matchPriority = getMatchPriority(kettle, recipe);
            if (bestPriority != null && matchPriority.compareTo(bestPriority) <= 0) {
                continue;
            }

            bestMatch = recipe;
            bestPriority = matchPriority;
        }
        return Optional.ofNullable(bestMatch);
    }

    private static Optional<ReactorKettleRecipe> findMatchingMixingRecipe(AirtightReactorKettleBlockEntity kettle, List<? extends Recipe<?>> recipes) {
        ReactorKettleRecipe bestMatch = null;
        RecipeMatchPriority bestPriority = null;
        for (Recipe<?> candidate : recipes) {
            if (!(candidate instanceof ReactorKettleRecipe recipe) || !new ReactorKettleCraftPlanner(kettle, recipe).matches()) {
                continue;
            }

            RecipeMatchPriority priority = getMatchPriority(kettle, recipe);
            if (bestPriority != null && priority.compareTo(bestPriority) <= 0) {
                continue;
            }

            bestMatch = recipe;
            bestPriority = priority;
        }
        return Optional.ofNullable(bestMatch);
    }

    private static RecipeMatchPriority getMatchPriority(AirtightReactorKettleBlockEntity kettle, ReactorKettleRecipe recipe) {
        int temperaturePriority = TemperatureMatching.getMatchPriority(recipe.getTemperatureMatching(), recipe.getTemperatureCondition(), kettle.getRecipeTemperature());
        long minimumPressurePriority = 0;
        for (GasRecipeRequirement gasRequirement : recipe.getGasRequirements()) {
            minimumPressurePriority = Math.max(minimumPressurePriority, gasRequirement.pressure().minimumPressurePaOrVacuum());
        }
        return new RecipeMatchPriority(temperaturePriority, minimumPressurePriority);
    }

    private record RecipeMatchPriority(int temperaturePriority, long minimumPressurePriority) implements Comparable<RecipeMatchPriority> {
        @Override
        public int compareTo(RecipeMatchPriority other) {
            int temperatureComparison = Integer.compare(temperaturePriority, other.temperaturePriority);
            if (temperatureComparison != 0) {
                return temperatureComparison;
            }

            return Long.compare(minimumPressurePriority, other.minimumPressurePriority);
        }
    }
}
