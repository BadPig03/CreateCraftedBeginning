package net.ty.createcraftedbeginning.api.gas.recipes;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import net.ty.createcraftedbeginning.api.gas.interfaces.IAssemblyRecipeWithGas;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory.AssemblyCutting;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class CuttingWithGasRecipe extends StandardProcessingWithGasRecipe<RecipeWrapper> implements IAssemblyRecipeWithGas {
    public CuttingWithGasRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.CUTTING_WITH_GAS, params);
    }

    @Override
    public boolean matches(@NotNull RecipeWrapper inv, @NotNull Level level) {
        return !inv.isEmpty() && ingredients.getFirst().test(inv.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 4;
    }

    @Override
    protected boolean canSpecifyDuration() {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        return CreateLang.translateDirect("recipe.assembly.cutting");
    }

    @Override
    public void addRequiredMachines(@NotNull Set<ItemLike> list) {
        list.add(AllBlocks.MECHANICAL_SAW.get());
    }

    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    @Override
    public Supplier<Supplier<SequencedAssemblyWithGasSubCategory>> getJEISubCategory() {
        return () -> AssemblyCutting::new;
    }
}
