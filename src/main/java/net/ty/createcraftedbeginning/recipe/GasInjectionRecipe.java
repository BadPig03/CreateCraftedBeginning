package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.gases.GasCapabilities.GasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory.AssemblyInjecting;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterContainerContents;
import net.ty.createcraftedbeginning.content.airtights.gascanister.GasCanisterUtils;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBlockEntity;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class GasInjectionRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas {
    public GasInjectionRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.GAS_INJECTION, params);
    }

    public static ItemStack getResultItem(Level level, ItemStack itemStack, GasStack gasStack) {
        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndGas(level, gasStack, input));
        if (assemblyRecipe.isPresent()) {
            SizedGasIngredient requiredGas = assemblyRecipe.get().value().getGasIngredient();
            GasStack requiredGasStack = requiredGas.getFirstGas();
            if (GasStack.isSameGasSameComponents(requiredGasStack, gasStack)) {
                List<ItemStack> results = assemblyRecipe.get().value().rollResults(level.random);
                return results.getFirst();
            }
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();
            if (!recipe.getIngredient().test(itemStack)) {
                continue;
            }

            SizedGasIngredient gasIngredient = recipe.getGasIngredient();
            GasStack ingredientGasStack = gasIngredient.getFirstGas();
            if (!GasStack.isSameGasSameComponents(ingredientGasStack, gasStack)) {
                continue;
            }

            return recipe.rollResults(level.random).getFirst();
        }

        return ItemStack.EMPTY;
    }

    public static long getRequiredGasAmount(Level level, ItemStack itemStack, GasStack gasStack) {
        if (GasCanisterUtils.isCanisterInjectable(itemStack, gasStack) && itemStack.getCapability(GasHandler.ITEM) instanceof GasCanisterContainerContents canisterContents) {
            long maxCapacity = GasInjectionChamberBlockEntity.getMaxCapacity();
            return Math.min(maxCapacity, canisterContents.getTankCapacity(0) - canisterContents.getGasInTank(0).getAmount());
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndGas(level, gasStack, input));
        if (assemblyRecipe.isPresent()) {
            SizedGasIngredient requiredGas = assemblyRecipe.get().value().getGasIngredient();
            GasStack requiredGasStack = requiredGas.getFirstGas();
            if (GasStack.isSameGasSameComponents(requiredGasStack, gasStack)) {
                return requiredGas.amount();
            }
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();
            if (!recipe.getIngredient().test(itemStack)) {
                continue;
            }

            SizedGasIngredient gasIngredient = recipe.getGasIngredient();
            GasStack ingredientGasStack = gasIngredient.getFirstGas();
            if (!GasStack.isSameGasSameComponents(ingredientGasStack, gasStack)) {
                continue;
            }

            return ingredientGasStack.getAmount();
        }

        return -1;
    }

    public static boolean isItemInvalidForInjection(@Nullable Level level, ItemStack itemStack, GasStack gasStack) {
        if (level == null || GasCanisterUtils.isCanisterInjectable(itemStack, gasStack)) {
            return false;
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyWithGasRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class);
        if (assemblyRecipe.isPresent()) {
            return false;
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();
            Ingredient ingredient = recipe.getIngredient();
            if (ingredient.test(itemStack)) {
                return false;
            }
        }

        return true;
    }

    @Contract(pure = true)
    private static Predicate<RecipeHolder<GasInjectionRecipe>> matchItemAndGas(Level level, GasStack gasStack, SingleRecipeInput input) {
        return r -> r.value().matches(input, level) && GasStack.isSameGasSameComponents(r.value().getGasIngredient().getFirstGas(), gasStack);
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
        return CCBLang.translateDirect("recipe.assembly.gas_injection_injecting_gas", gasIngredients.getFirst().getFirstGas().getHoverName().getString());
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public void addAssemblyGasIngredients(List<SizedGasIngredient> list) {
        list.add(getGasIngredient());
    }

    @Override
    public Supplier<Supplier<SequencedAssemblyWithGasSubCategory>> getJEISubCategory() {
        return () -> AssemblyInjecting::new;
    }
}