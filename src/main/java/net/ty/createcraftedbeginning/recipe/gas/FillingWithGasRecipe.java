package net.ty.createcraftedbeginning.recipe.gas;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class FillingWithGasRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas {
    public FillingWithGasRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.FILLING_WITH_GAS, params);
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return ingredients.getFirst().test(input.getItem(0));
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
        FluidStack[] stacks = fluidIngredients.getFirst().getFluids();
        if (stacks.length == 0) {
            return Component.literal("Invalid");
        }
        return CreateLang.translateDirect("recipe.assembly.spout_filling_fluid", stacks[0].getHoverName().getString());
    }

    @Override
    public void addAssemblyFluidIngredients(List<SizedFluidIngredient> list) {
        list.add(getRequiredFluid());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(AllBlocks.SPOUT.get());
    }

    public SizedFluidIngredient getRequiredFluid() {
        if (fluidIngredients.isEmpty()) {
            throw new IllegalStateException("Filling Recipe has no fluid ingredient!");
        }
        return fluidIngredients.getFirst();
    }
}
