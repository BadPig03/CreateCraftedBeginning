package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.kinetics.press.PressingRecipe;
import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
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
import net.ty.createcraftedbeginning.platform.access.SmithingTransformRecipeAccess;
import net.ty.createcraftedbeginning.platform.access.SmithingTrimRecipeAccess;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipeContext.ConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.ForgingPressRecipeContext.OutputPlan;
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
    protected SmithingRecipe smithingRecipe;

    public ForgingPressRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.FORGING_PRESS, params);
    }

    public static boolean canConvertSmithingRecipe(Recipe<?> source) {
        return source instanceof SmithingTransformRecipe && source instanceof SmithingTransformRecipeAccess
            || source instanceof SmithingTrimRecipe && source instanceof SmithingTrimRecipeAccess;
    }

    public static RecipeHolder<ForgingPressRecipe> convertToForgingPressRecipe(RecipeHolder<?> holder) {
        Builder<ForgingPressRecipe> builder = new Builder<>(ForgingPressRecipe::new, holder.id());
        Recipe<?> source = holder.value();
        if (source instanceof SmithingTransformRecipe smithingRecipe && source instanceof SmithingTransformRecipeAccess accessor) {
            ForgingPressRecipe recipe = builder.require(accessor.getBase()).require(accessor.getTemplate()).require(accessor.getAddition()).build().setSmithingRecipe(smithingRecipe);
            return new RecipeHolder<>(holder.id(), recipe);
        }

        if (source instanceof SmithingTrimRecipe smithingRecipe && source instanceof SmithingTrimRecipeAccess accessor) {
            ForgingPressRecipe recipe = builder.require(accessor.getBase()).require(accessor.getTemplate()).require(accessor.getAddition()).build().setSmithingRecipe(smithingRecipe);
            return new RecipeHolder<>(holder.id(), recipe);
        }

        return new RecipeHolder<>(holder.id(), builder.build());
    }

    public static RecipeHolder<ForgingPressRecipe> convertPressingToForgingPressRecipe(RecipeHolder<?> holder) {
        Builder<ForgingPressRecipe> builder = new Builder<>(ForgingPressRecipe::new, holder.id());
        if (!(holder.value() instanceof PressingRecipe pressingRecipe)) {
            return new RecipeHolder<>(holder.id(), builder.build());
        }

        builder.withItemIngredients(pressingRecipe.getIngredients());
        pressingRecipe.getRollableResults().forEach(builder::output);
        return new RecipeHolder<>(holder.id(), builder.build());
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

        IItemHandler pressHead = press.getPressHeadInventory();
        Ingredient pressHeadIngredient = getIngredient(recipe, 1);
        if (!matchesNonConsumableSlot(pressHead, pressHeadIngredient)) {
            return false;
        }

        IItemHandler addition = press.getAdditionInventory();
        IItemHandler input = press.getInputInventory();
        Ingredient inputIngredient = getIngredient(recipe, 0);
        Ingredient additionIngredient = getIngredient(recipe, 2);
        int maxCrafts = getMaxItemCrafts(addition, additionIngredient, input, inputIngredient);
        if (maxCrafts <= 0) {
            return false;
        }

        ItemStack inputStack = getConsumableStack(input, inputIngredient);
        if (inputStack == null) {
            return false;
        }

        IFluidHandler fluids = press.getFluidCapability();
        IGasHandler gases = press.getGasCapability();
        CraftPlan craftPlan = findLargestCraftPlan(press, recipe, level, inputStack, fluids, gases, maxCrafts);
        if (craftPlan == null) {
            return false;
        }

        if (simulate) {
            return true;
        }

        List<ItemStack> outputs = createRecipeOutputItems(recipe, level, inputStack, true, craftPlan.crafts());
        Optional<OutputPlan> outputPlan = press.planOutputs(outputs);
        if (outputPlan.isEmpty()) {
            return false;
        }

        int additionAmount = additionIngredient.isEmpty() ? 0 : craftPlan.crafts();
        int inputAmount = inputIngredient.isEmpty() ? 0 : craftPlan.crafts();
        ConsumptionPlan consumptionPlan = press.createConsumptionPlan(addition.getStackInSlot(0).copy(), additionAmount, input.getStackInSlot(0).copy(), inputAmount, craftPlan.fluidAmounts(), craftPlan.gasAmounts());
        return press.commitCraft(consumptionPlan, outputPlan.get());
    }

    private static @Nullable CraftPlan findLargestCraftPlan(ForgingPressRecipeContext press, ForgingPressRecipe recipe, Level level, ItemStack input, IFluidHandler fluids, IGasHandler gases, int maxCrafts) {
        List<ItemStack> singleCraftOutputs = createRecipeOutputItems(recipe, level, input, false, 1);
        if (singleCraftOutputs.isEmpty() || !outputsPassFilter(press, singleCraftOutputs)) {
            return null;
        }

        int low = 1;
        int high = maxCrafts;
        CraftPlan bestPlan = null;
        while (low <= high) {
            int crafts = low + high >>> 1;
            int[] fluidAmounts = new int[fluids.getTanks()];
            long[] gasAmounts = new long[gases.getTanks()];
            boolean hasResources = planFluidAndGasConsumption(recipe, fluids, gases, fluidAmounts, gasAmounts, crafts);
            boolean outputsFit = hasResources && press.acceptOutputs(createRecipeOutputItems(recipe, level, input, false, crafts), true);
            if (!outputsFit) {
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
        if (ingredient.isEmpty()) {
            return true;
        }

        ItemStack stack = inventory.getStackInSlot(0);
        return !stack.isEmpty() && ingredient.test(stack);
    }

    private static @Nullable ItemStack getConsumableStack(IItemHandler inventory, Ingredient ingredient) {
        if (ingredient.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack extracted = inventory.extractItem(0, 1, true);
        if (extracted.isEmpty() || !ingredient.test(extracted)) {
            return null;
        }
        return extracted.copy();
    }

    private static int getMaxItemCrafts(IItemHandler addition, Ingredient additionIngredient, IItemHandler input, Ingredient inputIngredient) {
        int maxCrafts = getAvailableCrafts(addition, additionIngredient, 64);
        return getAvailableCrafts(input, inputIngredient, maxCrafts);
    }

    private static int getAvailableCrafts(IItemHandler inventory, Ingredient ingredient, int maxCrafts) {
        if (ingredient.isEmpty()) {
            return maxCrafts;
        }

        ItemStack extracted = inventory.extractItem(0, maxCrafts, true);
        if (extracted.isEmpty() || !ingredient.test(extracted)) {
            return 0;
        }
        return extracted.getCount();
    }

    private static List<ItemStack> createRecipeOutputItems(ForgingPressRecipe recipe, Level level, ItemStack input, boolean rollRandomOutputs) {
        List<ItemStack> outputs = new ArrayList<>();
        List<ProcessingOutput> rollableResults = recipe.getRollableResults();
        for (int resultIndex = 0; resultIndex < rollableResults.size(); resultIndex++) {
            ProcessingOutput output = rollableResults.get(resultIndex);
            ItemStack stack = rollRandomOutputs ? output.rollOutput(level.random) : output.getStack();
            if (stack.isEmpty()) {
                continue;
            }

            ItemStack copied = stack.copy();
            if (resultIndex == PRIMARY_RESULT_INDEX && !input.isEmpty()) {
                copied.applyComponents(input.getComponentsPatch());
            }
            outputs.add(copied);
        }
        return outputs;
    }

    private static List<ItemStack> createRecipeOutputItems(ForgingPressRecipe recipe, Level level, ItemStack input, boolean rollRandomOutputs, int crafts) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int i = 0; i < crafts; i++) {
            outputs.addAll(createRecipeOutputItems(recipe, level, input, rollRandomOutputs));
        }
        return outputs;
    }

    private static boolean planFluidAndGasConsumption(ForgingPressRecipe recipe, IFluidHandler fluids, IGasHandler gases, int[] fluidAmounts, long[] gasAmounts, int crafts) {
        return planFluidConsumption(recipe.getFluidIngredients(), fluids, fluidAmounts, crafts) && planGasConsumption(recipe.getGasIngredients(), gases, gasAmounts, crafts);
    }

    private static boolean planFluidConsumption(List<SizedFluidIngredient> ingredients, IFluidHandler fluids, int[] amounts, int crafts) {
        for (SizedFluidIngredient ingredient : ingredients) {
            long required = (long) ingredient.amount() * crafts;
            if (required <= 0 || required > Integer.MAX_VALUE) {
                return false;
            }

            if (!consumeFluid(ingredient, fluids, amounts, (int) required)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeFluid(SizedFluidIngredient ingredient, IFluidHandler fluids, int[] amounts, int required) {
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            FluidStack stack = fluids.getFluidInTank(tank);
            if (!ingredient.test(stack)) {
                continue;
            }

            int available = stack.getAmount() - amounts[tank];
            if (available <= 0) {
                continue;
            }

            int drained = Math.min(required, available);
            amounts[tank] += drained;
            required -= drained;
            if (required <= 0) {
                return true;
            }
        }
        return false;
    }

    private static boolean planGasConsumption(List<SizedGasIngredient> ingredients, IGasHandler gases, long[] amounts, int crafts) {
        for (SizedGasIngredient ingredient : ingredients) {
            long amountPerCraft = ingredient.amount();
            if (amountPerCraft <= 0 || amountPerCraft > Long.MAX_VALUE / crafts) {
                return false;
            }

            if (!consumeGas(ingredient, gases, amounts, amountPerCraft * crafts)) {
                return false;
            }
        }
        return true;
    }

    private static boolean consumeGas(SizedGasIngredient ingredient, IGasHandler gases, long[] amounts, long required) {
        for (int tank = 0; tank < gases.getTanks(); tank++) {
            GasStack stack = gases.getGasInTank(tank);
            if (!ingredient.test(stack)) {
                continue;
            }

            long available = stack.getAmount() - amounts[tank];
            if (available <= 0) {
                continue;
            }

            long drained = Math.min(required, available);
            amounts[tank] += drained;
            required -= drained;
            if (required <= 0) {
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

    public @Nullable SmithingRecipe getSmithingRecipe() {
        return smithingRecipe;
    }

    public ForgingPressRecipe setSmithingRecipe(@Nullable SmithingRecipe recipe) {
        smithingRecipe = recipe;
        return this;
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

        IFluidHandler fluids = forgingInput.getFluidHandler();
        IGasHandler gases = forgingInput.getGasHandler();
        return planFluidAndGasConsumption(this, fluids, gases, new int[fluids.getTanks()], new long[gases.getTanks()], 1);
    }

    protected boolean matchesItemInputs(RecipeInput input) {
        NonNullList<Ingredient> ingredients = getIngredients();
        int slots = Math.max(input.size(), ingredients.size());
        for (int slot = 0; slot < slots; slot++) {
            Ingredient ingredient = slot < ingredients.size() ? ingredients.get(slot) : Ingredient.EMPTY;
            ItemStack stack = slot < input.size() ? input.getItem(slot) : ItemStack.EMPTY;
            if (ingredient.isEmpty()) {
                if (!stack.isEmpty()) {
                    return false;
                }

                continue;
            }

            if (stack.isEmpty() || !ingredient.test(stack)) {
                return false;
            }
        }
        return true;
    }

    protected interface ForgingPressRecipeInput extends RecipeInput {
        IFluidHandler getFluidHandler();

        IGasHandler getGasHandler();
    }

    protected record CraftPlan(int crafts, int[] fluidAmounts, long[] gasAmounts) {
        protected CraftPlan {
            fluidAmounts = fluidAmounts.clone();
            gasAmounts = gasAmounts.clone();
        }
    }
}
