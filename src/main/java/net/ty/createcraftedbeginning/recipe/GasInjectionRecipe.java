package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.compat.jei.category.sequencedAssembly.SequencedAssemblySubCategory;
import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import com.simibubi.create.content.processing.recipe.StandardProcessingRecipe;
import com.simibubi.create.content.processing.sequenced.IAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.foundation.fluid.FluidIngredient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.compat.jei.category.CCBSequencedAssemblySubCategory;
import net.ty.createcraftedbeginning.content.compressedair.CanisterUtil;
import net.ty.createcraftedbeginning.data.CCBLang;
import net.ty.createcraftedbeginning.data.CCBTags;
import net.ty.createcraftedbeginning.registry.CCBBlocks;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import net.ty.createcraftedbeginning.util.Helpers;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class GasInjectionRecipe extends StandardProcessingRecipe<SingleRecipeInput> implements IAssemblyRecipe {
    public GasInjectionRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.GAS_INJECTION, params);
    }

    public static ItemStack getResultItem(Level level, ItemStack itemStack, FluidStack fluidStack) {
        if (level == null) {
            return ItemStack.EMPTY;
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndFluid(level, fluidStack, input));
        if (assemblyRecipe.isPresent()) {
            FluidIngredient requiredFluid = assemblyRecipe.get().value().getRequiredFluid();
            FluidStack requiredFluidStack = requiredFluid.getMatchingFluidStacks().getFirst();
            if (Helpers.isFluidTheSame(requiredFluidStack, fluidStack)) {
                List<ItemStack> results = assemblyRecipe.get().value().rollResults();
                return results.getFirst();
            }
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();

            Ingredient ingredient = recipe.getIngredientsItem();
            if (!ingredient.test(itemStack)) {
                continue;
            }

            FluidIngredient fluidIngredient = recipe.getIngredientsFluid();
            FluidStack ingredientFluidStack = fluidIngredient.getMatchingFluidStacks().getFirst();
            if (!Helpers.isFluidTheSame(ingredientFluidStack, fluidStack)) {
                continue;
            }

            return recipe.rollResults().getFirst();
        }

        return ItemStack.EMPTY;
    }

    public static int getRequiredFluidAmountForItem(Level level, ItemStack itemStack, FluidStack fluidStack) {
        if (level == null) {
            return -1;
        }

        if (fluidStack.is(CCBTags.CCBFluidTags.MEDIUM_PRESSURE_COMPRESSED_AIR.tag) && CanisterUtil.isValidCanister(itemStack, false)) {
            int airAmount = CanisterUtil.getAirUsed(itemStack);
            if (airAmount == 0) {
                return -1;
            }
            return Math.min(airAmount, 1000);
        }

        if (fluidStack.is(CCBTags.CCBFluidTags.HIGH_PRESSURE_COMPRESSED_AIR.tag) && CanisterUtil.isValidCanister(itemStack, true)) {
            int airAmount = CanisterUtil.getAirUsed(itemStack);
            if (airAmount == 0) {
                return -1;
            }
            return Math.min(airAmount, 1000);
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class, matchItemAndFluid(level, fluidStack, input));
        if (assemblyRecipe.isPresent()) {
            FluidIngredient requiredFluid = assemblyRecipe.get().value().getRequiredFluid();
            FluidStack requiredFluidStack = requiredFluid.getMatchingFluidStacks().getFirst();
            if (Helpers.isFluidTheSame(requiredFluidStack, fluidStack)) {
                return requiredFluid.getRequiredAmount();
            }
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();

            Ingredient ingredient = recipe.getIngredientsItem();
            if (!ingredient.test(itemStack)) {
                continue;
            }

            FluidIngredient fluidIngredient = recipe.getIngredientsFluid();
            FluidStack ingredientFluidStack = fluidIngredient.getMatchingFluidStacks().getFirst();
            if (!Helpers.isFluidTheSame(ingredientFluidStack, fluidStack)) {
                continue;
            }

            return ingredientFluidStack.getAmount();
        }

        return -1;
    }

    public static boolean isItemInvalidForInjection(Level level, ItemStack itemStack) {
        if (level == null) {
            return true;
        }

        if (CanisterUtil.isInjectableCanister(itemStack)) {
            return false;
        }

        SingleRecipeInput input = new SingleRecipeInput(itemStack);
        Optional<RecipeHolder<GasInjectionRecipe>> assemblyRecipe = SequencedAssemblyRecipe.getRecipe(level, input, CCBRecipeTypes.GAS_INJECTION.getType(), GasInjectionRecipe.class);
        if (assemblyRecipe.isPresent()) {
            return false;
        }

        List<RecipeHolder<GasInjectionRecipe>> recipes = level.getRecipeManager().getAllRecipesFor(CCBRecipeTypes.GAS_INJECTION.getType());
        for (RecipeHolder<GasInjectionRecipe> holder : recipes) {
            GasInjectionRecipe recipe = holder.value();
            Ingredient ingredient = recipe.getIngredientsItem();
            if (ingredient.test(itemStack)) {
                return false;
            }
        }
        return true;
    }

    private static Predicate<RecipeHolder<GasInjectionRecipe>> matchItemAndFluid(Level level, FluidStack fluidStack, SingleRecipeInput input) {
        return r -> r.value().matches(input, level) && Helpers.isFluidTheSame(r.value().getRequiredFluid().getMatchingFluidStacks().getFirst(), fluidStack);
    }

    @Override
    public boolean matches(@NotNull SingleRecipeInput inv, @NotNull Level level) {
        return ingredients.getFirst().test(inv.getItem(0));
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

    public FluidIngredient getRequiredFluid() {
        if (fluidIngredients.isEmpty()) {
            throw new IllegalStateException("Filling Recipe has no fluid ingredient!");
        }
        return fluidIngredients.getFirst();
    }

    public Ingredient getIngredientsItem() {
        return ingredients.getFirst();
    }

    public FluidIngredient getIngredientsFluid() {
        return fluidIngredients.getFirst();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        return CCBLang.translateDirect("recipe.assembly.gas_injection_injecting_gas", fluidIngredients.getFirst().getMatchingFluidStacks().getFirst().getHoverName().getString());
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(CCBBlocks.GAS_INJECTION_CHAMBER_BLOCK.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public void addAssemblyFluidIngredients(List<FluidIngredient> list) {
        list.add(getRequiredFluid());
    }

    @Override
    public Supplier<Supplier<SequencedAssemblySubCategory>> getJEISubCategory() {
        return () -> CCBSequencedAssemblySubCategory.AssemblyInjecting::new;
    }
}