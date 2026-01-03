package net.ty.createcraftedbeginning.api.gas.gases;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.recipes.SequencedAssemblyWithGasSubCategory;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public interface IAssemblyRecipeWithGas {
    default boolean supportsAssembly() {
        return true;
    }

    @OnlyIn(Dist.CLIENT)
    Component getDescriptionForAssembly();

    void addRequiredMachines(Set<ItemLike> list);

    void addAssemblyIngredients(List<Ingredient> list);

    default void addAssemblyFluidIngredients(List<SizedFluidIngredient> list) {
    }

    default void addAssemblyGasIngredients(List<SizedGasIngredient> list) {
    }

    Supplier<Supplier<SequencedAssemblyWithGasSubCategory>> getJEISubCategory();
}
