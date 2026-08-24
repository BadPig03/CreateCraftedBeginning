package net.ty.createcraftedbeginning.content.airtights.residueoutlet;

import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.ty.createcraftedbeginning.core.MachineResourceSnapshots;
import net.ty.createcraftedbeginning.core.ResourceTransaction;
import org.jetbrains.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
final class ResidueOutletInsertionPlanner {
    private final ResidueOutletBlockEntity outlet;
    private final ResidueOutletInventory inventory;

    ResidueOutletInsertionPlanner(ResidueOutletBlockEntity outlet, ResidueOutletInventory inventory) {
        this.outlet = outlet;
        this.inventory = inventory;
    }

    @Nullable ResidueInsertionPlan create(FluidStack fluidStack, ItemStack itemStack, int maxAmount) {
        boolean hasFluid = !fluidStack.isEmpty();
        boolean hasItem = !itemStack.isEmpty();
        Level level = outlet.getLevel();
        if (level == null || maxAmount <= 0 || hasFluid == hasItem) {
            return null;
        }

        Provider registryProvider = level.registryAccess();
        if (!hasFluid) {
            return createItemInsertionPlan(itemStack, maxAmount, registryProvider);
        }
        return createFluidInsertionPlan(fluidStack, maxAmount, registryProvider);
    }

    private @Nullable ResidueInsertionPlan createFluidInsertionPlan(FluidStack fluid, int maxAmount, Provider registryProvider) {
        int plannedAmount = outlet.insertResidueFluid(fluid.copyWithAmount(maxAmount), FluidAction.SIMULATE);
        if (plannedAmount <= 0) {
            return null;
        }

        SmartFluidTankBehaviour fluidTankBehaviour = outlet.getFluidTankBehaviour();
        FluidStack plannedFluid = fluid.copyWithAmount(plannedAmount);
        return new ResidueInsertionPlan(plannedAmount, ResourceTransaction.participant(() -> outlet.insertResidueFluid(plannedFluid, FluidAction.SIMULATE) == plannedAmount, () -> MachineResourceSnapshots.snapshotFluidTanks(registryProvider, fluidTankBehaviour), () -> outlet.insertResidueFluid(plannedFluid, FluidAction.EXECUTE) == plannedAmount, tankSnapshot -> {
            MachineResourceSnapshots.restoreFluidTanks(registryProvider, tankSnapshot, fluidTankBehaviour);
            outlet.notifyUpdate();
        }));
    }

    private @Nullable ResidueInsertionPlan createItemInsertionPlan(ItemStack item, int maxUnits, Provider registryProvider) {
        int plannedUnits = Math.min(maxUnits, inventory.getItemInsertionCapacityUnits(item));
        if (plannedUnits <= 0) {
            return null;
        }

        ItemStack plannedItem = item.copyWithCount(1);
        return new ResidueInsertionPlan(plannedUnits, ResourceTransaction.participant(() -> inventory.getItemInsertionCapacityUnits(plannedItem) >= plannedUnits, () -> inventory.serializeNBT(registryProvider).copy(), () -> inventory.addPartialItemUnits(plannedUnits, plannedItem) == plannedUnits, inventorySnapshot -> {
            inventory.deserializeNBT(registryProvider, inventorySnapshot.copy());
            outlet.notifyUpdate();
        }));
    }
}
