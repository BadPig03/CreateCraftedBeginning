package net.ty.createcraftedbeginning.recipe;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.CCBAPI;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.foundation.lang.CCBLang;
import net.ty.createcraftedbeginning.recipe.gas.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractFluid;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractGas;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractItem;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;
import net.ty.createcraftedbeginning.recipe.trie.IAirtightWithGasRecipe;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Predicate;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas, IAirtightWithGasRecipe {
    private static final Object RECIPE_CACHE_KEY = new Object();

    public GasInjectionRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.GAS_INJECTION, params);
    }

    public static Optional<GasInjectionRecipe> findRecipe(Level level, ItemStack itemStack, GasStack gasStack) {
        return findRecipeMatch(level, itemStack, gasStack).map(RecipeMatch::recipe);
    }

    public static Optional<RecipeMatch> findRecipeMatch(Level level, ItemStack itemStack, GasStack gasStack) {
        if (itemStack.isEmpty() || gasStack.isEmpty()) {
            return Optional.empty();
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndGas(level, gasStack, input));
        if (assemblyRecipe.isPresent()) {
            return Optional.of(new RecipeMatch(assemblyRecipe.get().value(), true));
        }

        if (!AirtightWithGasRecipeTrieFinder.hasFailed(RECIPE_CACHE_KEY, level)) {
            try {
                return findItemInTrie(level, itemStack, gasStack, input);
            } catch (ExecutionException | UncheckedExecutionException exception) {
                disableRecipeTrie(level, exception);
            }
        }
        return findItemLinear(level, gasStack, input);
    }

    public static Optional<RecipeMatch> findFluidRecipeMatch(Level level, IFluidHandler fluids, GasStack gasStack) {
        if (gasStack.isEmpty() || fluids.getTanks() <= 0) {
            return Optional.empty();
        }

        if (!AirtightWithGasRecipeTrieFinder.hasFailed(RECIPE_CACHE_KEY, level)) {
            try {
                return findFluidInTrie(level, fluids, gasStack);
            } catch (ExecutionException | UncheckedExecutionException exception) {
                disableRecipeTrie(level, exception);
            }
        }
        return findFluidLinear(level, fluids, gasStack);
    }

    private static Optional<RecipeMatch> findItemInTrie(Level level, ItemStack itemStack, GasStack gasStack, SingleRecipeInput input) throws ExecutionException {
        AirtightWithGasRecipeTrie<?> trie = getRecipeTrie(level);
        Set<AbstractVariant> variants = new HashSet<>();
        variants.add(new AbstractItem(itemStack.getItem()));
        variants.add(new AbstractGas(gasStack.getGasType()));
        for (Recipe<?> candidate : trie.lookup(variants)) {
            if (!(candidate instanceof GasInjectionRecipe recipe) || !recipe.matches(input, level) || !recipe.matchesGas(gasStack)) {
                continue;
            }

            return Optional.of(new RecipeMatch(recipe, false));
        }
        return Optional.empty();
    }

    private static Optional<RecipeMatch> findFluidInTrie(Level level, IFluidHandler fluids, GasStack gasStack) throws ExecutionException {
        AirtightWithGasRecipeTrie<?> trie = getRecipeTrie(level);
        Set<AbstractVariant> variants = new HashSet<>();
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack stack = fluids.getFluidInTank(tank);
            if (!stack.isEmpty()) {
                variants.add(new AbstractFluid(stack.getFluid()));
            }
        }
        if (variants.isEmpty()) {
            return Optional.empty();
        }

        variants.add(new AbstractGas(gasStack.getGasType()));
        for (Recipe<?> candidate : trie.lookup(variants)) {
            if (!(candidate instanceof GasInjectionRecipe recipe) || !recipe.isFluidInjection() || !recipe.matchesFluid(fluids) || !recipe.matchesGas(gasStack)) {
                continue;
            }

            return Optional.of(new RecipeMatch(recipe, false));
        }
        return Optional.empty();
    }

    private static AirtightWithGasRecipeTrie<?> getRecipeTrie(Level level) throws ExecutionException {
        return AirtightWithGasRecipeTrieFinder.get(RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof GasInjectionRecipe);
    }

    private static Optional<RecipeMatch> findItemLinear(Level level, GasStack gasStack, SingleRecipeInput input) {
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(RECIPE_CACHE_KEY, level, recipe -> recipe.value() instanceof GasInjectionRecipe)) {
            if (!(holder.value() instanceof GasInjectionRecipe recipe) || !recipe.matches(input, level) || !recipe.matchesGas(gasStack)) {
                continue;
            }

            return Optional.of(new RecipeMatch(recipe, false));
        }
        return Optional.empty();
    }

    private static Optional<RecipeMatch> findFluidLinear(Level level, IFluidHandler fluids, GasStack gasStack) {
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(RECIPE_CACHE_KEY, level, recipe -> recipe.value() instanceof GasInjectionRecipe)) {
            if (!(holder.value() instanceof GasInjectionRecipe recipe) || !recipe.isFluidInjection() || !recipe.matchesFluid(fluids) || !recipe.matchesGas(gasStack)) {
                continue;
            }

            return Optional.of(new RecipeMatch(recipe, false));
        }
        return Optional.empty();
    }

    private static void disableRecipeTrie(Level level, Exception exception) {
        if (!AirtightWithGasRecipeTrieFinder.recordFailure(RECIPE_CACHE_KEY, level)) {
            return;
        }

        CCBAPI.LOGGER.error("Failed to build the gas injection recipe trie; falling back to a linear recipe search until recipes are reloaded", exception);
    }

    public static void invalidateRecipeCaches() {
        AirtightWithGasRecipeTrieFinder.invalidateFailures(RECIPE_CACHE_KEY);
    }

    public static Optional<ItemStack> getResultItem(Level level, ItemStack itemStack, GasStack gasStack) {
        return findRecipe(level, itemStack, gasStack).map(recipe -> recipe.rollFirstResult(level));
    }

    @Contract(pure = true)
    private static Predicate<RecipeHolder<GasInjectionRecipe>> matchItemAndGas(Level level, GasStack gasStack, SingleRecipeInput input) {
        return holder -> holder.value().matches(input, level) && holder.value().matchesGas(gasStack);
    }

    private boolean matchesGas(GasStack gasStack) {
        return getGasIngredient().ingredient().test(gasStack);
    }

    public boolean matchesFluid(IFluidHandler fluids) {
        if (!isFluidInjection()) {
            return false;
        }

        SizedFluidIngredient ingredient = getFluidIngredient();
        int remaining = ingredient.amount();
        for (int tank = 0; tank < fluids.getTanks() && remaining > 0; tank++) {
            FluidStack stack = fluids.getFluidInTank(tank);
            if (stack.isEmpty() || !ingredient.test(stack)) {
                continue;
            }

            remaining -= Math.min(remaining, stack.getAmount());
        }
        return remaining <= 0;
    }

    public boolean isFluidInjection() {
        return ingredients.isEmpty() && results.isEmpty() && fluidIngredients.size() == 1 && fluidResults.size() == 1;
    }

    public ItemStack rollFirstResult(Level level) {
        return rollResults(level.random).stream().findFirst().orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return fluidIngredients.isEmpty() && !ingredients.isEmpty() && !input.isEmpty() && ingredients.getFirst().test(input.getItem(0));
    }

    public SizedGasIngredient getGasIngredient() {
        if (gasIngredients.isEmpty()) {
            throw new IllegalStateException("Gas Injection Recipe has no gas ingredient!");
        }

        return gasIngredients.getFirst();
    }

    public SizedFluidIngredient getFluidIngredient() {
        if (fluidIngredients.isEmpty()) {
            throw new IllegalStateException("Gas Injection Recipe has no fluid ingredient!");
        }

        return fluidIngredients.getFirst();
    }

    public FluidStack getFluidResult() {
        if (fluidResults.isEmpty()) {
            return FluidStack.EMPTY;
        }
        return fluidResults.getFirst();
    }

    public Ingredient getIngredient() {
        return ingredients.isEmpty() ? Ingredient.EMPTY : ingredients.getFirst();
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
    protected int getMaxFluidInputCount() {
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
    protected void validateSpecial(List<String> errors) {
        if (gasIngredients.size() != 1) {
            errors.add("Gas injection recipes require exactly one gas ingredient.");
        }

        boolean hasFluidMedium = !fluidIngredients.isEmpty() || !fluidResults.isEmpty();
        if (!hasFluidMedium) {
            return;
        }

        if (!ingredients.isEmpty() || !results.isEmpty()) {
            errors.add("Gas injection recipes cannot mix item and fluid inputs or outputs.");
        }
        if (fluidIngredients.size() != 1 || fluidResults.size() != 1) {
            errors.add("Fluid gas injection recipes require exactly one fluid input and one fluid output.");
            return;
        }

        if (fluidIngredients.getFirst().amount() <= 0) {
            errors.add("Fluid gas injection recipe input amount must be greater than zero.");
        }
        if (!fluidResults.getFirst().isEmpty() && fluidResults.getFirst().getAmount() > 0) {
            return;
        }

        errors.add("Fluid gas injection recipe output must not be empty.");
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        String gasName = gasIngredients.getFirst().getFirstGas().getHoverName().getString();
        return CCBLang.translateDirect("recipe.assembly.gas_injection_injecting_gas", gasName);
    }

    @Override
    public void addAssemblyGasIngredients(List<SizedGasIngredient> list) {
        list.add(getGasIngredient());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(BuiltInRegistries.BLOCK.get(CCBAPI.asResource("gas_injection_chamber")));
    }

    public record RecipeMatch(GasInjectionRecipe recipe, boolean sequencedAssembly) {}
}
