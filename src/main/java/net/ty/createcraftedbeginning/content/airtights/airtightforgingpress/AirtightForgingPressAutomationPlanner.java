package net.ty.createcraftedbeginning.content.airtights.airtightforgingpress;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
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
public final class AirtightForgingPressAutomationPlanner {
    private static final int SMITHING_BASE_SLOT = 1;
    private static final int SMITHING_ADDITION_SLOT = 2;

    private AirtightForgingPressAutomationPlanner() {
    }

    @Internal
    public static Optional<Plan> preparePressingRecipe(AirtightForgingPressBlockEntity press, AirtightForgingPressPressingRecipe recipe) {
        Level level = press.getLevel();
        if (level == null || !press.getPressHeadInventory().isEmpty() || !press.getAdditionInventory().isEmpty()) {
            return Optional.empty();
        }

        IItemHandler inputInventory = press.getInputInventory();
        ItemStack inputStack = inputInventory.getStackInSlot(0);
        int batchSize = findLargestPressingBatch(press, recipe, inputStack);
        if (batchSize <= 0) {
            return Optional.empty();
        }

        ItemStack batchInput = inputStack.copyWithCount(batchSize);
        Optional<List<ItemStack>> outputStacks = recipe.rollOutputs(level, batchInput);
        if (outputStacks.isEmpty()) {
            return Optional.empty();
        }

        Optional<OutputPlan> plannedOutput = press.planOutputs(outputStacks.get());
        if (plannedOutput.isEmpty()) {
            return Optional.empty();
        }

        int[] fluidAmounts = new int[press.getFluidCapability().getTanks()];
        GasConsumptionPlan gasPlan = GasConsumptionPlan.empty(press.getGasCapability().getTanks());
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(ItemStack.EMPTY, 0, inputStack.copy(), batchSize, fluidAmounts, gasPlan);
        return Optional.of(new Plan(consumptionPlan, plannedOutput.get()));
    }

    @Internal
    public static boolean canApplyPressingRecipe(AirtightForgingPressBlockEntity press, AirtightForgingPressPressingRecipe recipe, ItemStack inputStack) {
        return findLargestPressingBatch(press, recipe, inputStack) > 0;
    }

    static Optional<Plan> planSmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe) {
        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        SmithingRecipeInput smithingInput = createSmithingInput(press);
        if (!canApplySmithingRecipe(press, recipe, smithingInput)) {
            return Optional.empty();
        }

        List<ItemStack> outputStacks = getSmithingOutputs(recipe, smithingInput, level);
        Optional<OutputPlan> plannedOutput = press.planOutputs(outputStacks);
        if (plannedOutput.isEmpty()) {
            return Optional.empty();
        }

        IItemHandler processingInventory = press.getAdditionInventory();
        IItemHandler inputInventory = press.getInputInventory();
        ItemStack simulatedProcessingStack = processingInventory.extractItem(0, 1, true);
        ItemStack simulatedInputStack = inputInventory.extractItem(0, 1, true);
        if (simulatedProcessingStack.getCount() != 1 || simulatedInputStack.getCount() != 1) {
            return Optional.empty();
        }

        int[] fluidAmounts = new int[press.getFluidCapability().getTanks()];
        GasConsumptionPlan gasPlan = GasConsumptionPlan.empty(press.getGasCapability().getTanks());
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(simulatedProcessingStack, 1, simulatedInputStack, 1, fluidAmounts, gasPlan);
        return Optional.of(new Plan(consumptionPlan, plannedOutput.get()));
    }

    static SmithingRecipeInput createSmithingInput(AirtightForgingPressBlockEntity press) {
        ItemStack templateStack = press.getPressHeadInventory().getStackInSlot(0).copy();
        ItemStack additionStack = press.getAdditionInventory().getStackInSlot(0).copy();
        ItemStack baseStack = press.getInputInventory().getStackInSlot(0).copy();
        return new SmithingRecipeInput(templateStack, baseStack, additionStack);
    }

    static boolean canApplySmithingRecipe(AirtightForgingPressBlockEntity press, SmithingRecipe recipe, SmithingRecipeInput smithingInput) {
        Level level = press.getLevel();
        if (level == null || !recipe.matches(smithingInput, level)) {
            return false;
        }

        List<ItemStack> outputStacks = getSmithingOutputs(recipe, smithingInput, level);
        return !outputStacks.isEmpty() && press.testRecipeFilter(outputStacks.getFirst()) && press.acceptOutputs(outputStacks, true);
    }

    private static int findLargestPressingBatch(AirtightForgingPressBlockEntity press, AirtightForgingPressPressingRecipe recipe, ItemStack inputStack) {
        Level level = press.getLevel();
        if (level == null || !recipe.matches(level, inputStack)) {
            return 0;
        }

        List<ItemStack> singleCraftOutputs = recipe.previewOutputs(1);
        if (singleCraftOutputs.isEmpty() || !press.testRecipeFilter(singleCraftOutputs.getFirst())) {
            return 0;
        }

        int lowerBound = 1;
        int upperBound = inputStack.getCount();
        int largestBatch = 0;
        while (lowerBound <= upperBound) {
            int batchSize = lowerBound + upperBound >>> 1;
            boolean canFitOutputs = press.acceptOutputs(recipe.previewOutputs(batchSize), true);
            if (!canFitOutputs) {
                upperBound = batchSize - 1;
                continue;
            }

            largestBatch = batchSize;
            lowerBound = batchSize + 1;
        }
        return largestBatch;
    }

    private static List<ItemStack> getSmithingOutputs(SmithingRecipe recipe, SmithingRecipeInput smithingInput, Level level) {
        List<ItemStack> outputStacks = new ArrayList<>();
        ItemStack smithingResult = recipe.assemble(smithingInput, level.registryAccess());
        if (smithingResult.isEmpty()) {
            return outputStacks;
        }

        outputStacks.add(smithingResult.copy());
        NonNullList<ItemStack> remainingItems = recipe.getRemainingItems(smithingInput);
        addConsumedSlotRemainder(outputStacks, remainingItems, SMITHING_BASE_SLOT);
        addConsumedSlotRemainder(outputStacks, remainingItems, SMITHING_ADDITION_SLOT);
        return outputStacks;
    }

    private static void addConsumedSlotRemainder(List<ItemStack> outputStacks, NonNullList<ItemStack> remainingItems, int slot) {
        if (slot < 0 || slot >= remainingItems.size()) {
            return;
        }

        ItemStack remainingStack = remainingItems.get(slot);
        if (remainingStack.isEmpty()) {
            return;
        }

        outputStacks.add(remainingStack.copy());
    }
}
