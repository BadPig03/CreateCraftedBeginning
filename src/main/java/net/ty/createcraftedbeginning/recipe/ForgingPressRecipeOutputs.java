package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ForgingPressRecipeOutputs {
    private static final int PRIMARY_RESULT_INDEX = 0;
    private final ForgingPressRecipe recipe;

    ForgingPressRecipeOutputs(ForgingPressRecipe recipe) {
        this.recipe = recipe;
    }

    List<ItemStack> preview(Level level, ItemStack input, boolean copyInputComponents, int crafts) {
        return createRecipeOutputItems(level, input, copyInputComponents, false, crafts);
    }

    List<ItemStack> roll(Level level, ItemStack input, boolean copyInputComponents, int crafts) {
        return createRecipeOutputItems(level, input, copyInputComponents, true, crafts);
    }

    private List<ItemStack> createRecipeOutputItems(Level level, ItemStack input, boolean copyInputComponents, boolean rollRandomOutputs) {
        List<ItemStack> outputs = new ArrayList<>();
        List<ProcessingOutput> rollableResults = recipe.getRollableResults();
        for (int resultIndex = 0; resultIndex < rollableResults.size(); resultIndex++) {
            ProcessingOutput output = rollableResults.get(resultIndex);
            ItemStack outputStack = rollRandomOutputs ? output.rollOutput(level.random) : output.getStack();
            if (outputStack.isEmpty()) {
                continue;
            }

            ItemStack copiedOutput = outputStack.copy();
            if (copyInputComponents && resultIndex == PRIMARY_RESULT_INDEX && !input.isEmpty()) {
                copiedOutput.applyComponents(input.getComponentsPatch());
            }
            outputs.add(copiedOutput);
        }
        return outputs;
    }

    private List<ItemStack> createRecipeOutputItems(Level level, ItemStack input, boolean copyInputComponents, boolean rollRandomOutputs, int crafts) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int craftIndex = 0; craftIndex < crafts; craftIndex++) {
            outputs.addAll(createRecipeOutputItems(level, input, copyInputComponents, rollRandomOutputs));
        }
        return outputs;
    }
}
