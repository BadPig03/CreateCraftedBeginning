package net.ty.createcraftedbeginning.recipe;

import com.simibubi.create.content.processing.recipe.ProcessingOutput;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.recipe.ReactorKettleCraftPlanner.Inputs;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ReactorKettleRecipeOutputs {
    private final ReactorKettleRecipe recipe;

    ReactorKettleRecipeOutputs(ReactorKettleRecipe recipe) {
        this.recipe = recipe;
    }

    Outputs preview(Inputs inputs) {
        return createOutputs(inputs, false);
    }

    Outputs roll(Inputs inputs) {
        return createOutputs(inputs, true);
    }

    private Outputs createOutputs(Inputs inputs, boolean rollOutputs) {
        List<ItemStack> items = createRecipeOutputItems(inputs.level(), inputs.items(), inputs.itemAmounts(), rollOutputs);
        List<FluidStack> fluids = createRecipeOutputFluids();
        List<GasStack> gases = createRecipeOutputGases();
        return new Outputs(items, fluids, gases);
    }

    private List<ItemStack> createRecipeOutputItems(Level level, IItemHandler availableItems, int[] itemAmounts, boolean rollRandomOutputs) {
        List<ItemStack> outputs = new ArrayList<>();
        if (rollRandomOutputs) {
            for (ItemStack itemStack : recipe.rollResults(level.random)) {
                if (itemStack.isEmpty()) {
                    continue;
                }

                outputs.add(itemStack.copy());
            }
        }
        else {
            for (ProcessingOutput output : recipe.getRollableResults()) {
                ItemStack itemStack = output.getStack();
                if (itemStack.isEmpty()) {
                    continue;
                }

                outputs.add(itemStack.copy());
            }
        }

        List<ItemStack> consumedItems = new ArrayList<>();
        for (int slot = 0; slot < itemAmounts.length; slot++) {
            ItemStack stack = availableItems.getStackInSlot(slot);
            for (int count = 0; count < itemAmounts[slot]; count++) {
                consumedItems.add(stack.copyWithCount(1));
            }
        }
        CraftingInput remainderInput = CraftingInput.of(consumedItems.size(), 1, consumedItems);
        for (ItemStack remainingItem : recipe.getRemainingItems(remainderInput)) {
            if (remainingItem.isEmpty()) {
                continue;
            }

            outputs.add(remainingItem.copy());
        }
        return outputs;
    }

    private List<FluidStack> createRecipeOutputFluids() {
        List<FluidStack> outputs = new ArrayList<>();
        for (FluidStack fluidStack : recipe.getFluidResults()) {
            if (fluidStack.isEmpty()) {
                continue;
            }

            outputs.add(fluidStack.copy());
        }
        return outputs;
    }

    private List<GasStack> createRecipeOutputGases() {
        List<GasStack> outputs = new ArrayList<>();
        for (GasStack gasStack : recipe.getGasResults()) {
            if (gasStack.isEmpty()) {
                continue;
            }

            outputs.add(gasStack.copy());
        }
        return outputs;
    }

    record Outputs(List<ItemStack> items, List<FluidStack> fluids, List<GasStack> gases) {}
}
