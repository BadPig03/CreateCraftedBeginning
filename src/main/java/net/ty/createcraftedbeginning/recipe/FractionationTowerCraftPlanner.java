package net.ty.createcraftedbeginning.recipe;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.ty.createcraftedbeginning.api.gas.GasAction;
import net.ty.createcraftedbeginning.api.gas.GasStack;
import net.ty.createcraftedbeginning.api.gas.handler.GasStorageHandler;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlan;
import net.ty.createcraftedbeginning.recipe.gas.consumption.GasConsumptionPlanner;
import net.ty.createcraftedbeginning.recipe.transaction.MachineResourceSnapshots;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;
import java.util.Optional;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public final class FractionationTowerCraftPlanner {
    private final IItemHandler items;
    private final IFluidHandler fluids;
    private final GasStorageHandler gases;

    public FractionationTowerCraftPlanner(IItemHandler items, IFluidHandler fluids, GasStorageHandler gases) {
        this.items = items;
        this.fluids = fluids;
        this.gases = gases;
    }

    public Optional<InputPlan> planInputs(FractionationTowerRecipe recipe) {
        int[] itemAmounts = new int[items.getSlots()];
        int[] fluidAmounts = new int[fluids.getTanks()];
        Optional<GasConsumptionPlan> gasPlan = GasConsumptionPlanner.plan(recipe.getGasRequirements(), gases);
        if (gasPlan.isEmpty() || !RecipeInputAllocation.planItems(recipe.getIngredients(), items, itemAmounts) || !RecipeInputAllocation.planFluids(recipe.getFluidIngredients(), fluids, fluidAmounts)) {
            return Optional.empty();
        }

        return Optional.of(new InputPlan(items, fluids, gasPlan.get(), itemAmounts, fluidAmounts));
    }

    public boolean transferOutput(FractionationTowerOutput output, boolean simulate) {
        ItemStack item = output.item();
        if (!item.isEmpty()) {
            return ItemHandlerHelper.insertItemStacked(items, item, simulate).isEmpty();
        }

        FluidStack fluid = output.fluid();
        if (!fluid.isEmpty()) {
            FluidAction action = simulate ? FluidAction.SIMULATE : FluidAction.EXECUTE;
            return fluids.fill(fluid, action) == fluid.getAmount();
        }

        GasStack gas = output.gas();
        GasAction action = simulate ? GasAction.SIMULATE : GasAction.EXECUTE;
        return !gas.isEmpty() && gases.fill(gas, action) == gas.getAmount();
    }

    public static final class InputPlan {
        private final IItemHandler items;
        private final IFluidHandler fluids;
        private final GasConsumptionPlan gasPlan;
        private final int[] itemAmounts;
        private final int[] fluidAmounts;
        private final List<ItemStack> expectedItems;
        private final List<FluidStack> expectedFluids;

        private InputPlan(IItemHandler items, IFluidHandler fluids, GasConsumptionPlan gasPlan, int[] itemAmounts, int[] fluidAmounts) {
            this.items = items;
            this.fluids = fluids;
            this.gasPlan = gasPlan;
            this.itemAmounts = itemAmounts.clone();
            this.fluidAmounts = fluidAmounts.clone();
            expectedItems = MachineResourceSnapshots.copyItems(items);
            expectedFluids = MachineResourceSnapshots.copyFluids(fluids);
        }

        public boolean canExecute() {
            return MachineResourceSnapshots.matchesItems(items, expectedItems) && MachineResourceSnapshots.matchesFluids(fluids, expectedFluids) && gasPlan.canExecute();
        }

        public boolean execute() {
            if (!canExecute()) {
                return false;
            }

            for (int slot = 0; slot < itemAmounts.length; slot++) {
                int amount = itemAmounts[slot];
                if (amount == 0) {
                    continue;
                }

                ItemStack extracted = items.extractItem(slot, amount, false);
                if (extracted.getCount() != amount || !ItemStack.isSameItemSameComponents(extracted, expectedItems.get(slot))) {
                    return false;
                }
            }
            for (int tank = 0; tank < fluidAmounts.length; tank++) {
                int amount = fluidAmounts[tank];
                if (amount == 0) {
                    continue;
                }

                FluidStack expected = expectedFluids.get(tank);
                FluidStack extracted = fluids.drain(expected.copyWithAmount(amount), FluidAction.EXECUTE);
                if (extracted.getAmount() != amount || !FluidStack.isSameFluidSameComponents(extracted, expected)) {
                    return false;
                }
            }
            return gasPlan.execute();
        }
    }
}
