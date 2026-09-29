package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingRecipeParams;
import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ChilledCompactingRecipe extends ChilledBasinRecipe {
    public ChilledCompactingRecipe(ProcessingRecipeParams params) {
        super(CCBRecipeTypes.CHILLED_COMPACTING, params);
    }
}
