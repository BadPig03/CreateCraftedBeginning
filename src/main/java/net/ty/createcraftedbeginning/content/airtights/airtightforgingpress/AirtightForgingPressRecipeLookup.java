package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.handler.GasHandler;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPlanner;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipe;
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
public final class AirtightForgingPressRecipeLookup {
    private static final Object FORGING_PRESS_RECIPE_CACHE_KEY = new Object();
    private static final Object AUTOMATIC_PRESSING_RECIPE_CACHE_KEY = new Object();
    private static final Object AUTOMATIC_CRUSHING_RECIPE_CACHE_KEY = new Object();
    private static final Object AUTOMATIC_SMITHING_RECIPE_CACHE_KEY = new Object();
    private static final AtomicLong RECIPE_CACHE_VERSION = new AtomicLong();

    private AirtightForgingPressRecipeLookup() {
    }

    public static void invalidateRecipeCaches() {
        AirtightRecipeTrieFinder.invalidateFailures(FORGING_PRESS_RECIPE_CACHE_KEY);
        RECIPE_CACHE_VERSION.incrementAndGet();
    }

    public static boolean isAllowedAutomaticPressingRecipe(RecipeHolder<? extends Recipe<?>> holder) {
        return holder.value() instanceof PressingRecipe && !AllRecipeTypes.shouldIgnoreInAutomation(holder);
    }

    public static boolean isAllowedAutomaticCrushingRecipe(RecipeHolder<? extends Recipe<?>> holder) {
        Recipe<?> recipe = holder.value();
        return recipe instanceof CrushingRecipe && recipe.getType() == AllRecipeTypes.CRUSHING.getType() && !AllRecipeTypes.shouldIgnoreInAutomation(holder);
    }

    public static boolean isAllowedAutomaticSmithingRecipe(RecipeHolder<? extends Recipe<?>> holder) {
        Recipe<?> recipe = holder.value();
        return recipe.getType() == RecipeType.SMITHING && recipe instanceof SmithingRecipe && !AllRecipeTypes.shouldIgnoreInAutomation(holder);
    }

    @Internal
    public static Optional<ForgingPressRecipe> getMatchingRecipe(AirtightForgingPressBlockEntity press) {
        if (!press.hasRecipeInputs()) {
            return Optional.empty();
        }

        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        if (AirtightRecipeTrieFinder.hasFailed(FORGING_PRESS_RECIPE_CACHE_KEY, level)) {
            return findMatchingLinearRecipe(press, level);
        }

        try {
            IItemHandler availableItems = press.getRecipeInputCapability();
            IFluidHandler availableFluids = press.getFluidCapability();
            GasHandler availableGases = press.getGasCapability();
            AirtightRecipeTrie<?> recipeTrie = AirtightRecipeTrieFinder.get(FORGING_PRESS_RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof ForgingPressRecipe);
            Set<AbstractVariant> availableVariants = AirtightRecipeTrie.getVariants(availableItems, availableFluids, availableGases);
            for (Recipe<?> candidateRecipe : recipeTrie.lookup(availableVariants)) {
                if (!(candidateRecipe instanceof ForgingPressRecipe forgingRecipe) || !new ForgingPressCraftPlanner(press, forgingRecipe).matches()) {
                    continue;
                }

                return Optional.of(forgingRecipe);
            }

            return Optional.empty();
        }
        catch (ExecutionException | UncheckedExecutionException exception) {
            if (AirtightRecipeTrieFinder.recordFailure(FORGING_PRESS_RECIPE_CACHE_KEY, level)) {
                CCBAPI.LOGGER.error("Failed to build the airtight forging press recipe trie; falling back to a linear recipe search until recipes are reloaded.", exception);
            }
        }

        return findMatchingLinearRecipe(press, level);
    }

    @Internal
    public static Optional<AirtightForgingPressPressingRecipe> getMatchingPressingRecipe(AirtightForgingPressBlockEntity press) {
        Level level = press.getLevel();
        if (level == null || !press.getPressHeadInventory().isEmpty() || !press.getAdditionInventory().isEmpty()) {
            return Optional.empty();
        }

        ItemStack inputStack = press.getInputInventory().getStackInSlot(0);
        if (inputStack.isEmpty()) {
            return Optional.empty();
        }

        List<RecipeHolder<PressingRecipe>> assemblySteps = SequencedAssemblyRecipe.getRecipes(level, inputStack, AllRecipeTypes.PRESSING.getType(), PressingRecipe.class, holder -> !AllRecipeTypes.shouldIgnoreInAutomation(holder));
        for (RecipeHolder<PressingRecipe> step : assemblySteps) {
            Optional<RecipeHolder<?>> assemblyHolder = level.getRecipeManager().byKey(step.id());
            if (assemblyHolder.isEmpty() || !(assemblyHolder.get().value() instanceof SequencedAssemblyRecipe assemblyRecipe)) {
                continue;
            }

            AirtightForgingPressPressingRecipe pressingRecipe = new AirtightForgingPressPressingRecipe(step.value(), inputStack, new RecipeHolder<>(step.id(), assemblyRecipe));
            if (!AirtightForgingPressAutomationPlanner.canApplyPressingRecipe(press, pressingRecipe, inputStack)) {
                continue;
            }

            return Optional.of(pressingRecipe);
        }

        if (!assemblySteps.isEmpty() || inputStack.has(AllDataComponents.SEQUENCED_ASSEMBLY)) {
            return Optional.empty();
        }

        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(AUTOMATIC_PRESSING_RECIPE_CACHE_KEY, level, AirtightForgingPressRecipeLookup::isAllowedAutomaticPressingRecipe)) {
            if (!(holder.value() instanceof PressingRecipe recipe)) {
                continue;
            }

            AirtightForgingPressPressingRecipe pressingRecipe = new AirtightForgingPressPressingRecipe(recipe, inputStack, null);
            if (!AirtightForgingPressAutomationPlanner.canApplyPressingRecipe(press, pressingRecipe, inputStack)) {
                continue;
            }

            return Optional.of(pressingRecipe);
        }

        return Optional.empty();
    }

    @Internal
    public static Optional<RecipeHolder<CrushingRecipe>> getMatchingCrushingRecipe(AirtightForgingPressBlockEntity press) {
        Level level = press.getLevel();
        if (level == null || !AirtightForgingPressCrushing.canProcess(press)) {
            return Optional.empty();
        }

        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(AUTOMATIC_CRUSHING_RECIPE_CACHE_KEY, level, AirtightForgingPressRecipeLookup::isAllowedAutomaticCrushingRecipe)) {
            if (!(holder.value() instanceof CrushingRecipe recipe)) {
                continue;
            }

            RecipeHolder<CrushingRecipe> crushingRecipe = new RecipeHolder<>(holder.id(), recipe);
            if (!AirtightForgingPressCrushing.canApply(press, crushingRecipe)) {
                continue;
            }

            return Optional.of(crushingRecipe);
        }
        return Optional.empty();
    }

    static long getRecipeCacheVersion() {
        return RECIPE_CACHE_VERSION.get();
    }

    static Optional<RecipeHolder<SmithingRecipe>> getMatchingSmithingRecipe(AirtightForgingPressBlockEntity press) {
        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        SmithingRecipeInput smithingInput = AirtightForgingPressAutomationPlanner.createSmithingInput(press);
        if (smithingInput.template().isEmpty() || smithingInput.base().isEmpty() || smithingInput.addition().isEmpty()) {
            return Optional.empty();
        }

        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(AUTOMATIC_SMITHING_RECIPE_CACHE_KEY, level, AirtightForgingPressRecipeLookup::isAllowedAutomaticSmithingRecipe)) {
            if (!(holder.value() instanceof SmithingRecipe smithingRecipe) || !AirtightForgingPressAutomationPlanner.canApplySmithingRecipe(press, smithingRecipe, smithingInput)) {
                continue;
            }

            return Optional.of(new RecipeHolder<>(holder.id(), smithingRecipe));
        }

        return Optional.empty();
    }

    private static Optional<ForgingPressRecipe> findMatchingLinearRecipe(AirtightForgingPressBlockEntity press, Level level) {
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(FORGING_PRESS_RECIPE_CACHE_KEY, level, recipeHolder -> recipeHolder.value() instanceof ForgingPressRecipe)) {
            if (holder.value() instanceof ForgingPressRecipe forgingRecipe && new ForgingPressCraftPlanner(press, forgingRecipe).matches()) {
                return Optional.of(forgingRecipe);
            }
        }
        return Optional.empty();
    }
}
