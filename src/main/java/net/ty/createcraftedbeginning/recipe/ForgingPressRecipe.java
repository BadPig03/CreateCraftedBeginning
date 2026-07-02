package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.foundation.blockEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
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
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import net.ty.createcraftedbeginning.api.gas.gases.ingredients.SizedGasIngredient;
import net.ty.createcraftedbeginning.api.gas.gases.behaviours.SmartGasTankBehaviour;
import net.ty.createcraftedbeginning.api.gas.recipes.ProcessingWithGasRecipeParams;
import net.ty.createcraftedbeginning.api.gas.recipes.StandardProcessingWithGasRecipe;
import net.ty.createcraftedbeginning.content.airtights.airtightforgingpress.AirtightForgingPressBlockEntity;
import net.ty.createcraftedbeginning.mixin.accessor.SmithingTransformRecipeAccessor;
import net.ty.createcraftedbeginning.mixin.accessor.SmithingTrimRecipeAccessor;
import net.ty.createcraftedbeginning.recipe.trie.IAirtightWithGasRecipe;
import net.ty.createcraftedbeginning.registry.CCBRecipeTypes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ForgingPressRecipe extends StandardProcessingWithGasRecipe<RecipeInput> implements IAirtightWithGasRecipe {
    @Nullable
    private SmithingRecipe smithingRecipe;

    public ForgingPressRecipe(ProcessingWithGasRecipeParams params) {
        super(CCBRecipeTypes.FORGING_PRESS, params);
    }

    public static RecipeHolder<ForgingPressRecipe> convertToForgingPressRecipe(RecipeHolder<?> holder) {
        Builder<ForgingPressRecipe> builder = new Builder<>(ForgingPressRecipe::new, holder.id());
        Recipe<?> holderValue = holder.value();
        if (holderValue instanceof SmithingTransformRecipe smithingRecipe && holderValue instanceof SmithingTransformRecipeAccessor accessor) {
            return new RecipeHolder<>(holder.id(), builder.require(accessor.getBase()).require(accessor.getTemplate()).require(accessor.getAddition()).build().setSmithingRecipe(smithingRecipe));
        }
        else if (holderValue instanceof SmithingTrimRecipe smithingRecipe && holderValue instanceof SmithingTrimRecipeAccessor accessor) {
            return new RecipeHolder<>(holder.id(), builder.require(accessor.getBase()).require(accessor.getTemplate()).require(accessor.getAddition()).build().setSmithingRecipe(smithingRecipe));
        }

        return new RecipeHolder<>(holder.id(), builder.build());
    }

    public static boolean match(AirtightForgingPressBlockEntity press, ForgingPressRecipe recipe) {
        return apply(press, recipe, true);
    }

    public static boolean apply(AirtightForgingPressBlockEntity press, ForgingPressRecipe recipe) {
        return apply(press, recipe, false);
    }

    private static boolean apply(AirtightForgingPressBlockEntity press, ForgingPressRecipe recipe, boolean test) {
        Level level = press.getLevel();
        if (level == null) {
            return false;
        }

        IItemHandler pressHeadInv = press.getProcessingInventories().getFirst();
        IItemHandler topInv = press.getProcessingInventories().getSecond();
        IItemHandler bottomInv = press.getInputOutputInventories().getFirst();
        IFluidHandler availableFluids = press.getFluidCapability();
        IGasHandler availableGases = press.getGasCapability();
        if (availableFluids == null || availableGases == null) {
            return false;
        }

        Ingredient bottomIngredient = getIngredient(recipe, 0);
        Ingredient pressHeadIngredient = getIngredient(recipe, 1);
        Ingredient topIngredient = getIngredient(recipe, 2);
        if (!matchesNonConsumableSlot(pressHeadInv, pressHeadIngredient)) {
            return false;
        }

        int crafts = getMaxItemCrafts(topInv, topIngredient, bottomInv, bottomIngredient);
        if (crafts <= 0) {
            return false;
        }

        ItemStack bottomInput = getConsumableStack(bottomInv, bottomIngredient);
        if (bottomInput == null) {
            return false;
        }

        int[] extractedFluidsFromTank = new int[availableFluids.getTanks()];
        long[] extractedGasesFromTank = new long[availableGases.getTanks()];
        while (crafts > 0) {
            extractedFluidsFromTank = new int[availableFluids.getTanks()];
            extractedGasesFromTank = new long[availableGases.getTanks()];
            if (!planFluidAndGasConsumption(recipe, availableFluids, availableGases, extractedFluidsFromTank, extractedGasesFromTank, crafts)) {
                crafts--;
                continue;
            }

            List<ItemStack> simulatedOutputItems = createRecipeOutputItems(recipe, level, bottomInput, false, crafts);
            if (simulatedOutputItems.isEmpty() || !outputsPassFilter(press, simulatedOutputItems)) {
                return false;
            }

            if (press.acceptOutputs(simulatedOutputItems, true)) {
                break;
            }

            crafts--;
        }

        if (crafts <= 0) {
            return false;
        }

        if (test) {
            return true;
        }

        List<ItemStack> outputItems = createRecipeOutputItems(recipe, level, bottomInput, true, crafts);
        if (!press.acceptOutputs(outputItems, true)) {
            return false;
        }

        if (!topIngredient.isEmpty()) {
            topInv.extractItem(0, crafts, false);
        }
        if (!bottomIngredient.isEmpty()) {
            bottomInv.extractItem(0, crafts, false);
        }
        executePlannedFluidAndGasConsumption(press, availableFluids, availableGases, extractedFluidsFromTank, extractedGasesFromTank);
        return press.acceptOutputs(outputItems, false);
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

    private static int getMaxItemCrafts(IItemHandler topInv, Ingredient topIngredient, IItemHandler bottomInv, Ingredient bottomIngredient) {
        int crafts = 64;
        crafts = Math.min(crafts, getAvailableCrafts(topInv, topIngredient, crafts));
        crafts = Math.min(crafts, getAvailableCrafts(bottomInv, bottomIngredient, crafts));
        return crafts;
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

    private static List<ItemStack> createRecipeOutputItems(ForgingPressRecipe recipe, Level level, ItemStack bottomInput, boolean rollRandomOutputs) {
        List<ItemStack> outputs = new ArrayList<>();
        if (rollRandomOutputs) {
            recipe.rollResults(level.random).stream().filter(stack -> !stack.isEmpty()).map(ItemStack::copy).forEach(outputs::add);
        }
        else {
            recipe.getRollableResults().stream().map(output -> output.getStack().copy()).filter(stack -> !stack.isEmpty()).forEach(outputs::add);
        }
        if (!bottomInput.isEmpty() && !outputs.isEmpty()) {
            ItemStack result = outputs.getFirst();
            ItemStack copied = result.copyWithCount(result.getCount());
            copied.applyComponents(bottomInput.getComponentsPatch());
            outputs.set(0, copied);
        }
        return outputs;
    }

    private static List<ItemStack> createRecipeOutputItems(ForgingPressRecipe recipe, Level level, ItemStack bottomInput, boolean rollRandomOutputs, int crafts) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int i = 0; i < crafts; i++) {
            outputs.addAll(createRecipeOutputItems(recipe, level, bottomInput, rollRandomOutputs));
        }
        return outputs;
    }

    private static boolean planFluidAndGasConsumption(ForgingPressRecipe recipe, IFluidHandler availableFluids, IGasHandler availableGases, int[] extractedFluidsFromTank, long[] extractedGasesFromTank, int crafts) {
        FluidIngredients:
        for (SizedFluidIngredient fluidIngredient : recipe.getFluidIngredients()) {
            int amountRequired = fluidIngredient.amount() * crafts;
            if (amountRequired <= 0) {
                continue;
            }

            for (int tank = 0; tank < availableFluids.getTanks(); tank++) {
                FluidStack fluidStack = availableFluids.getFluidInTank(tank);
                if (!fluidIngredient.test(fluidStack)) {
                    continue;
                }

                int availableAmount = fluidStack.getAmount() - extractedFluidsFromTank[tank];
                if (availableAmount <= 0) {
                    continue;
                }

                int drainedAmount = Math.min(amountRequired, availableAmount);
                extractedFluidsFromTank[tank] += drainedAmount;
                amountRequired -= drainedAmount;
                if (amountRequired > 0) {
                    continue;
                }

                continue FluidIngredients;
            }

            return false;
        }

        GasIngredients:
        for (SizedGasIngredient gasIngredient : recipe.getGasIngredients()) {
            long amountRequired = gasIngredient.amount() * crafts;
            if (amountRequired <= 0) {
                continue;
            }

            for (int tank = 0; tank < availableGases.getTanks(); tank++) {
                GasStack gasStack = availableGases.getGasInTank(tank);
                if (!gasIngredient.test(gasStack)) {
                    continue;
                }

                long availableAmount = gasStack.getAmount() - extractedGasesFromTank[tank];
                if (availableAmount <= 0) {
                    continue;
                }

                long drainedAmount = Math.min(amountRequired, availableAmount);
                extractedGasesFromTank[tank] += drainedAmount;
                amountRequired -= drainedAmount;
                if (amountRequired > 0) {
                    continue;
                }

                continue GasIngredients;
            }

            return false;
        }

        return true;
    }

    private static void executePlannedFluidAndGasConsumption(AirtightForgingPressBlockEntity press, IFluidHandler availableFluids, IGasHandler availableGases, int @NotNull [] extractedFluidsFromTank, long[] extractedGasesFromTank) {
        boolean fluidsAffected = false;
        for (int tank = 0; tank < extractedFluidsFromTank.length; tank++) {
            int amount = extractedFluidsFromTank[tank];
            if (amount <= 0) {
                continue;
            }

            availableFluids.getFluidInTank(tank).shrink(amount);
            fluidsAffected = true;
        }
        if (fluidsAffected) {
            press.getFluidTank().forEach(TankSegment::onFluidStackChanged);
        }

        boolean gasesAffected = false;
        for (int tank = 0; tank < extractedGasesFromTank.length; tank++) {
            long amount = extractedGasesFromTank[tank];
            if (amount <= 0) {
                continue;
            }

            availableGases.getGasInTank(tank).shrink(amount);
            gasesAffected = true;
        }
        if (gasesAffected) {
            press.getGasTank().forEach(SmartGasTankBehaviour.TankSegment::onGasStackChanged);
        }
    }

    private static boolean outputsPassFilter(AirtightForgingPressBlockEntity press, List<ItemStack> outputs) {
        FilteringBehaviour filter = press.getFilteringBehaviour();
        return filter != null && !outputs.isEmpty() && filter.test(outputs.getFirst());
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
    protected boolean specialValidateCondition() {
        int type = 0;
        if (getIngredients().size() == getMaxInputCount()) {
            type++;
        }
        if (!getFluidIngredients().isEmpty()) {
            type++;
        }
        if (!getGasIngredients().isEmpty()) {
            type++;
        }
        return type > 1;
    }

    @Override
    public boolean matches(RecipeInput input, Level level) {
        return true;
    }
}
