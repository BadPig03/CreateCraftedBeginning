package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPlanner.CraftPlan;
import net.ty.createcraftedbeginning.recipe.ForgingPressCraftPlanner.ForgingOperationPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.OutputPlan;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ForgingPressCraftPreparation {
    private ForgingPressCraftPreparation() {
    }

    public static Optional<Plan> prepare(ForgingPressRecipeContext press, ForgingPressRecipe recipe) {
        Level level = press.getLevel();
        if (level == null) {
            return Optional.empty();
        }

        ForgingOperationPlan operationPlan = new ForgingPressCraftPlanner(press, recipe).planOperation(level);
        if (operationPlan == null) {
            return Optional.empty();
        }

        CraftPlan craftPlan = operationPlan.craftPlan();
        ItemStack inputStack = operationPlan.inputStack();
        int craftCount = craftPlan.crafts();
        List<ItemStack> outputs = new ForgingPressRecipeOutputs(recipe).roll(level, inputStack, operationPlan.copyInputComponents(), craftCount);
        Optional<OutputPlan> outputPlan = press.planOutputs(outputs);
        if (outputPlan.isEmpty()) {
            return Optional.empty();
        }

        IItemHandler additionInventory = press.getAdditionInventory();
        IItemHandler inputInventory = press.getInputInventory();
        int additionAmount = operationPlan.additionIngredient().isEmpty() ? 0 : craftCount;
        int inputAmount = operationPlan.inputIngredient().isEmpty() ? 0 : craftCount;
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(additionInventory.getStackInSlot(0).copy(), additionAmount, inputInventory.getStackInSlot(0).copy(), inputAmount, craftPlan.fluidAmounts(), craftPlan.gasPlan());
        return Optional.of(new Plan(consumptionPlan, outputPlan.get()));
    }

    public record Plan(ConsumptionPlan consumption, OutputPlan output) {
    }
}
