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
    /**
     * Returns the description for assembly.
     *
     * @return the description for assembly
     */
    @OnlyIn(Dist.CLIENT)
    Component getDescriptionForAssembly();

    /**
     * Checks whether assembly is supported.
     *
     * @return {@code true} if assembly is supported; otherwise {@code false}
     */
    default boolean supportsAssembly() {
        return true;
    }

    /**
     * Adds the supplied assembly fluid ingredient.
     *
     * @param list the list to inspect or populate
     */
    default void addAssemblyFluidIngredients(List<SizedFluidIngredient> list) {
    }

    /**
     * Adds the supplied assembly gas ingredient.
     *
     * @param list the list to inspect or populate
     */
    default void addAssemblyGasIngredients(List<SizedGasIngredient> list) {
    }

    /**
     * Adds the supplied assembly ingredient.
     *
     * @param list the list to inspect or populate
     */
    void addAssemblyIngredients(List<Ingredient> list);

    /**
     * Adds the supplied required machine.
     *
     * @param list the list to inspect or populate
     */
    void addRequiredMachines(Set<ItemLike> list);
}
