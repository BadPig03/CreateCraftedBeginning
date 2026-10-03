package net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber;

import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner.BasinInputs;
import net.ty.createcraftedbeginning.content.airtights.gasinjectionchamber.GasInjectionChamberBasinPlanner.BasinPlan;
import net.ty.createcraftedbeginning.recipe.GasInjectionRecipe;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class GasInjectionChamberBasinOutputs {
    static List<ItemStack> createPotentialItemResults(GasInjectionRecipe recipe, int batchSize) {
        List<ItemStack> resultStacks = new ArrayList<>();
        if (!recipe.hasItemOutput() || batchSize <= 0) {
            return resultStacks;
        }

        for (ProcessingOutput output : recipe.getRollableResults()) {
            ItemStack resultPerBatch = output.getStack();
            if (resultPerBatch.isEmpty()) {
                continue;
            }

            for (int batch = 0; batch < batchSize; batch++) {
                addResultStack(resultStacks, resultPerBatch);
            }
        }
        return resultStacks;
    }

    private static void addResultStack(List<ItemStack> resultStacks, ItemStack stackToAdd) {
        if (stackToAdd.isEmpty()) {
            return;
        }

        ItemStack remainingStack = stackToAdd.copy();
        for (ItemStack existingStack : resultStacks) {
            if (!ItemStack.isSameItemSameComponents(existingStack, remainingStack)) {
                continue;
            }

            int availableSpace = existingStack.getMaxStackSize() - existingStack.getCount();
            if (availableSpace <= 0) {
                continue;
            }

            int movedCount = Math.min(availableSpace, remainingStack.getCount());
            existingStack.grow(movedCount);
            remainingStack.shrink(movedCount);
            if (remainingStack.isEmpty()) {
                return;
            }
        }

        while (!remainingStack.isEmpty()) {
            int splitCount = Math.min(remainingStack.getCount(), remainingStack.getMaxStackSize());
            resultStacks.add(remainingStack.split(splitCount));
        }
    }

    private static List<ItemStack> rollItemResults(GasInjectionRecipe recipe, Level level, int batchSize) {
        List<ItemStack> resultStacks = new ArrayList<>();
        if (!recipe.hasItemOutput() || batchSize <= 0) {
            return resultStacks;
        }

        for (int batch = 0; batch < batchSize; batch++) {
            for (ItemStack resultStack : recipe.rollResults(level.random)) {
                addResultStack(resultStacks, resultStack);
            }
        }
        return resultStacks;
    }

    Optional<BasinPlan> preview(BasinBlockEntity basin, BasinInputs inputs) {
        return createPlan(basin, inputs, false);
    }

    Optional<BasinPlan> roll(BasinBlockEntity basin, BasinInputs inputs) {
        return createPlan(basin, inputs, true);
    }

    FluidStack getBatchFluidResult(GasInjectionRecipe recipe, int batchSize) {
        if (!recipe.hasFluidOutput() || batchSize <= 0) {
            return FluidStack.EMPTY;
        }

        FluidStack resultPerBatch = recipe.getFluidResult();
        if (resultPerBatch.isEmpty()) {
            return FluidStack.EMPTY;
        }

        long resultAmount = (long) resultPerBatch.getAmount() * batchSize;
        if (resultAmount <= 0 || resultAmount > Integer.MAX_VALUE) {
            return FluidStack.EMPTY;
        }

        return resultPerBatch.copyWithAmount((int) resultAmount);
    }

    private Optional<BasinPlan> createPlan(BasinBlockEntity basin, BasinInputs inputs, boolean rollRandomItemOutputs) {
        GasInjectionRecipe recipe = inputs.recipe();
        int batchSize = inputs.batchSize();
        List<ItemStack> itemResults = rollRandomItemOutputs ? rollItemResults(recipe, inputs.level(), batchSize) : createPotentialItemResults(recipe, batchSize);
        FluidStack fluidResult = getBatchFluidResult(recipe, batchSize);
        if (!rollRandomItemOutputs && recipe.hasItemOutput() && itemResults.isEmpty() || recipe.hasFluidOutput() && fluidResult.isEmpty()) {
            return Optional.empty();
        }

        List<FluidStack> fluidResults = fluidResult.isEmpty() ? List.of() : List.of(fluidResult);
        if (!basin.acceptOutputs(itemResults, fluidResults, true)) {
            return Optional.empty();
        }

        return Optional.of(new BasinPlan(recipe, inputs.gasPlan(), inputs.itemInputs(), inputs.fluidInputs(), itemResults, fluidResults));
    }
}
