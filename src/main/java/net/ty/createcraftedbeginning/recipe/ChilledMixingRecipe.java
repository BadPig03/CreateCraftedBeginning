package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChilledMixingRecipe extends ChilledBasinRecipe {
    public ChilledMixingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.CHILLED_MIXING, params);
    }
}
