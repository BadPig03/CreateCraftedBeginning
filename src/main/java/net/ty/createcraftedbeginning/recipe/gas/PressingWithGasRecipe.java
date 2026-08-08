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
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Set;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressingWithGasRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas {
    /**
     * Creates a new {@code PressingWithGasRecipe} instance.
     *
     * @param params the parameters used to configure the operation
     */
    public PressingWithGasRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.PRESSING_WITH_GAS, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return !input.isEmpty() && ingredients.getFirst().test(input.getItem(0));
    }

    @Override
    protected int getMaxInputCount() {
        return 1;
    }

    @Override
    protected int getMaxOutputCount() {
        return 2;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        return CreateLang.translateDirect("recipe.assembly.pressing");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addAssemblyIngredients(List<Ingredient> list) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addRequiredMachines(Set<ItemLike> list) {
        list.add(AllBlocks.MECHANICAL_PRESS.get());
    }
}
