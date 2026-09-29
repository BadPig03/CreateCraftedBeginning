package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.kinetics.press.PressingRecipe;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.platform.SmithingRecipeBridge;
import net.ty.createcraftedbeginning.platform.SmithingRecipeBridge.Ingredients;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.gas.processing.GasProcessingRecipeParams;
import net.ty.createcraftedbeginning.recipe.gas.processing.StandardGasProcessingRecipe;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ForgingPressRecipe extends StandardGasProcessingRecipe<RecipeInput> {

    @Nullable
    private SmithingRecipe smithingRecipe;

    ForgingPressRecipe(GasProcessingRecipeParams params) {
        super(CCBRecipeTypes.FORGING_PRESS, params);
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
        GasStorageHandler gasHandler = forgingInput.getGasHandler();
        return ForgingPressCraftPlanner.planFluidConsumption(getFluidIngredients(), fluidHandler, new int[fluidHandler.getTanks()], 1) && GasConsumptionPlanner.plan(getGasRequirements(), gasHandler).isPresent();
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

    public @Nullable SmithingRecipe getSmithingRecipe() {
        return smithingRecipe;
    }

    private static boolean hasIngredient(NonNullList<Ingredient> ingredients, int index) {
        return index >= 0 && index < ingredients.size() && !ingredients.get(index).isEmpty();
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

        GasStorageHandler getGasHandler();
    }
}
