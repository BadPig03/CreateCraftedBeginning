package net.ty.createcraftedbeginning.recipe.gas;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
@SuppressWarnings("unused")
public interface IAssemblyRecipeWithGas {
    @OnlyIn(Dist.CLIENT)
    Component getDescriptionForAssembly();

    default boolean supportsAssembly() {
        return true;
    }

    default void addAssemblyFluidIngredients(List<SizedFluidIngredient> list) {
    }

    default void addAssemblyGasIngredients(List<SizedGasIngredient> list) {
    }

    void addAssemblyIngredients(List<Ingredient> list);

    void addRequiredMachines(Set<ItemLike> list);
}
