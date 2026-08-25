package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.crafting.SizedFluidIngredient;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.config.CCBConfig;
import net.ty.createcraftedbeginning.platform.SmithingRecipeBridge;
import net.ty.createcraftedbeginning.platform.SmithingRecipeBridge.Ingredients;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.interfaces.ForgingPressRecipeContext.OutputPlan;
import net.ty.createcraftedbeginning.recipe.trie.IAirtightWithGasRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ForgingPressRecipe extends StandardProcessingWithGasRecipe<RecipeInput> implements IAirtightWithGasRecipe {
    private static final int PRIMARY_RESULT_INDEX = 0;

    @Nullable
    private SmithingRecipe smithingRecipe;

    ForgingPressRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.FORGING_PRESS, params);
    }

    public static boolean canConvertSmithingRecipe(Recipe<?> source) {
        return SmithingRecipeBridge.getIngredients(source) != null;
    }

    public static RecipeHolder<ForgingPressRecipe> convertToForgingPressRecipe(RecipeHolder<?> sourceHolder) {
        Builder<ForgingPressRecipe> builder = new Builder<>(ForgingPressRecipe::new, sourceHolder.id());
        Recipe<?> sourceRecipe = sourceHolder.value();
        Ingredients smithingIngredients = SmithingRecipeBridge.getIngredients(sourceRecipe);
        if (sourceRecipe instanceof SmithingRecipe smithingRecipe && smithingIngredients != null) {
            ForgingPressRecipe forgingRecipe = builder.require(smithingIngredients.base()).require(smithingIngredients.template()).require(smithingIngredients.addition()).build().setSmithingRecipe(smithingRecipe);
            return new RecipeHolder<>(sourceHolder.id(), forgingRecipe);
        }

        return new RecipeHolder<>(sourceHolder.id(), builder.build());
    }

    public static RecipeHolder<ForgingPressRecipe> convertPressingToForgingPressRecipe(RecipeHolder<?> sourceHolder) {
        Builder<ForgingPressRecipe> builder = new Builder<>(ForgingPressRecipe::new, sourceHolder.id());
        if (!(sourceHolder.value() instanceof PressingRecipe pressingRecipe)) {
            return new RecipeHolder<>(sourceHolder.id(), builder.build());
        }

        builder.withItemIngredients(pressingRecipe.getIngredients());
        pressingRecipe.getRollableResults().forEach(builder::output);
        return new RecipeHolder<>(sourceHolder.id(), builder.build());
    }

    public static boolean match(ForgingPressRecipeContext press, ForgingPressRecipe recipe) {
        return apply(press, recipe, true);
    }

    public static boolean apply(ForgingPressRecipeContext press, ForgingPressRecipe recipe) {
        return apply(press, recipe, false);
    }

    private static boolean apply(ForgingPressRecipeContext press, ForgingPressRecipe recipe, boolean simulate) {
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        IItemHandler pressHeadInventory = press.getPressHeadInventory();
        if (!matchesNonConsumableSlot(pressHeadInventory, getIngredient(recipe, 1))) {
            return false;
        }

        boolean copyInputComponents = shouldCopyInputComponents(pressHeadInventory.getStackInSlot(0));
        IItemHandler additionInventory = press.getAdditionInventory();
        IItemHandler inputInventory = press.getInputInventory();
        Ingredient inputIngredient = getIngredient(recipe, 0);
        Ingredient additionIngredient = getIngredient(recipe, 2);
        int maxCrafts = getMaxItemCrafts(additionInventory, additionIngredient, inputInventory, inputIngredient);
        if (maxCrafts <= 0) {
            return false;
        }

        ItemStack inputStack = getConsumableStack(inputInventory, inputIngredient);
        if (inputStack == null) {
            return false;
        }

        IFluidHandler fluidHandler = press.getFluidCapability();
        IGasHandler gasHandler = press.getGasCapability();
        CraftPlan craftPlan = findLargestCraftPlan(press, recipe, level, inputStack, copyInputComponents, fluidHandler, gasHandler, maxCrafts);
        if (craftPlan == null) {
            return false;
        }

        if (simulate) {
            return true;
        }

        List<ItemStack> outputs = createRecipeOutputItems(recipe, level, inputStack, copyInputComponents, true, craftPlan.crafts());
        Optional<OutputPlan> outputPlan = press.planOutputs(outputs);
        if (outputPlan.isEmpty()) {
            return false;
        }

        int additionAmount = additionIngredient.isEmpty() ? 0 : craftPlan.crafts();
        int inputAmount = inputIngredient.isEmpty() ? 0 : craftPlan.crafts();
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(additionInventory.getStackInSlot(0).copy(), additionAmount, inputInventory.getStackInSlot(0).copy(), inputAmount, craftPlan.fluidAmounts(), craftPlan.gasAmounts());
        return press.commitCraft(consumptionPlan, outputPlan.get());
    }

    private static @Nullable CraftPlan findLargestCraftPlan(ForgingPressRecipeContext press, ForgingPressRecipe recipe, Level level, ItemStack input, boolean copyInputComponents, IFluidHandler fluidHandler, IGasHandler gasHandler, int maxCrafts) {
        List<ItemStack> singleCraftOutputs = createRecipeOutputItems(recipe, level, input, copyInputComponents, false, 1);
        if (singleCraftOutputs.isEmpty() || !outputsPassFilter(press, singleCraftOutputs)) {
            return null;
        }

        int low = 1;
        int high = maxCrafts;
        CraftPlan bestPlan = null;
        while (low <= high) {
            int crafts = low + high >>> 1;
            int[] fluidAmounts = new int[fluidHandler.getTanks()];
            long[] gasAmounts = new long[gasHandler.getTanks()];
            boolean hasRequiredResources = planFluidAndGasConsumption(recipe, fluidHandler, gasHandler, fluidAmounts, gasAmounts, crafts);
            boolean canFitOutputs = hasRequiredResources && press.acceptOutputs(createRecipeOutputItems(recipe, level, input, copyInputComponents, false, crafts), true);
            if (!canFitOutputs) {
                high = crafts - 1;
                continue;
            }

            bestPlan = new CraftPlan(crafts, fluidAmounts, gasAmounts);
            low = crafts + 1;
        }
        return bestPlan;
    }

    private static Ingredient getIngredient(ForgingPressRecipe recipe, int index) {
        NonNullList<Ingredient> ingredients = recipe.getIngredients();
        if (index < 0 || index >= ingredients.size()) {
            return Ingredient.EMPTY;
        }
        return ingredients.get(index);
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
            return inventory.getStackInSlot(0).isEmpty() ? ItemStack.EMPTY : null;
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
            return inventory.getStackInSlot(0).isEmpty() ? maxCrafts : 0;
        }

        ItemStack extractedStack = inventory.extractItem(0, maxCrafts, true);
        if (extractedStack.isEmpty() || !ingredient.test(extractedStack)) {
            return 0;
        }
        return extractedStack.getCount();
    }

    private static boolean shouldCopyInputComponents(ItemStack pressHead) {
        return !CCBConfig.server().airtights.copyComponentsOnlyWithSmithingTemplates.get() || pressHead.getItem() instanceof SmithingTemplateItem;
    }

    private static List<ItemStack> createRecipeOutputItems(ForgingPressRecipe recipe, Level level, ItemStack input, boolean copyInputComponents, boolean rollRandomOutputs) {
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

    private static List<ItemStack> createRecipeOutputItems(ForgingPressRecipe recipe, Level level, ItemStack input, boolean copyInputComponents, boolean rollRandomOutputs, int crafts) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int craftIndex = 0; craftIndex < crafts; craftIndex++) {
            outputs.addAll(createRecipeOutputItems(recipe, level, input, copyInputComponents, rollRandomOutputs));
        }
        return outputs;
    }

    private static boolean planFluidAndGasConsumption(ForgingPressRecipe recipe, IFluidHandler fluidHandler, IGasHandler gasHandler, int[] fluidAmounts, long[] gasAmounts, int crafts) {
        return planFluidConsumption(recipe.getFluidIngredients(), fluidHandler, fluidAmounts, crafts) && planGasConsumption(recipe.getGasIngredients(), gasHandler, gasAmounts, crafts);
    }

    private static boolean planFluidConsumption(List<SizedFluidIngredient> ingredients, IFluidHandler fluidHandler, int[] amounts, int crafts) {
        for (SizedFluidIngredient ingredient : ingredients) {
            long required = (long) ingredient.amount() * crafts;
            if (required <= 0 || required > Integer.MAX_VALUE) {
                return false;
            }

            if (!consumeFluid(ingredient, fluidHandler, amounts, (int) required)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeFluid(SizedFluidIngredient ingredient, IFluidHandler fluidHandler, int[] amounts, int remainingAmount) {
        for (int tankIndex = 0; tankIndex < fluidHandler.getTanks(); tankIndex++) {
            FluidStack fluidStack = fluidHandler.getFluidInTank(tankIndex);
            if (!ingredient.test(fluidStack)) {
                continue;
            }

            int availableAmount = fluidStack.getAmount() - amounts[tankIndex];
            if (availableAmount <= 0) {
                continue;
            }

            int consumedAmount = Math.min(remainingAmount, availableAmount);
            amounts[tankIndex] += consumedAmount;
            remainingAmount -= consumedAmount;
            if (remainingAmount <= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean planGasConsumption(List<SizedGasIngredient> ingredients, IGasHandler gasHandler, long[] amounts, int crafts) {
        for (SizedGasIngredient ingredient : ingredients) {
            long amountPerCraft = ingredient.amount();
            if (amountPerCraft <= 0 || amountPerCraft > Long.MAX_VALUE / crafts) {
                return false;
            }

            if (!consumeGas(ingredient, gasHandler, amounts, amountPerCraft * crafts)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeGas(SizedGasIngredient ingredient, IGasHandler gasHandler, long[] amounts, long remainingAmount) {
        for (int tankIndex = 0; tankIndex < gasHandler.getTanks(); tankIndex++) {
            GasStack gasStack = gasHandler.getGasInTank(tankIndex);
            if (!ingredient.test(gasStack)) {
                continue;
            }

            long availableAmount = gasStack.getAmount() - amounts[tankIndex];
            if (availableAmount <= 0) {
                continue;
            }

            long consumedAmount = Math.min(remainingAmount, availableAmount);
            amounts[tankIndex] += consumedAmount;
            remainingAmount -= consumedAmount;
            if (remainingAmount <= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean outputsPassFilter(ForgingPressRecipeContext press, List<ItemStack> outputs) {
        return !outputs.isEmpty() && press.testRecipeFilter(outputs.getFirst());
    }

    private static boolean hasIngredient(NonNullList<Ingredient> ingredients, int index) {
        return index >= 0 && index < ingredients.size() && !ingredients.get(index).isEmpty();
    }

    @Override
    protected int getMaxInputCount() {
        return 3;
    }

    @Override
    protected int getMaxOutputCount() {
        return 8;
    }

    @Override
    protected int getMaxFluidInputCount() {
        return 1;
    }

    @Override
    protected int getMaxGasInputCount() {
        return 1;
    }

    @Override
    protected void validateSpecial(List<String> errors) {
        NonNullList<Ingredient> itemIngredients = getIngredients();
        boolean usesBaseItem = hasIngredient(itemIngredients, 0);
        boolean usesPressHead = hasIngredient(itemIngredients, 1);
        boolean usesAdditionItem = hasIngredient(itemIngredients, 2);
        boolean usesFullItemPattern = usesBaseItem && usesPressHead && usesAdditionItem;
        boolean usesFluid = !getFluidIngredients().isEmpty();
        boolean usesGas = !getGasIngredients().isEmpty();
        int advancedInputModes = (usesFullItemPattern ? 1 : 0) + (usesFluid ? 1 : 0) + (usesGas ? 1 : 0);
        if (advancedInputModes > 1) {
            errors.add("Forging Press recipes may use at most one advanced input mode: a complete three-item pattern, a fluid input, or a gas input.");
        }
        if (!usesBaseItem && !usesAdditionItem && !usesFluid && !usesGas) {
            errors.add("Forging Press recipes must define at least one consumable base item, addition item, fluid, or gas input.");
        }
        if (!getRollableResults().isEmpty()) {
            return;
        }

        errors.add("Forging Press recipes must define at least one item output.");
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        if (!matchesItemInputs(input)) {
            return false;
        }

        if (getFluidIngredients().isEmpty() && getGasIngredients().isEmpty()) {
            return true;
        }

        if (!(input instanceof ForgingPressRecipeInput forgingInput)) {
            return false;
        }

        IFluidHandler fluidHandler = forgingInput.getFluidHandler();
        IGasHandler gasHandler = forgingInput.getGasHandler();
        return planFluidAndGasConsumption(this, fluidHandler, gasHandler, new int[fluidHandler.getTanks()], new long[gasHandler.getTanks()], 1);
    }

    public @Nullable SmithingRecipe getSmithingRecipe() {
        return smithingRecipe;
    }

    private ForgingPressRecipe setSmithingRecipe(@Nullable SmithingRecipe recipe) {
        smithingRecipe = recipe;
        return this;
    }

    private boolean matchesItemInputs(RecipeInput input) {
        NonNullList<Ingredient> ingredients = getIngredients();
        int slotCount = Math.max(input.size(), ingredients.size());
        for (int slot = 0; slot < slotCount; slot++) {
            Ingredient ingredient = slot < ingredients.size() ? ingredients.get(slot) : Ingredient.EMPTY;
            ItemStack inputStack = slot < input.size() ? input.getItem(slot) : ItemStack.EMPTY;
            if (ingredient.isEmpty()) {
                if (!inputStack.isEmpty()) {
                    return false;
                }

                continue;
            }

            if (inputStack.isEmpty() || !ingredient.test(inputStack)) {
                return false;
            }
        }
        return true;
    }

    private interface ForgingPressRecipeInput extends RecipeInput {
        IFluidHandler getFluidHandler();

        IGasHandler getGasHandler();
    }

    private record CraftPlan(int crafts, int[] fluidAmounts, long[] gasAmounts) {
        private CraftPlan {
            fluidAmounts = fluidAmounts.clone();
            gasAmounts = gasAmounts.clone();
        }
    }
}
