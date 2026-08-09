package net.ty.createcraftedbeginning.recipe.gas;

import com.simibubi.create.foundation.utility.CreateLang;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.recipe.CCBRecipeTypes;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PressingWithGasRecipe extends StandardProcessingWithGasRecipe<SingleRecipeInput> implements IAssemblyRecipeWithGas {
    public PressingWithGasRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.PRESSING_WITH_GAS, params);
    }

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

    @Override
    @OnlyIn(Dist.CLIENT)
    public Component getDescriptionForAssembly() {
        return CreateLang.translateDirect("recipe.assembly.pressing");
    }

}
