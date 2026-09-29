package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import com.simibubi.create.content.kinetics.crusher.CrushingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import com.simibubi.create.foundation.recipe.RecipeApplier;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPreparation.Plan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.OutputPlan;
import org.jetbrains.annotations.ApiStatus.Internal;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Internal
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class AirtightForgingPressCrushing {
    private AirtightForgingPressCrushing() {
    }

    @Internal
    public static boolean canApply(AirtightForgingPressBlockEntity press, RecipeHolder<CrushingRecipe> recipe) {
        return findLargestBatch(press, recipe) > 0;
    }

    @Internal
    public static Optional<Plan> prepare(AirtightForgingPressBlockEntity press, RecipeHolder<CrushingRecipe> recipe) {
        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        int batchSize = findLargestBatch(press, recipe);
        if (batchSize <= 0) {
            return Optional.empty();
        }

        ItemStack input = press.getInputInventory().getStackInSlot(0).copy();
        List<ItemStack> outputs = RecipeApplier.applyRecipeOn(level, input.copyWithCount(batchSize), recipe.value(), false);
        if (input.hasCraftingRemainingItem()) {
            ItemStack remainder = input.getCraftingRemainingItem();
            if (!remainder.isEmpty()) {
                outputs.add(remainder.copy());
            }
        }

        Optional<OutputPlan> outputPlan = press.planOutputs(outputs);
        if (outputPlan.isEmpty()) {
            return Optional.empty();
        }

        int[] fluidAmounts = new int[press.getFluidCapability().getTanks()];
        GasConsumptionPlan gasPlan = GasConsumptionPlan.empty(press.getGasCapability().getTanks());
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(ItemStack.EMPTY, 0, input, batchSize, fluidAmounts, gasPlan);
        return Optional.of(new Plan(consumptionPlan, outputPlan.get()));
    }

    static boolean canProcess(AirtightForgingPressBlockEntity press) {
        return CCBConfig.server().machines.airtightForgingPress.enableAutomaticCrushingRecipes.get() && press.getPressHeadInventory().getStackInSlot(0).is(Items.HEAVY_CORE) && press.getAdditionInventory().isEmpty();
    }

    private static int findLargestBatch(AirtightForgingPressBlockEntity press, RecipeHolder<CrushingRecipe> recipeHolder) {
        Level level = press.getLevel();
        CrushingRecipe recipe = recipeHolder.value();
        if (level == null || !canProcess(press) || level.getRecipeManager().byKey(recipeHolder.id()).filter(current -> current.value() == recipe).isEmpty()) {
            return 0;
        }

        ItemStack input = press.getInputInventory().getStackInSlot(0);
        List<ProcessingOutput> results = recipe.getRollableResults();
        if (input.isEmpty() || !recipe.matches(new SingleRecipeInput(input), level) || results.isEmpty() || !press.testRecipeFilter(results.getFirst().getStack())) {
            return 0;
        }

        int lowerBound = 1;
        int upperBound = input.getCount();
        int largestBatch = 0;
        while (lowerBound <= upperBound) {
            int batchSize = lowerBound + upperBound >>> 1;
            List<ItemStack> outputs = new ArrayList<>();
            for (ProcessingOutput result : results) {
                ItemStack output = result.getStack();
                if (output.isEmpty()) {
                    continue;
                }

                for (int craft = 0; craft < batchSize; craft++) {
                    outputs.add(output.copy());
                }
            }
            if (input.hasCraftingRemainingItem()) {
                ItemStack remainder = input.getCraftingRemainingItem();
                if (!remainder.isEmpty()) {
                    outputs.add(remainder.copy());
                }
            }
            if (!press.acceptOutputs(outputs, true)) {
                upperBound = batchSize - 1;
                continue;
            }

            largestBatch = batchSize;
            lowerBound = batchSize + 1;
        }
        return largestBatch;
    }
}
