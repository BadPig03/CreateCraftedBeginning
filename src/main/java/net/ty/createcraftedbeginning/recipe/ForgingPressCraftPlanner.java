package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasRecipePressureSpeed;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class ForgingPressCraftPlanner {
    private final ForgingPressRecipeContext press;
    private final ForgingPressRecipe recipe;

    public ForgingPressCraftPlanner(ForgingPressRecipeContext press, ForgingPressRecipe recipe) {
        this.press = press;
        this.recipe = recipe;
    }

    static boolean planFluidConsumption(List<SizedFluidIngredient> ingredients, IFluidHandler fluidHandler, int[] amounts, int crafts) {
        for (SizedFluidIngredient ingredient : ingredients) {
            long required = (long) ingredient.amount() * crafts;
            if (required <= 0 || required > Integer.MAX_VALUE) {
                return false;
            }

            int remaining = (int) required;
            for (int tankIndex = 0; tankIndex < fluidHandler.getTanks(); tankIndex++) {
                FluidStack fluidStack = fluidHandler.getFluidInTank(tankIndex);
                if (!ingredient.test(fluidStack)) {
                    continue;
                }

                int availableAmount = fluidStack.getAmount() - amounts[tankIndex];
                if (availableAmount <= 0) {
                    continue;
                }

                int consumedAmount = Math.min(remaining, availableAmount);
                amounts[tankIndex] += consumedAmount;
                remaining -= consumedAmount;
                if (remaining <= 0) {
                    break;
                }
            }

            if (remaining > 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesNonConsumableSlot(IItemHandler inventory, Ingredient ingredient) {
        ItemStack storedStack = inventory.getStackInSlot(0);
        if (ingredient.isEmpty()) {
            return storedStack.isEmpty();
        }

        return !storedStack.isEmpty() && ingredient.test(storedStack);
    }

    private static @Nullable ItemStack getConsumableStack(IItemHandler inventory, Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            if (inventory.getStackInSlot(0).isEmpty()) {
                return ItemStack.EMPTY;
            }

            return null;
        }

        ItemStack extractedStack = inventory.extractItem(0, 1, true);
        if (extractedStack.isEmpty() || !ingredient.test(extractedStack)) {
            return null;
        }

        return extractedStack.copy();
    }

    private static int getMaxItemCrafts(IItemHandler addition, Ingredient additionIngredient, IItemHandler input, Ingredient inputIngredient) {
        int maxCrafts = getAvailableCrafts(addition, additionIngredient, 64);
        return getAvailableCrafts(input, inputIngredient, maxCrafts);
    }

    private static int getAvailableCrafts(IItemHandler inventory, Ingredient ingredient, int maxCrafts) {
        if (ingredient.isEmpty()) {
            if (!inventory.getStackInSlot(0).isEmpty()) {
                return 0;
            }

            return maxCrafts;
        }

        ItemStack extractedStack = inventory.extractItem(0, maxCrafts, true);
        if (extractedStack.isEmpty() || !ingredient.test(extractedStack)) {
            return 0;
        }

        return extractedStack.getCount();
    }

    public float getPressureSpeedMultiplier() {
        Level level = press.getLevel();
        if (level == null) {
            return 1;
        }

        ForgingOperationPlan operationPlan = planOperation(level);
        if (operationPlan == null) {
            return 1;
        }

        return GasRecipePressureSpeed.multiplier(operationPlan.craftPlan().gasPlan());
    }

    public boolean matches() {
        Level level = press.getLevel();
        return level != null && planOperation(level) != null;
    }

    @Nullable ForgingOperationPlan planOperation(Level level) {
        IItemHandler pressHeadInventory = press.getPressHeadInventory();
        if (!matchesNonConsumableSlot(pressHeadInventory, getIngredient(1))) {
            return null;
        }

        ItemStack pressHead = pressHeadInventory.getStackInSlot(0);
        boolean copyInputComponents = !CCBConfig.server().machines.airtightForgingPress.requireSmithingTemplateForComponentCopy.get() || pressHead.getItem() instanceof SmithingTemplateItem;
        IItemHandler additionInventory = press.getAdditionInventory();
        IItemHandler inputInventory = press.getInputInventory();
        Ingredient inputIngredient = getIngredient(0);
        Ingredient additionIngredient = getIngredient(2);
        int maxCrafts = getMaxItemCrafts(additionInventory, additionIngredient, inputInventory, inputIngredient);
        if (maxCrafts <= 0) {
            return null;
        }

        ItemStack inputStack = getConsumableStack(inputInventory, inputIngredient);
        if (inputStack == null) {
            return null;
        }

        IFluidHandler fluidHandler = press.getFluidCapability();
        GasStorageHandler gasHandler = press.getGasCapability();
        CraftPlan craftPlan = findLargestCraftPlan(level, inputStack, copyInputComponents, fluidHandler, gasHandler, maxCrafts);
        if (craftPlan == null) {
            return null;
        }

        return new ForgingOperationPlan(inputStack, inputIngredient, additionIngredient, copyInputComponents, craftPlan);
    }

    private @Nullable CraftPlan findLargestCraftPlan(Level level, ItemStack input, boolean copyInputComponents, IFluidHandler fluidHandler, GasStorageHandler gasHandler, int maxCrafts) {
        List<ItemStack> singleCraftOutputs = new ForgingPressRecipeOutputs(recipe).preview(level, input, copyInputComponents, 1);
        if (singleCraftOutputs.isEmpty() || !outputsPassFilter(singleCraftOutputs)) {
            return null;
        }

        int low = 1;
        int high = maxCrafts;
        CraftPlan bestPlan = null;
        while (low <= high) {
            int crafts = low + high >>> 1;
            int[] fluidAmounts = new int[fluidHandler.getTanks()];
            Optional<GasConsumptionPlan> gasPlan = GasConsumptionPlanner.plan(recipe.getGasRequirements(), gasHandler, crafts);
            boolean hasRequiredResources = gasPlan.isPresent() && planFluidConsumption(recipe.getFluidIngredients(), fluidHandler, fluidAmounts, crafts);
            boolean canFitOutputs = hasRequiredResources && press.acceptOutputs(new ForgingPressRecipeOutputs(recipe).preview(level, input, copyInputComponents, crafts), true);
            if (!canFitOutputs) {
                high = crafts - 1;
                continue;
            }

            bestPlan = new CraftPlan(crafts, fluidAmounts, gasPlan.get());
            low = crafts + 1;
        }
        return bestPlan;
    }

    private Ingredient getIngredient(int index) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (index < 0 || index >= ingredients.size()) {
            return Ingredient.EMPTY;
        }

        return ingredients.get(index);
    }

    private boolean outputsPassFilter(List<ItemStack> outputs) {
        return !outputs.isEmpty() && press.testRecipeFilter(outputs.getFirst());
    }

    record ForgingOperationPlan(ItemStack inputStack, Ingredient inputIngredient, Ingredient additionIngredient, boolean copyInputComponents, CraftPlan craftPlan) {
        ForgingOperationPlan {
            inputStack = inputStack.copy();
        }
    }

    record CraftPlan(int crafts, int[] fluidAmounts, GasConsumptionPlan gasPlan) {
        CraftPlan {
            fluidAmounts = fluidAmounts.clone();
        }
    }
}
