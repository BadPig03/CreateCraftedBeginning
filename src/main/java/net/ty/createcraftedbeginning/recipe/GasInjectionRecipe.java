package net.ty.createcraftedbeginning.recipe;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.simibubi.create.foundation.recipe.RecipeFinder;
import net.minecraft.MethodsReturnNonnullByDefault;
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
import net.ty.createcraftedbeginning.CreateCraftedBeginning;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory.AssemblyInjecting;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractGas;
import net.ty.createcraftedbeginning.recipe.trie.AbstractVariant.AbstractItem;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrie;
import net.ty.createcraftedbeginning.recipe.trie.AirtightWithGasRecipeTrieFinder;
import net.ty.createcraftedbeginning.recipe.trie.IAirtightWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.Contract;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas, IAirtightWithGasRecipe {
    private static final Object RECIPE_CACHE_KEY = new Object();
    private static final AtomicBoolean RECIPE_TRIE_FAILURE_LOGGED = new AtomicBoolean();

    private static volatile boolean recipeTrieDisabled;

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

        if (!recipeTrieDisabled) {
            try {
                return findInTrie(level, itemStack, gasStack, input);
            } catch (ExecutionException | UncheckedExecutionException exception) {
                recipeTrieDisabled = true;
                if (RECIPE_TRIE_FAILURE_LOGGED.compareAndSet(false, true)) {
                    CreateCraftedBeginning.LOGGER.error("Failed to build the gas injection recipe trie; falling back to a linear recipe search until recipes are reloaded", exception);
                }
            }
        }

        return findLinear(level, gasStack, input);
    }

    private static Optional<RecipeMatch> findInTrie(Level level, ItemStack itemStack, GasStack gasStack, SingleRecipeInput input) throws ExecutionException {
        AirtightWithGasRecipeTrie<?> trie = AirtightWithGasRecipeTrieFinder.get(RECIPE_CACHE_KEY, level, holder -> holder.value() instanceof GasInjectionRecipe);
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

    private static Optional<RecipeMatch> findLinear(Level level, GasStack gasStack, SingleRecipeInput input) {
        for (RecipeHolder<? extends Recipe<?>> holder : RecipeFinder.get(RECIPE_CACHE_KEY, level, recipe -> recipe.value() instanceof GasInjectionRecipe)) {
            if (!(holder.value() instanceof GasInjectionRecipe recipe) || !recipe.matches(input, level) || !recipe.matchesGas(gasStack)) {
                continue;
            }

            return Optional.of(new RecipeMatch(recipe, false));
        }
        return Optional.empty();
    }

    public static void invalidateRecipeCaches() {
        recipeTrieDisabled = false;
        RECIPE_TRIE_FAILURE_LOGGED.set(false);
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

    public ItemStack rollFirstResult(Level level) {
        return rollResults(level.random).stream().findFirst().orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredients.getFirst().test(input.getItem(0));
    }

    public SizedGasIngredient getGasIngredient() {
        if (gasIngredients.isEmpty()) {
            throw new IllegalStateException("Gas Injection Recipe has no gas ingredient!");
        }

        return gasIngredients.getFirst();
    }

    public Ingredient getIngredient() {
        return ingredients.getFirst();
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
    public Supplier<Supplier<SequencedAssemblyWithGasSubCategory>> getJEISubCategory() {
        return () -> AssemblyInjecting::new;
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get());
    }

    public record RecipeMatch(GasInjectionRecipe recipe, boolean sequencedAssembly) {}
}
