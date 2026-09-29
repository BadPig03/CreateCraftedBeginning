package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.AllRecipeTypes;
import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe;
import com.simibubi.create.content.processing.sequenced.SequencedAssemblyRecipe.SequencedAssembly;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressPressingRecipe {
    private final PressingRecipe recipe;
    private final ItemStack expectedInput;
    private final @Nullable RecipeHolder<SequencedAssemblyRecipe> assembly;

    @Internal
    public AirtightForgingPressPressingRecipe(PressingRecipe recipe, ItemStack input, @Nullable RecipeHolder<SequencedAssemblyRecipe> assembly) {
        this.recipe = recipe;
        expectedInput = input.copyWithCount(1);
        this.assembly = assembly;
    }

    boolean matches(Level level, ItemStack input) {
        if (input.isEmpty() || !ItemStack.isSameItemSameComponents(expectedInput, input)) {
            return false;
        }

        if (assembly == null) {
            return recipe.matches(new SingleRecipeInput(input), level);
        }

        return level.getRecipeManager().byKey(assembly.id()).filter(holder -> holder.value() == assembly.value()).isPresent();
    }

    List<ItemStack> previewOutputs(int crafts) {
        List<ItemStack> singleOutputs = new ArrayList<>();
        List<ProcessingOutput> results = recipe.getRollableResults();
        int firstStaticResult = 0;
        if (assembly != null) {
            SequencedAssemblyRecipe assemblyRecipe = assembly.value();
            SequencedAssembly progress = expectedInput.get(AllDataComponents.SEQUENCED_ASSEMBLY);
            int nextStep = 1;
            if (progress != null) {
                nextStep = progress.step() + 1;
            }

            int totalSteps = assemblyRecipe.getSequence().size() * assemblyRecipe.getLoops();
            if (nextStep >= totalSteps) {
                for (ProcessingOutput result : assemblyRecipe.resultPool) {
                    ItemStack resultStack = result.getStack();
                    if (result.getChance() <= 0 || resultStack.isEmpty()) {
                        continue;
                    }

                    singleOutputs.add(resultStack.copy());
                }
            }
            else {
                ItemStack transitionalItem = assemblyRecipe.getTransitionalItem().copyWithCount(1);
                transitionalItem.set(AllDataComponents.SEQUENCED_ASSEMBLY, new SequencedAssembly(assembly.id(), nextStep, (float) nextStep / totalSteps));
                singleOutputs.add(transitionalItem);
            }
            firstStaticResult = 1;
        }

        for (int resultIndex = firstStaticResult; resultIndex < results.size(); resultIndex++) {
            ItemStack result = results.get(resultIndex).getStack();
            if (result.isEmpty()) {
                continue;
            }

            singleOutputs.add(result.copy());
        }

        if (expectedInput.hasCraftingRemainingItem()) {
            ItemStack remainder = expectedInput.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                singleOutputs.add(remainder.copy());
            }
        }

        List<ItemStack> outputs = new ArrayList<>();
        for (ItemStack output : singleOutputs) {
            for (int craft = 0; craft < crafts; craft++) {
                outputs.add(output.copy());
            }
        }
        return outputs;
    }

    Optional<List<ItemStack>> rollOutputs(Level level, ItemStack input) {
        if (!matches(level, input)) {
            return Optional.empty();
        }

        if (assembly != null) {
            Optional<RecipeHolder<PressingRecipe>> currentRecipe = SequencedAssemblyRecipe.getRecipe(level, new SingleRecipeInput(input), AllRecipeTypes.PRESSING.getType(), PressingRecipe.class, holder -> holder.id().equals(assembly.id()) && holder.value() == recipe);
            if (currentRecipe.isEmpty()) {
                return Optional.empty();
            }
        }

        return Optional.of(RecipeApplier.applyRecipeOn(level, input, recipe, true));
    }
}
