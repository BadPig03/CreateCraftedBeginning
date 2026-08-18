package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.gases.GasStack;
import net.ty.createcraftedbeginning.api.gas.gases.interfaces.IGasHandler;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import javax.annotation.ParametersAreNonnullByDefault;

import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public interface ForgingPressRecipeContext {
    @Nullable Level getLevel();

    IItemHandler getPressHeadInventory();

    IItemHandler getAdditionInventory();

    IItemHandler getInputInventory();

    IFluidHandler getFluidCapability();

    IGasHandler getGasCapability();

    boolean testRecipeFilter(ItemStack stack);

    Optional<OutputPlan> planOutputs(List<ItemStack> outputItems);

    boolean acceptOutputs(List<ItemStack> outputItems, boolean simulate);

    ConsumptionPlan createConsumptionPlan(ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, int[] fluidAmounts, long[] gasAmounts);

    boolean commitCraft(ConsumptionPlan consumptionPlan, OutputPlan outputPlan);

    record OutputPlan(List<ItemStack> expectedSlots, List<ItemStack> finalSlots) {
        public OutputPlan {
            expectedSlots = copyStacks(expectedSlots);
            finalSlots = copyStacks(finalSlots);
        }

        private static @Unmodifiable List<ItemStack> copyStacks(List<ItemStack> stacks) {
            return stacks.stream().map(ItemStack::copy).toList();
        }
    }

    record ConsumptionPlan(ItemStack expectedPressHeadStack, ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, FluidStack expectedFluid, int fluidAmount, GasStack expectedGas, long gasAmount) {
        public ConsumptionPlan {
            expectedPressHeadStack = expectedPressHeadStack.copy();
            expectedProcessingStack = expectedProcessingStack.copy();
            expectedInputStack = expectedInputStack.copy();
            expectedFluid = expectedFluid.copy();
            expectedGas = expectedGas.copy();
            if (processingAmount < 0 || inputAmount < 0 || fluidAmount < 0 || gasAmount < 0) {
                throw new IllegalArgumentException("Consumption amounts must not be negative");
            }
        }
    }
}
