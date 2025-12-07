package net.ty.createcraftedbeginning.api.gas.recipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.interfaces.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory.AssemblySpouting;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class FillingWithGasRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas {
    public FillingWithGasRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.FILLING_WITH_GAS, params);
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

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        List<FluidStack> matchingFluidStacks = Arrays.asList(fluidIngredients.getFirst().getFluids());
        return matchingFluidStacks.isEmpty() ? Component.literal("Invalid") : CreateLang.translateDirect("recipe.assembly.spout_filling_fluid", matchingFluidStacks.getFirst().getHoverName().getString());
    }

    @Override
    public void addRequiredMachines(@NotNull Set<ItemLike> list) {
        list.add(AllBlocks.SPOUT.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public void addAssemblyFluidIngredients(@NotNull List<SizedFluidIngredient> list) {
        list.add(getRequiredFluid());
    }

    @Override
    public Supplier<Supplier<SequencedAssemblyWithGasSubCategory>> getJEISubCategory() {
        return () -> AssemblySpouting::new;
    }

    public SizedFluidIngredient getRequiredFluid() {
        if (fluidIngredients.isEmpty()) {
            throw new IllegalStateException("Filling Recipe has no fluid ingredient!");
        }
        return fluidIngredients.getFirst();
    }
}
