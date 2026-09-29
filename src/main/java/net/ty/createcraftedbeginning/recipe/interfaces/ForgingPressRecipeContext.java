package net.ty.createcraftedbeginning.recipe.interfaces;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
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

    GasStorageHandler getGasCapability();

    boolean testRecipeFilter(ItemStack stack);

    Optional<OutputPlan> planOutputs(List<ItemStack> outputItems);

    boolean acceptOutputs(List<ItemStack> outputItems, boolean simulate);

    ConsumptionPlan createConsumptionPlan(ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, int[] fluidAmounts, GasConsumptionPlan gasPlan);

    boolean commitCraft(ConsumptionPlan consumptionPlan, OutputPlan outputPlan);

    record OutputPlan(List<ItemStack> expectedSlots, List<ItemStack> finalSlots) {
        public OutputPlan {
            expectedSlots = copyStacks(expectedSlots);
            finalSlots = copyStacks(finalSlots);
        }

        @Override
        public List<ItemStack> expectedSlots() {
            return copyStacks(expectedSlots);
        }

        @Override
        public List<ItemStack> finalSlots() {
            return copyStacks(finalSlots);
        }

        private static @Unmodifiable List<ItemStack> copyStacks(List<ItemStack> stacks) {
            return stacks.stream().map(ItemStack::copy).toList();
        }
    }

    record ConsumptionPlan(ItemStack expectedPressHeadStack, ItemStack expectedProcessingStack, int processingAmount, ItemStack expectedInputStack, int inputAmount, FluidStack expectedFluid, int fluidAmount, GasConsumptionPlan gasPlan) {
        public ConsumptionPlan {
            expectedPressHeadStack = expectedPressHeadStack.copy();
            expectedProcessingStack = expectedProcessingStack.copy();
            expectedInputStack = expectedInputStack.copy();
            expectedFluid = expectedFluid.copy();
            if (processingAmount < 0 || inputAmount < 0 || fluidAmount < 0) {
                throw new IllegalArgumentException("Consumption amounts must be non-negative; got processingAmount=" + processingAmount + ", inputAmount=" + inputAmount + ", fluidAmount=" + fluidAmount + " mB.");
            }
        }

        @Override
        public ItemStack expectedPressHeadStack() {
            return expectedPressHeadStack.copy();
        }

        @Override
        public ItemStack expectedProcessingStack() {
            return expectedProcessingStack.copy();
        }

        @Override
        public ItemStack expectedInputStack() {
            return expectedInputStack.copy();
        }

        @Override
        public FluidStack expectedFluid() {
            return expectedFluid.copy();
        }
    }
}
